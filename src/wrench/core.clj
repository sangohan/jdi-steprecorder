(ns wrench.core
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io])
  (:import
   com.sun.jdi.Bootstrap
   com.sun.jdi.request.StepRequest
   (com.sun.jdi.event StepEvent ThreadStartEvent MethodEntryEvent MethodExitEvent))
  (:gen-class))

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
  (str (.sourcePath location) " : " (.lineNumber location)
       " # " (-> location .method method-str)))

(defn step-request [thread]
  (-> thread .virtualMachine .eventRequestManager
      (.createStepRequest thread StepRequest/STEP_LINE StepRequest/STEP_INTO)
      (doto .enable)))

(defn thread-start-request [vm]
  (-> vm .eventRequestManager
      .createThreadStartRequest
      (doto .enable)))

(defn filename-for-thread [t]
  (str (.name t) "__"(.uniqueID t) ".threadlog"))

(def vms (atom []))
(def current-event-set (atom nil))

(defn boring-thread? [thread]
  (not (#{"main"} (.name thread))))

(defn boring-location? [location]
  #_(let [source-path (.sourcePath location)]
    (or (. source-path startsWith "java/")
        (. source-path startsWith "sun/"))))

(defprotocol EventHandling
  (on-event [this]))

(extend-type ThreadStartEvent EventHandling
  (on-event [this]
    (step-request (.thread this))))

(extend-type StepEvent EventHandling
  (on-event [this]
    (let [loc (.location this)]
      (if-not (boring-location? loc)
        (-> loc location-str println)))
    (-> this .virtualMachine .resume)))

(defn on-event-set [event-set]
  (reset! current-event-set event-set)
  (doseq [e (seq event-set)]
    (on-event e)))

(def listeners-should-be-running (atom true))
;; (swap! listeners-should-be-running not)

(defn event-handler-loop [vm]
  (println "Starting event handler loop")
  (while @listeners-should-be-running
    (-> vm .eventQueue (.remove 1000) on-event-set))
  (println "Stopping event handler loop"))

(defn start-event-listener [vm]
  (doto (Thread. #(event-handler-loop vm)) .start))

(defn start-recording [vm]
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
  (Thread/sleep 1000))

(defn -main [& args]
  (let [vm (socket-attach nil "5005")]
    (swap! vms conj vm)
    (start-recording vm)
    (->> vm .allThreads (map filename-for-thread) println)
    (read-line)
    (stop-recording vm)))
