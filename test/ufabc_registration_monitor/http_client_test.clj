(ns ufabc-registration-monitor.http-client-test
  (:require [clojure.test :refer :all]
            [ufabc-registration-monitor.http-client :as http]
            [matcher-combinators.test :refer [match?]]
            [clj-http.client]
            [clojure.spec.test.alpha :as stest]
            [matcher-combinators.matchers :as m]))

(stest/instrument)

(def registrations-sample {:body "matriculas={\"2034\":[\"8992\"],\"4994\":[\"8595\",\"8492\"],\"526\":[\"8417\",\"8662\",\"9190\"]};"})
(def registrations-count-sample {:body "contagemMatriculas={\"8682\":\"94\",\"8623\":\"1\",\"8290\":\"77\"};"})
(def courses-sample {:body "todasDisciplinas=[{\"creditos\":4,\"obrigatoriedades\":[{\"obrigatoriedade\":\"obrigatoria\",\"curso_id\":250}],\"nome\":\"Aerodinamica I A-Noturno (Sao Bernardo)\",\"campus\":18,\"recomendacoes\":null,\"codigo\":\"ESTS016-17\",\"vagas\":86,\"nome_campus\":\"Campus Sao Bernardo do Campo\",\"vagas_ingressantes\":null,\"horarios\":[{\"horas\":[\"19:00\",\"19:30\",\"20:00\",\"20:30\",\"21:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":2},{\"horas\":[\"21:00\",\"21:30\",\"22:00\",\"22:30\",\"23:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":4}],\"id\":8220,\"tpi\":[4,0,5]},{\"creditos\":4,\"obrigatoriedades\":[{\"obrigatoriedade\":\"limitada\",\"curso_id\":250}],\"nome\":\"Aerodinamica II A-Matutino (Sao Bernardo)\",\"campus\":18,\"recomendacoes\":null,\"codigo\":\"ESZS019-17\",\"vagas\":40,\"nome_campus\":\"Campus Sao Bernardo do Campo\",\"vagas_ingressantes\":null,\"horarios\":[{\"horas\":[\"10:00\",\"10:30\",\"11:00\",\"11:30\",\"12:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":2},{\"horas\":[\"08:00\",\"08:30\",\"09:00\",\"09:30\",\"10:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":4}],\"id\":8228,\"tpi\":[4,0,5]}];\n"})

(deftest coerce-registrations
  (is (= {2034 [8992]
          4994 [8595 8492]
          526 [8417 8662 9190]}
         (http/coerce-registrations (http/parse-response registrations-sample)))))

(deftest coerce-registrations-count
  (is (= {8290 77
          8623 1
          8682 94}
         (http/coerce-registrations-count (http/parse-response registrations-count-sample)))))

(deftest http-get!
  (with-redefs [clj-http.client/get (constantly {:body "html" :status 200})]
    (is (let [{:keys [body status]} (http/http-get! "https://www.google.com")]
          (and (string? body)
               (= 200 status))))))

(deftest bookmarks->url-test
  (let [bookmark-settings {:foo {:url "foo"}
                           :bar {:url "bar"}}
        bookmark :foo]
    (is (= "foo" (http/bookmarks->url bookmark bookmark-settings)))))

(deftest parse-response
  (is (match? (m/equals {"2034" ["8992"], "4994" ["8595" "8492"], "526" ["8417" "8662" "9190"]})
              (http/parse-response registrations-sample))
      "Can parse registrations")

  (is (match? (m/equals {"8682" "94", "8623" "1", "8290" "77"})
              (http/parse-response registrations-count-sample))
      "Can parse registrations count")

  (is (match? (m/embeds [{"vagas" 86
                          "campus" 18
                          "id" 8220
                          "horarios" [{"horas" ["19:00" "19:30" "20:00" "20:30" "21:00"]
                                       "periodicidade_extenso" " - semanal" "semana" 2}
                                      {"horas" ["21:00" "21:30" "22:00" "22:30" "23:00"]
                                       "periodicidade_extenso" " - semanal" "semana" 4}]
                          "nome" "Aerodinamica I A-Noturno (Sao Bernardo)"
                          "creditos" 4
                          "obrigatoriedades" [{"obrigatoriedade" "obrigatoria" "curso_id" 250}]
                          "recomendacoes" nil
                          "tpi" [4 0 5]
                          "nome_campus" "Campus Sao Bernardo do Campo"
                          "vagas_ingressantes" nil
                          "codigo" "ESTS016-17"}])
              (http/parse-response courses-sample))
      "Can parse all courses"))

(stest/unstrument)
