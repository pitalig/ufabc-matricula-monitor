(ns ufabc-registration-monitor.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [ufabc-registration-monitor.core :as core]
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

(deftest parse-int
  (is (= 123 (core/parse-int "123")))
  (is (= 123 (core/parse-int "aaa123")))
  (is (= nil (core/parse-int "aaa"))))

(def registrations-sample {:body "matriculas={\"2034\":[\"8992\"],\"4994\":[\"8595\",\"8492\"],\"526\":[\"8417\",\"8662\",\"9190\"]};"})
(def registrations-count-sample {:body "contagemMatriculas={\"8682\":\"94\",\"8623\":\"1\",\"8290\":\"77\"};"})
(def courses-sample {:body "todasDisciplinas=[{\"creditos\":4,\"obrigatoriedades\":[{\"obrigatoriedade\":\"obrigatoria\",\"curso_id\":250}],\"nome\":\"Aerodinamica I A-Noturno (Sao Bernardo)\",\"campus\":18,\"recomendacoes\":null,\"codigo\":\"ESTS016-17\",\"vagas\":86,\"nome_campus\":\"Campus Sao Bernardo do Campo\",\"vagas_ingressantes\":null,\"horarios\":[{\"horas\":[\"19:00\",\"19:30\",\"20:00\",\"20:30\",\"21:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":2},{\"horas\":[\"21:00\",\"21:30\",\"22:00\",\"22:30\",\"23:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":4}],\"id\":8220,\"tpi\":[4,0,5]},{\"creditos\":4,\"obrigatoriedades\":[{\"obrigatoriedade\":\"limitada\",\"curso_id\":250}],\"nome\":\"Aerodinamica II A-Matutino (Sao Bernardo)\",\"campus\":18,\"recomendacoes\":null,\"codigo\":\"ESZS019-17\",\"vagas\":40,\"nome_campus\":\"Campus Sao Bernardo do Campo\",\"vagas_ingressantes\":null,\"horarios\":[{\"horas\":[\"10:00\",\"10:30\",\"11:00\",\"11:30\",\"12:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":2},{\"horas\":[\"08:00\",\"08:30\",\"09:00\",\"09:30\",\"10:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":4}],\"id\":8228,\"tpi\":[4,0,5]}];\n"})

(deftest coerce-registrations
  (is (= {2034 [8992]
          4994 [8595 8492]
          526 [8417 8662 9190]}
         (core/coerce-registrations (core/parse-response registrations-sample)))))

(deftest coerce-registrations-count
  (is (= {8290 77
          8623 1
          8682 94}
         (core/coerce-registrations-count (core/parse-response registrations-count-sample)))))

(deftest http-get!
  (with-redefs [http/get (constantly {:body "html" :status 200})]
    (is (let [{:keys [body status]} (core/http-get! "https://www.google.com")]
          (and (string? body)
               (= 200 status))))))

(deftest bookmarks->url-test
  (let [bookmark-settings {:foo {:url "foo"}
                           :bar {:url "bar"}}
        bookmark :foo]
    (is (= "foo" (core/bookmarks->url bookmark bookmark-settings)))))

(deftest parse-response
  (is (match? (m/equals {"2034" ["8992"], "4994" ["8595" "8492"], "526" ["8417" "8662" "9190"]})
              (core/parse-response registrations-sample))
      "Can parse registrations")

  (is (match? (m/equals {"8682" "94", "8623" "1", "8290" "77"})
              (core/parse-response registrations-count-sample))
      "Can parse registrations count")

  (is (match? (m/embeds [{"vagas" 86,
                          "campus" 18,
                          "id" 8220,
                          "horarios" [{"horas" ["19:00" "19:30" "20:00" "20:30" "21:00"], "periodicidade_extenso" " - semanal", "semana" 2}
                                      {"horas" ["21:00" "21:30" "22:00" "22:30" "23:00"], "periodicidade_extenso" " - semanal", "semana" 4}],
                          "nome" "Aerodinamica I A-Noturno (Sao Bernardo)",
                          "creditos" 4,
                          "obrigatoriedades" [{"obrigatoriedade" "obrigatoria", "curso_id" 250}],
                          "recomendacoes" nil,
                          "tpi" [4 0 5],
                          "nome_campus" "Campus Sao Bernardo do Campo",
                          "vagas_ingressantes" nil,
                          "codigo" "ESTS016-17"}])
              (core/parse-response courses-sample))
      "Can parse all courses"))

(stest/unstrument)
