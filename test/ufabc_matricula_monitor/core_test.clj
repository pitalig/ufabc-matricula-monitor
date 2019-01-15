(ns ufabc-matricula-monitor.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [ufabc-matricula-monitor.core :as core]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [clj-http.client :as http]))

(stest/instrument)

(deftest map-keys
  (is (= {123 "456"}
         (core/map-keys {"123" "456"} core/parse-int)))
  (is (= {"123" 456}
         (core/map-keys {123 456} str))))

(deftest map-vals
  (is (= {"123" 456}
         (core/map-vals {"123" "456"} core/parse-int)))
  (is (= {123 "456"}
         (core/map-vals {123 456} str))))

(deftest map-kv
  (is (= {123 456}
         (core/map-kv {"123" "456"} core/parse-int)))
  (is (= {"123" "456"}
         (core/map-kv {123 456} str))))


(deftest secure-get!
  (with-redefs [http/get (constantly {:body "html" :status 200})]
    (is (let [{:keys [body status]} (core/secure-get! "https://www.google.com")]
          (and (string? body)
               (= 200 status))))))

(deftest parse-response
  (are [result sample-path]
    (match?
      (m/embeds result)
      (core/parse-response {:body (slurp sample-path)}))
    {"2034" ["8992" "8595" "8935"]} "resources/matriculas_sample_small.txt"
    {"8682" "94"} "resources/contagem_matriculas_sample_small.txt"
    [{"vagas" 45 "campus" 17 "id" 8994}] "resources/todas_disciplinas_sample_small.txt"))

(stest/unstrument)
