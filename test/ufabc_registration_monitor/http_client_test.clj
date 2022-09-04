(ns ufabc-registration-monitor.http-client-test
  (:require [clojure.test :refer :all]
            [ufabc-registration-monitor.http-client :as http]
            [matcher-combinators.test :refer [match?]]
            [clj-http.client]
            [matcher-combinators.matchers :as m]))

(deftest coerce-registrations-count
  (is (match? (m/equals {8290 77
                         8623 1
                         8682 94})
        (http/coerce-registrations-count {"8682" "94"
                                           "8623" "1"
                                           "8290" "77"}))))

(deftest coerce-courses-test
  (is (match? (m/equals [{:id 8220 :name "Aerodinamica I A-Noturno (Sao Bernardo)" :slots 86}
                         {:id 8221 :name "Aerodinamica I B-Noturno (Sao Bernardo)" :slots 86}])
        (http/coerce-courses [{:campus 18
                               :codigo "ESTS016-17"
                               :creditos 4
                               :horarios [{:horas ["19:00" "19:30" "20:00" "20:30" "21:00"]
                                           :periodicidade_extenso " - semanal"
                                           :semana 2}
                                          {:horas ["21:00" "21:30" "22:00" "22:30" "23:00"]
                                           :periodicidade_extenso " - semanal"
                                           :semana 4}]
                               :id 8220
                               :nome "Aerodinamica I A-Noturno (Sao Bernardo)"
                               :nome_campus "Campus Sao Bernardo do Campo"
                               :obrigatoriedades [{:curso_id 250 :obrigatoriedade "obrigatoria"}]
                               :recomendacoes nil
                               :tpi [4 0 5]
                               :vagas 86
                               :vagas_ingressantes nil}
                              {:id 8221
                               :nome "Aerodinamica I B-Noturno (Sao Bernardo)"
                               :vagas 86}]))))

(deftest get!
  (is (let [effects {:http-get-fn! (constantly {:body "html" :status 200})
                     :sleep-fn! identity}
            {:keys [body status]} (http/get! "https://www.google.com" effects)]
        (and (string? body)
             (= 200 status)))))

(deftest parse-response
  (is (match? (m/equals {8682 94
                         8623 1
                         8290 77})
        (let [{:keys [small-sample coerce-fn json-coerce-key-fn]} (:registrations-count http/bookmark-settings)]
          (http/parse-response small-sample coerce-fn json-coerce-key-fn)))
      "Can parse registrations count")

  (is (match? (m/equals [{:id 8220 :name "Aerodinamica I A-Noturno (Sao Bernardo)" :slots 86}])
        (let [{:keys [small-sample coerce-fn json-coerce-key-fn]} (:courses http/bookmark-settings)]
          (http/parse-response small-sample coerce-fn json-coerce-key-fn)))
      "Can parse all courses"))
