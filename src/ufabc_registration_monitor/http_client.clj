(ns ufabc-registration-monitor.http-client
  (:require [cheshire.core :as json]
            [ufabc-registration-monitor.utils :as utils]
            [clojure.string :as string]))

(defn coerce-registrations-count [json-response]
  (->> json-response
       json/parse-string
       (map (fn [[k v]]
              [(utils/parse-int k) (utils/parse-int v)]))
       (into {})))

(defn coerce-courses [json-response]
  (let [parsed-response (json/parse-string json-response true)
        parse-course (fn [course] {:id (:id course)
                                   :name (:nome course)
                                   :slots (:vagas course)})]
    (map parse-course parsed-response)))

(defn parse-response [raw-response coerce-fn]
  (-> raw-response
      :body
      (string/replace-first #".*=" "")
      (string/replace-first #"\n" "")
      coerce-fn))

(def bookmark-settings
  {:registrations-count {:url "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Map where the keys are course ids and the value is the number of registered students in that course"
                         :coerce-fn coerce-registrations-count
                         :sample {:body "contagemMatriculas={\"8682\":\"94\",\"8623\":\"1\",\"8290\":\"77\"};\n"}
                         :coerced-sample {8682 94 8623 1 8290 77}}

   :courses {:url "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
             :doc "List of maps with information about each course"
             :coerce-fn coerce-courses
             :sample {:body "todasDisciplinas=[{\"horarios\":[{\"horas\":[\"19:00\",\"19:30\",\"20:00\",\"20:30\",\"21:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":2},{\"horas\":[\"21:00\",\"21:30\",\"22:00\",\"22:30\",\"23:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":4}],\"nome\":\"Aerodinamica I A-Noturno (Sao Bernardo)\",\"vagas\":86,\"creditos\":4,\"obrigatoriedades\":[{\"curso_id\":250,\"obrigatoriedade\":\"obrigatoria\"}],\"vagas_ingressantes\":null,\"codigo\":\"ESTS016-17\",\"id\":8220,\"nome_campus\":\"Campus Sao Bernardo do Campo\",\"recomendacoes\":null,\"campus\":18,\"tpi\":[4,0,5]},{\"id\":8221,\"nome\":\"Aerodinamica I B-Noturno (Sao Bernardo)\",\"vagas\":86}];\n"}
             :coerced-sample  [{:id 8220 :name "Aerodinamica I A-Noturno (Sao Bernardo)" :slots 86}
                               {:id 8221 :name "Aerodinamica I B-Noturno (Sao Bernardo)" :slots 86}]}})

(defn get!
  "Send a http get to an url.
   If it fails, will retry `max-retries` times with an incremental sleep between retries."
  [url {:keys [http-get-fn! sleep-fn!]}
   & {:keys [max-retries base-interval-sec]
      :or {max-retries 5, base-interval-sec 5}}]
  (loop [t 0]
    (let [result (try (http-get-fn! url {:insecure? true})
                      (catch Exception e
                        (if (< max-retries t) (throw e) nil)))]
      (or result
          (do
            (sleep-fn! (* t base-interval-sec 1000))
            (recur (inc t)))))))

(defn get-bookmark! [bookmark-key bookmarks system]
  (let [{:keys [url coerce-fn]} (get bookmarks bookmark-key)]
    (-> (get! url system)
        (parse-response coerce-fn))))
