(defproject ufabc-registration-monitor "0.1.0-SNAPSHOT"
  :description "App for monitoring UFABC registration"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [cheshire "5.11.0"]
                 [org.julienxx/clj-slack "0.6.3"]
                 [nubank/matcher-combinators "3.5.1"]]
  :main ^:skip-aot ufabc-registration-monitor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :kaocha {:dependencies [[lambdaisland/kaocha "0.0-521"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]
            "test" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
