(ns wrench.core
  (:import
   com.sun.jdi.Bootstrap)
  (:gen-class))

;; TODO use mount

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

(def vm (socket-attach nil "5005"))

(.description vm)

(defn -main [& args]
  (prn (.allThreads vm))
  (println "Hello, World!"))
