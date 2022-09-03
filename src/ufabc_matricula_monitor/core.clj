(ns ufabc-matricula-monitor.core
  (:gen-class)
  (:require [ufabc-matricula-monitor.slack :as slack]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :refer (replace-first)]
            [clojure.data :refer [diff]]
            [clojure.spec.alpha :as s]))

(s/def ::url string?)
(s/def ::max-retries int?)
(s/def ::base-interval-sec int?)
(s/def ::body string?)
(s/def ::status int?)

(defn map-keys [m f]
  (reduce-kv #(assoc %1 (f %2) %3) {} m))

(defn map-vals [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn map-kv [m f]
  (reduce-kv #(assoc %1 (f %2) (f %3)) {} m))

(defn parse-int [s]
  (some->> s
           (re-find #"\d+")
           (Integer.)))

(defn coerce-matriculas [parsed-response]
  (-> parsed-response
      (map-keys parse-int)
      (map-vals #(map parse-int %))))

(defn coerce-contagem-matriculas [parsed-response]
  (-> parsed-response
      (map-kv parse-int)))

(def bookmark-settings
  {:matriculas {:url "https://matricula.ufabc.edu.br/cache/matriculas.js"
                :doc "Mapa com lista de disciplinas matrículadas para cada id de aluno"
                :sample-path "resources/matriculas_sample.txt"
                :coerce-fn coerce-matriculas}
   :contagem-matriculas {:url "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Mapa de número de requisições por disciplina"
                         :sample-path "resources/contagem_matriculas_sample.txt"
                         :coerce-fn coerce-contagem-matriculas}
   :todas-disciplinas {:url "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
                       :doc "Lista de informações das disciplinas"
                       :sample-path "resources/todas_disciplinas_sample.txt"}})

(defn alert-error! [exception]
  (slack/message "#random" (str "ERRO: \n" (.getMessage exception)))
  (println (str "Raw error:\n"
                exception
                "\nError message:\n"
                (.getMessage exception)
                "\nError data:\n"
                (ex-data exception))))

(s/fdef http-get!
  :args (s/cat :url ::url
               :kwargs (s/keys* :req-un [::max-retries ::base-interval-sec]))
  :ret (s/keys :opt [::body ::status]))
(defn http-get!
  "Send a http get to an url.
   If it fails, will retry `max-retries` times with an incremental sleep between retries."
  [url & {:keys [max-retries base-interval-sec]
          :or {max-retries 5, base-interval-sec 5}}]
  (loop [t 0]
    (let [result (try (http/get url {:insecure? true})
                      (catch Exception e
                        (if (< max-retries t)
                          (throw e)
                          nil)))]
      (or
        result
        (do
          (Thread/sleep (* t base-interval-sec 1000))
          (recur (inc t)))))))

(defn bookmarks->url [endpoint bookmarks]
  (-> bookmarks endpoint :url))

(s/fdef parse-response
  :args (s/cat :raw-response (s/keys :req-un [::body])))
(defn parse-response [raw-response]
  (-> raw-response
      :body
      (replace-first #".*=" "")
      json/parse-string))

(def disciplinas
  (delay (-> :todas-disciplinas
             (bookmarks->url bookmark-settings)
             http-get!
             :body
             (replace-first #"todasDisciplinas=" "")
             (replace-first #"\n" "")
             (json/parse-string true))))

(defn id->disciplina [id]
  (first (filter #(= id (str (:id %))) @disciplinas)))

(def important-ids
  #{670 ; Fenômenos de Transporte A-noturno (Santo André)
    809 ; Materiais e Suas Propriedades A1-noturno (Santo André)
    679 ; Fenômenos de Transporte B1-noturno (São Bernardo)
    931 ; Habitação e Assentamentos Humanos A-noturno (Santo André)
    425 ; Ecologia do Ambiente Urbano A-noturno (Santo André)
    440 ; Compostagem A-noturno (Santo André)
    })

(defn alert! [disciplina open-slots]
  (let [msg (str (:nome disciplina) " tem " open-slots " vagas!")]
    (println msg)
    (slack/message "#random" msg)
    (when (important-ids (:id disciplina))
      (slack/message "#general" msg))))

(defn alert-when-open-slots! [[id req]]
  (let [disciplina (id->disciplina id)
        open-slots (- (:vagas disciplina) (parse-int req))]
    (when (and (> open-slots 0)
               #_(= (:nome_campus disciplina) "Campus Santo André")
               #_(re-find #"Noturno" (:nome disciplina)))
      (alert! disciplina open-slots))))

(defn get-updates [old new]
  (second (diff old new)))

(defn verify-and-alert! [old new]
  (doseq [cont (get-updates old new)] (alert-when-open-slots! cont)))

(defn log-exception! [ex]
  (println {:exception/data (ex-data ex)
            :exception/message (ex-message ex)
            :exception/cause (ex-cause ex)})
  (slack/message "#random"
                 (str ":fire: :fire: :fire: :fire: \n"
                      {:exception/data (ex-data ex)
                       :exception/message (ex-message ex)
                       :exception/cause (ex-cause ex)})))

(defn start! []
  (slack/message "#random" "Starting!")
  (try (loop [contagem (-> :contagem-matriculas
                           (bookmarks->url bookmark-settings)
                           http-get!
                           parse-response)]
         (let [updated-contagem (-> :contagem-matriculas
                                    (bookmarks->url bookmark-settings)
                                    http-get!
                                    parse-response)]
           (if (= contagem updated-contagem)
             (println "Nothing changed")
             (do (println "Changes!")
                 (verify-and-alert! contagem updated-contagem)))
           (Thread/sleep 10000)
           (recur updated-contagem)))
       (catch Exception ex (log-exception! ex))))

(defn -main
  [& args]
  (println "Starting...")
  (start!))
