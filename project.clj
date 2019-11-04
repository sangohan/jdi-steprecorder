(defproject steprecorder "0.1.0-SNAPSHOT"
  :description "Record execution of JVM programs"
  :url "http://github.com/alesguzik/jdi-steprecorder"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]]
  :plugins [[lein-jdk-tools "0.1.1"]]
  :main ^:skip-aot steprecorder.core
  :target-path "target/%s"
  :uberjar-name "steprecorder-standalone.jar"
  :profiles {:uberjar {:aot :all}})
