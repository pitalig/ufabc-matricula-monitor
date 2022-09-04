(ns ufabc-registration-monitor.http-client
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [ufabc-registration-monitor.utils :as utils]
            [clojure.string :as string]))

(defn coerce-registrations-count [parsed-response]
  (->> parsed-response
       (map (fn [[k v]]
              [(utils/parse-int k) (utils/parse-int v)]))
       (into {})))

(defn coerce-courses [parsed-response]
  (let [parse-course (fn [course] {:id (:id course)
                                   :name (:nome course)
                                   :slots (:vagas course)})]
    (map parse-course parsed-response)))

(def bookmark-settings
  {:registrations-count {:used true
                         :url "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Map where the keys are course ids and the value is the number of registered students in that course"
                         :sample-path "resources/registrations_count_sample.txt"
                         :small-sample {:body "contagemMatriculas={\"8682\":\"94\",\"8623\":\"1\",\"8290\":\"77\"};\n"}
                         :coerce-fn coerce-registrations-count
                         :json-coerce-key-fn false}
   :courses {:used true
             :url "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
             :doc "List of maps with information about each course"
             :sample-path "resources/courses_sample.txt"
             :small-sample {:body "todasDisciplinas=[{\"creditos\":4,\"obrigatoriedades\":[{\"obrigatoriedade\":\"obrigatoria\",\"curso_id\":250}],\"nome\":\"Aerodinamica I A-Noturno (Sao Bernardo)\",\"campus\":18,\"recomendacoes\":null,\"codigo\":\"ESTS016-17\",\"vagas\":86,\"nome_campus\":\"Campus Sao Bernardo do Campo\",\"vagas_ingressantes\":null,\"horarios\":[{\"horas\":[\"19:00\",\"19:30\",\"20:00\",\"20:30\",\"21:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":2},{\"horas\":[\"21:00\",\"21:30\",\"22:00\",\"22:30\",\"23:00\"],\"periodicidade_extenso\":\" - semanal\",\"semana\":4}],\"id\":8220,\"tpi\":[4,0,5]}];\n"}
             :coerce-fn coerce-courses
             :json-coerce-key-fn true}})

(s/fdef get!
  :args (s/cat :url ::url
               :kwargs (s/keys* :req-un [::max-retries ::base-interval-sec]))
  :ret (s/keys :opt [::body ::status]))
(defn get!
  "Send a http get to an url.
   If it fails, will retry `max-retries` times with an incremental sleep between retries."
  [url http-get-fn! & {:keys [max-retries base-interval-sec]
                       :or {max-retries 5, base-interval-sec 5}}]
  (loop [t 0]
    (let [result (try (http-get-fn! url {:insecure? true})
                      (catch Exception e
                        (if (< max-retries t)
                          (throw e)
                          nil)))]
      (or
        result
        (do
          (Thread/sleep (* t base-interval-sec 1000))
          (recur (inc t)))))))

(s/fdef parse-response
  :args (s/cat :raw-response (s/keys :req-un [::body])))
(defn parse-response [raw-response coerce-fn json-coerce-key-fn]
  (-> raw-response
      :body
      (string/replace-first #".*=" "")
      (string/replace-first #"\n" "")
      (json/parse-string json-coerce-key-fn)
      coerce-fn))

(defn get-bookmark! [bookmark-key bookmarks http-get-fn!]
  (let [{:keys [url coerce-fn json-coerce-key-fn]} (get bookmarks bookmark-key)]
    (-> (get! url http-get-fn!)
        (parse-response coerce-fn json-coerce-key-fn))))
