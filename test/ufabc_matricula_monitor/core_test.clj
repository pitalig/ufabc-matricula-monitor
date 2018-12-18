(ns ufabc-matricula-monitor.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [ufabc-matricula-monitor.core :as core]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]))

(stest/instrument)

(deftest secure-get!
  (is (let [{:keys [body status]} (core/secure-get! "https://www.google.com")]
        (and (string? body)
             (= 200 status)))))

(deftest parse-response
  (are [result sample-path]
    (match?
      (m/embeds result)
      (core/parse-response {:body (slurp sample-path)}))
    {"2034" ["8992" "8595" "8935"]} "resources/matriculas_sample.txt"
    {"8682" "94"} "resources/contagem_matriculas_sample.txt"
    [{"vagas" 45 "campus" 17 "id" 8994}] "resources/todas_disciplinas_sample.txt"))

(stest/unstrument)
