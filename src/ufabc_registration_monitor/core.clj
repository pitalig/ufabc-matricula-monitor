(ns ufabc-registration-monitor.core
  (:gen-class)
  (:require [ufabc-registration-monitor.slack :as slack]
            [ufabc-registration-monitor.utils :as utils]
            [ufabc-registration-monitor.http-client :as http]
            [cheshire.core :as json]
            [clojure.data :refer [diff]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def ::url string?)
(s/def ::max-retries int?)
(s/def ::base-interval-sec int?)
(s/def ::body string?)
(s/def ::status int?)

(defn id->course [id courses]
  (first (filter #(= id (str (:id %))) courses))
  ; TODO: Throw when not found
  )

(def monitored-ids
  #{670 ; Fenômenos de Transporte A-noturno (Santo André)
    809 ; Materiais e Suas Propriedades A1-noturno (Santo André)
    679 ; Fenômenos de Transporte B1-noturno (São Bernardo)
    931 ; Habitação e Assentamentos Humanos A-noturno (Santo André)
    425 ; Ecologia do Ambiente Urbano A-noturno (Santo André)
    440 ; Compostagem A-noturno (Santo André)
    })

(defn alert-for-open-slots [course-id registration-count courses]
  (let [course (id->course course-id courses)
        open-slots (- (:vagas course) (utils/parse-int registration-count))
        message (str (:nome course) " has " open-slots " slots!")]
    (cond
      (<= open-slots 0) nil
      (monitored-ids (:id course)) {:channel "#general" :text message}
      :else {:channel "#random" :text message})))

(defn get-updates [old new]
  (second (diff old new)))

(defn verify-and-alert! [old-registrations new-registrations courses]
  (println "Changes!")
  (doseq [[course-id registration-count] (get-updates old-registrations new-registrations)]
    (some-> (alert-for-open-slots course-id registration-count courses)
            utils/log!
            slack/message!)))

(defn start! []
  (slack/message! "#random" "Starting!")
  (try
    (let [courses (-> :courses
                      (http/bookmarks->url http/bookmark-settings)
                      http/http-get! ;; TODO: Make a get fn that just receive the bookmark key
                      :body
                      (string/replace-first #"todasDisciplinas=" "") ;; TODO: Use parse response here
                      (string/replace-first #"\n" "")
                      (json/parse-string true))]
      (loop [count {}]
        (let [updated-count (-> :registrations-count
                                (http/bookmarks->url http/bookmark-settings)
                                http/http-get!
                                http/parse-response)]
          (if (or #_(empty? count)
                (= count updated-count))
            (println "Nothing changed")
            (verify-and-alert! count updated-count courses))
          (Thread/sleep 10000)
          (recur updated-count))))
    (catch Exception ex (do (utils/log-exception! ex)
                            (slack/log-exception! ex)))))

(defn -main
  [& args]
  (println "Starting...")
  (start!))
