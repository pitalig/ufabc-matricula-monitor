(ns ufabc-matricula-monitor.core
  (:gen-class)
  (:require [ufabc-matricula-monitor.slack :as slack]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :refer (replace-first)]
            [clojure.data :refer [diff]]))

(def discovery
  {:matriculas {:endpoint "https://matricula.ufabc.edu.br/cache/matriculas.js"
                :doc "PROVAVELMENTE: mapa com lista de disciplinas matrículadas para cada id de aluno"}
   :contagem-matriculas {:endpoint "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Mapa de número de requisições por disciplina"
                         :eg {"825" "90"}}
   :todas-disciplinas {:endpoint "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
                       :doc "Lista de informações das disciplinas"
                       :eg [{:horarios [{:horas ["21:00" "21:30" "22:00" "22:30" "23:00"]
                                         :periodicidade_extenso " - semanal"
                                         :semana 2}
                                        {:horas ["19:00" "19:30" "20:00" "20:30" "21:00"]
                                         :periodicidade_extenso " - quinzenal (I)"
                                         :semana 4}
                                        {:horas ["19:00" "19:30" "20:00" "20:30" "21:00"]
                                         :periodicidade_extenso " - quinzenal (II)"
                                         :semana 4}]
                             :nome "Visão Computacional A-Noturno (Santo André)"
                             :vagas 31
                             :creditos 4
                             :obrigatoriedades [{:curso_id 1
                                                 :obrigatoriedade "limitada"}
                                                {:curso_id 17
                                                 :obrigatoriedade "limitada"}]
                             :vagas_ingressantes nil
                             :codigo "ESZA019-17"
                             :id 825
                             :nome_campus "Campus Santo André"
                             :recomendacoes nil
                             :campus 1
                             :tpi [3 1 4]}]}})

(defn get-endpoint [key]
  (-> discovery key :endpoint))

(defn alert-error! [exception]
  (slack/message "#random" (str "ERRO: \n" (.getMessage exception)))
  (println (str "Raw error:\n"
             exception
             "\nError message:\n"
             (.getMessage exception)
             "\nError data:\n"
             (ex-data exception))))

(defn secure-get! [url & {:keys [max-retries base-interval-sec]
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

; try to make this req/parse as a separate and generic function for all endpoints with request timeout and try-catch
(defn parse-matriculas []
  (-> (get-endpoint :matriculas)
    secure-get!
    :body
    (replace-first #"matriculas=" "")
    (replace-first #"\n" "")
    json/parse-string))

(defn parse-contagem []
  (try (-> (get-endpoint :contagem-matriculas)
         secure-get!
         :body
         (replace-first #"contagemMatriculas=" "")
         (replace-first #"\n" "")
         json/parse-string)
       (catch Exception e (do (alert-error! e)
                              (Thread/sleep 15000)
                              (parse-contagem)))))

(def disciplinas
  (delay (-> (get-endpoint :todas-disciplinas)
           (secure-get!)
           :body
           (replace-first #"todasDisciplinas=" "")
           (replace-first #"\n" "")
           (json/parse-string true))))

(defn id->disciplina [id]
  (first (filter #(= id (str (:id %))) @disciplinas)))

(defn parse-int [s]
  (Integer. (re-find #"\d+" s)))

(def important-ids
  #{547 480 443 539 551 28 133 315 147})

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
  (try (loop [contagem (parse-contagem)]
         (let [updated-contagem (parse-contagem)]
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
