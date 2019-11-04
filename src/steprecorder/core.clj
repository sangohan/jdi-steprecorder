(ns steprecorder.core
  (:require
   [clojure.tools.cli :as cli]
   [clojure.string :as string]
   [clojure.java.io :as io])
  (:import
   com.sun.jdi.Bootstrap
   com.sun.jdi.request.StepRequest
   (com.sun.jdi.event StepEvent ThreadStartEvent MethodEntryEvent MethodExitEvent))
  (:gen-class))

(def recording-dir (atom "."))

(def vm-manager (Bootstrap/virtualMachineManager))

(def connectors
  (->> vm-manager
       .attachingConnectors
       (map #(vector (-> % .transport .name keyword) %))
       (into {})))

(def socket-connector (:dt_socket connectors))

(defn socket-attach [host port]
  (let [args (.defaultArguments socket-connector)]
    (when host (-> args (.get "hostname") (.setValue host)))
    (when port (-> args (.get "port")     (.setValue port)))
    (.attach socket-connector args)))

(defn method-str [method]
  (str (.returnTypeName method) " " (.name method) "("
       (->> method .argumentTypeNames vec (string/join ", "))
       ")"))

(defn location-str [location]
  (str (.sourcePath location) ":" (.lineNumber location)
       " # " (-> location .method method-str)))

(defn step-event-my-str [step-event]
  (-> step-event .location location-str))

(defn step-event-str [step-event]
  (let [loc (.location step-event)
        frame (-> step-event .thread (.frame 0))
        package-and-class (->> frame .toString (re-seq #"^[^:]+") first)]
    (format "%s.%s(%s:%d)"
            package-and-class (-> loc .method .name) (.sourceName loc) (.lineNumber loc))))

(defn step-request [thread]
  (-> thread .virtualMachine .eventRequestManager
      (.createStepRequest thread StepRequest/STEP_LINE StepRequest/STEP_INTO)
      (doto .enable)))

(defn thread-start-request [vm]
  (-> vm .eventRequestManager
      .createThreadStartRequest
      (doto .enable)))

(defn filename-for-thread [t]
  (str @recording-dir "/" (.name t) "__"(.uniqueID t) ".threadlog"))

(def vms (atom []))
(def current-event-set (atom nil))
(def thread-writers (atom {}))

(defn get-thread-writer [thread]
  (or ; *out*
      (-> thread-writers
          (swap! update thread #(or % (delay (-> (doto (filename-for-thread thread) io/make-parents)
                                                 io/writer))))
          (get thread)
          force)))

(defprotocol EventHandling
  (on-event [this]))

(extend-type ThreadStartEvent EventHandling
  (on-event [this]
    (step-request (.thread this))))

(extend-type StepEvent EventHandling
  (on-event [this]
    (let [w (get-thread-writer (.thread this))]
      (.write w (step-event-str this))
      ;(.write w (step-event-my-str this))
      (.write w "\n"))
    (-> this .virtualMachine .resume)))

(defn on-event-set [event-set]
  (reset! current-event-set event-set)
  (doseq [e (seq event-set)]
    (on-event e)))

(def listeners-should-be-running (atom true))

(defn event-handler-loop [vm]
  (while @listeners-should-be-running
    (-> vm .eventQueue (.remove 100) on-event-set))
  (doseq [[thread writer] @thread-writers]
    (.close @writer)))

(defn start-event-listener [vm]
  (doto (Thread. #(event-handler-loop vm)) .start))

(defn start-recording [vm]
  (println "Starting recording")
  (.suspend vm)
  (thread-start-request vm)
  (doseq [thread (.allThreads vm)]
    (step-request thread))
  (.resume vm)
  (start-event-listener vm))

(defn stop-recording [vm]
  (.suspend vm)
  (reset! listeners-should-be-running false)
  (let [em (-> vm .eventRequestManager)]
    (doseq [request (concat (-> em .stepRequests seq)
                            (-> em .threadStartRequests seq))]
      (.deleteEventRequest em request)))
  (println "Recording stopped"))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default "5005"]
   ["-H" "--host HOST" "Hostname"
    :default "localhost"]
   ["-d" "--dir DIRECTORY" "Directory to place thread recordings"
    :default "."
    :default-desc "Current directory"]
   ["-h" "--help"]])

(defn run [options]
  (let [{:keys [host port dir]} options]
    (reset! recording-dir dir)
    (try
      (let [vm (socket-attach host port)]
        (swap! vms conj vm)
        (start-recording vm)
        (println "Press [Enter] to stop recording")
        (read-line)
        (stop-recording vm))
      (catch java.net.ConnectException e
        (-> "Error: Cannot connect to %s:%s" (format host port) println)))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) (-> "\nUsage:\n%s\n"  (format summary) println)
      errors (->> errors (string/join "\n * ") (println "Errors:\n * "))
      :else (run options))))
