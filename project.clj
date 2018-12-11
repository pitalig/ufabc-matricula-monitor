(defproject ufabc-matricula-monitor "0.1.0-SNAPSHOT"
  :description "App for monitoring UFABC enrollment"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http "3.9.1"]
                 [cheshire "5.8.1"]
                 [org.julienxx/clj-slack "0.5.6"]]
  :main ^:skip-aot ufabc-matricula-monitor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})