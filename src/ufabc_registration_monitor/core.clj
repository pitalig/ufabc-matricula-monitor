(ns ufabc-registration-monitor.core
  (:gen-class)
  (:require [ufabc-registration-monitor.slack :as slack]
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

(defn coerce-registrations [parsed-response]
  (-> parsed-response
      (map-keys parse-int)
      (map-vals #(map parse-int %))))

(defn coerce-registrations-count [parsed-response]
  (-> parsed-response
      (map-kv parse-int)))

(def bookmark-settings
  {:registrations {:url "https://matricula.ufabc.edu.br/cache/matriculas.js"
                   :doc "Map where the keys are student ids and the value is a list with ids of the courses that they are registered"
                   :sample-path "resources/registrations_sample.txt"
                   :coerce-fn coerce-registrations}
   :registrations-count {:url "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Map where the keys are course ids and the value is the number of registered students in that course"
                         :sample-path "resources/registrations_count_sample.txt"
                         :coerce-fn coerce-registrations-count}
   :courses {:url "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
             :doc "List of maps with information about each course"
             :sample-path "resources/courses_sample.txt"}})

(defn alert-error! [exception]
  (slack/message! "#random" (str "ERROR: \n" (.getMessage exception)))
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
      (json/parse-string)))

(defn id->course [id courses]
  (first (filter #(= id (str (:id %))) courses)))

(def monitored-ids
  #{670 ; Fenômenos de Transporte A-noturno (Santo André)
    809 ; Materiais e Suas Propriedades A1-noturno (Santo André)
    679 ; Fenômenos de Transporte B1-noturno (São Bernardo)
    931 ; Habitação e Assentamentos Humanos A-noturno (Santo André)
    425 ; Ecologia do Ambiente Urbano A-noturno (Santo André)
    440 ; Compostagem A-noturno (Santo André)
    })

(defn alert! [course open-slots]
  (let [msg (str (:nome course) " tem " open-slots " vagas!")]
    (println msg)
    (slack/message! "#random" msg)
    (when (monitored-ids (:id course))
      (slack/message! "#general" msg))))

(defn alert-when-open-slots! [[id req] courses]
  (let [course (id->course id courses)
        open-slots (- (:vagas course) (parse-int req))]
    (when (> open-slots 0)
      (alert! course open-slots))))

(defn get-updates [old new]
  (second (diff old new)))

(defn verify-and-alert! [old-registrations new-registrations courses]
  (doseq [registration (get-updates old-registrations new-registrations)]
    (alert-when-open-slots! registration courses)))

(defn log-exception! [ex]
  (println {:exception/data (ex-data ex)
            :exception/message (ex-message ex)
            :exception/cause (ex-cause ex)})
  (slack/message! "#random"
                  (str ":fire: :fire: :fire: :fire: \n"
                      "data: " (ex-data ex) "\n"
                      "message: " (ex-message ex) "\n"
                      "cause: " (ex-cause ex) "\n")))

(defn start! []
  (slack/message! "#random" "Starting!")
  (try
    (let [courses (-> :courses
                      (bookmarks->url bookmark-settings)
                      http-get! ;; TODO: Make a get fn that just receive the bookmark key
                      :body
                      (replace-first #"todasDisciplinas=" "") ;; TODO: Use parse response here
                      (replace-first #"\n" "")
                      (json/parse-string true))]
      (loop [count {}]
        (let [updated-count (-> :registrations-count
                                (bookmarks->url bookmark-settings)
                                http-get!
                                parse-response)]
          (if (or (empty? count)
                  (= count updated-count))
            (println "Nothing changed")
            (do (println "Changes!")
                (verify-and-alert! count updated-count courses)))
          (Thread/sleep 10000)
          (recur updated-count))))
    (catch Exception ex (log-exception! ex))))

(defn -main
  [& args]
  (println "Starting...")
  (start!))
