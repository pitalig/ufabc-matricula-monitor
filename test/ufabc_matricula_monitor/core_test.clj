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

(defn gen-sample [sample-path]
  {:body (slurp sample-path)})

(def matriculas-sample (gen-sample "resources/matriculas_sample_small.txt"))
(def contagem-matriculas-sample (gen-sample "resources/contagem_matriculas_sample_small.txt"))
(def todas-disciplinas-sample (gen-sample "resources/todas_disciplinas_sample_small.txt"))

(deftest coerce-matriculas
  (is (= {2034 [8992 8595 8935]
          526 [8662 9190]
          4994 [8417 8448 8484 8595 8492]}
         (core/coerce-matriculas (core/parse-response matriculas-sample)))))

(deftest secure-get!
  (with-redefs [http/get (constantly {:body "html" :status 200})]
    (is (let [{:keys [body status]} (core/secure-get! "https://www.google.com")]
          (and (string? body)
               (= 200 status))))))

(deftest parse-response
  (are [result sample]
    (match?
      (m/embeds result)
      (core/parse-response sample))
    {"2034" ["8992" "8595" "8935"]} matriculas-sample
    {"8682" "94"} contagem-matriculas-sample
    [{"vagas" 45 "campus" 17 "id" 8994}] todas-disciplinas-sample))

(stest/unstrument)
