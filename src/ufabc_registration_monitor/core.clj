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
  (first (filter #(= id (:id %)) courses))
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
        open-slots (- (:slots course) registration-count)
        message (str (:name course) " has " open-slots " slots!")]
    (cond
      (<= open-slots 0) nil
      (monitored-ids (:id course)) {:channel "#general" :text message}
      :else {:channel "#random" :text message})))

(defn get-updates [old new]
  (second (diff old new)))

(defn verify-and-alert! [old-registrations new-registrations courses]
  (println "Changes!")
  ; TODO Remove this (first), it's just a test
  (doseq [[course-id registration-count] [(first (get-updates old-registrations new-registrations))]]
    (some-> (alert-for-open-slots course-id registration-count courses)
            utils/log!
            slack/message!)))

(defn start! []
  (slack/message! "#random" "Starting!")
  (try
    (let [courses (http/get-bookmark! :courses http/bookmark-settings)]
      (loop [count {} #_(http/get-bookmark! :registrations-count http/bookmark-settings)]
        (Thread/sleep 2000) ; Todo increase sleep
        (let [updated-count (http/get-bookmark! :registrations-count http/bookmark-settings)]
          (if (= count updated-count)
            (println "Nothing changed")
            (verify-and-alert! count updated-count courses))
          (recur updated-count))))
    (catch Exception ex (do (utils/log-exception! ex)
                            (slack/log-exception! ex)))))

(defn -main
  [& args]
  (println "Starting...")
  (start!))
