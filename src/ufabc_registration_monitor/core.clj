(ns ufabc-registration-monitor.core
  (:gen-class)
  (:require [ufabc-registration-monitor.slack :as slack]
            [ufabc-registration-monitor.utils :as utils]
            [ufabc-registration-monitor.http-client :as http]
            [clj-slack.chat]
            [clojure.data :refer [diff]]
            [clojure.spec.alpha :as s]
            [clj-http.client]))

(s/def ::url string?)
(s/def ::max-retries int?)
(s/def ::base-interval-sec int?)
(s/def ::body string?)
(s/def ::status int?)

(defn alert-for-open-slots [course-id registration-count courses monitored-ids]
  (let [course (first (filter #(= course-id (:id %)) courses))
        open-slots (- (:slots course) registration-count)]
    (when (> open-slots 0)
      {:channel (if (monitored-ids course-id) "#general" "#random")
       :text (str (:name course) " has " open-slots " slots!")})))

(defn get-updates [old new]
  (second (diff old new)))

(defn alert-open-slots!
  [old-registrations new-registrations courses monitored-ids
   {:keys [log-fn! slack-post-message-fn!]}]
  (println "Changes!")
  ; TODO Remove this (first), it's just to reduce noise while testing
  (doseq [[course-id registration-count] [(first (get-updates old-registrations new-registrations))]]
    (some-> (alert-for-open-slots course-id registration-count courses monitored-ids)
            (utils/log! log-fn!)
            (slack/message! slack-post-message-fn!))))

(defn start! [{:keys [log-fn! http-get-fn! slack-post-message-fn! sleep-fn!] :as effects}]
  (slack/message! "#random" "Starting!" slack-post-message-fn!)
  (try
    (let [monitored-ids #{670 ; Fenômenos de Transporte A-noturno (Santo André)
                          809 ; Materiais e Suas Propriedades A1-noturno (Santo André)
                          679 ; Fenômenos de Transporte B1-noturno (São Bernardo)
                          931 ; Habitação e Assentamentos Humanos A-noturno (Santo André)
                          425 ; Ecologia do Ambiente Urbano A-noturno (Santo André)
                          440 ; Compostagem A-noturno (Santo André)
                          }
          courses (http/get-bookmark! :courses http/bookmark-settings effects)]
      (loop [count {} #_(http/get-bookmark! :registrations-count http/bookmark-settings effects)]
        (sleep-fn! 2000) ; Todo increase sleep
        (let [updated-count (http/get-bookmark! :registrations-count http/bookmark-settings effects)]
          (if (= count updated-count)
            (log-fn! "Nothing changed")
            (alert-open-slots! count updated-count courses monitored-ids effects))
          (recur updated-count))))
    (catch Exception ex (do (utils/log-exception! ex log-fn!)
                            (slack/log-exception! ex slack-post-message-fn!)))))

(defn -main
  [& args]
  (println "Starting...")
  (start! {:http-get-fn! clj-http.client/get
           :slack-post-message-fn! clj-slack.chat/post-message
           :log-fn! println
           :sleep-fn! (fn [milis] (println (str "Sleep " milis)) #_(Thread/sleep milis))}))
