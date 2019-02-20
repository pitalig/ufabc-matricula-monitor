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

(def discovery
  {:matriculas {:url "https://matricula.ufabc.edu.br/cache/matriculas.js"
                :doc "Mapa com lista de disciplinas matrículadas para cada id de aluno"
                :eg-path "resources/matriculas_sample.txt"
                :coerce-fn coerce-matriculas}
   :contagem-matriculas {:url "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Mapa de número de requisições por disciplina"
                         :eg-path "resources/contagem_matriculas_sample.txt"
                         :coerce-fn coerce-contagem-matriculas}
   :todas-disciplinas {:url "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
                       :doc "Lista de informações das disciplinas"
                       :eg-path "resources/todas_disciplinas_sample.txt"}})

(defn alert-error! [exception]
  (slack/message "#random" (str "ERRO: \n" (.getMessage exception)))
  (println (str "Raw error:\n"
             exception
             "\nError message:\n"
             (.getMessage exception)
             "\nError data:\n"
             (ex-data exception))))

(s/fdef secure-get!
  :args (s/cat :url ::url
               :kwargs (s/keys* :req-un [::max-retries ::base-interval-sec]))
  :ret (s/keys :opt [::body ::status]))
(defn secure-get!
  "Send a http get to an url.
   If it fails, will retry `max-retries` times with an exponential sleep betwen retries."
  [url & {:keys [max-retries base-interval-sec]
          :or {max-retries 5, base-interval-sec 5}}]
  (loop [t 0]
    (let [result (try (http/get url)
                      (catch Exception e
                        (if (< max-retries t)
                          (throw e)
                          nil)))]
      (or
        result
        (do
          (Thread/sleep (* t t base-interval-sec 1000))
          (recur (inc t)))))))

(defn secure-get-endpoint! [endpoint]
  (-> discovery endpoint :url secure-get!))

(s/fdef parse-response
  :args (s/cat :raw-response (s/keys :req-un [::body])))
(defn parse-response [raw-response]
  (-> raw-response
      :body
      (replace-first #".*=" "")
      json/parse-string))

(def disciplinas
  (delay (-> (secure-get-endpoint! :todas-disciplinas)
             :body
             (replace-first #"todasDisciplinas=" "")
             (replace-first #"\n" "")
             (json/parse-string true))))

(defn id->disciplina [id]
  (first (filter #(= id (str (:id %))) @disciplinas)))

(def important-ids
  #{8731 ; Engenharia Laboral
    8749 ; Estatística Aplicada a Sistemas de Gestão
    9037 ; Estratégias de Comunicação Organizacional
    8717 ; Modelos de Decisão Multicritério
    8526 ; Introdução aos Processos de Fabricação Metal - Mecânico
    8532 ; Introdução aos Processos de Fabricação Metal - Mecânico
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
               (= (:nome_campus disciplina) "Campus Santo André")
               (re-find #"Noturno" (:nome disciplina)))
      (alert! disciplina open-slots))))

(defn get-updates [old new]
  (second (diff old new)))

(defn verify-and-alert! [old new]
  (doseq [cont (get-updates old new)] (alert-when-open-slots! cont)))

(defn start! []
  (slack/message "#random" "Starting!")
  (try (loop [contagem (parse-response (secure-get-endpoint! :contagem-matriculas))]
         (let [updated-contagem (parse-response (secure-get-endpoint! :contagem-matriculas))]
           (if (= contagem updated-contagem)
             (println "Nothing changed")
             (do (println "Changes!")
                 (verify-and-alert! contagem updated-contagem)))
           (Thread/sleep 15000)
           (recur updated-contagem)))
       (catch Exception ex (do (println (ex-data ex)) (slack/message "#general" (str ":fire: :fire: :fire: :fire: \n" (ex-data ex)))))))

(defn -main
  [& args]
  (println "Starting...")
  (start!))
