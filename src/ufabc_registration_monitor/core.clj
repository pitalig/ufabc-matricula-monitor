(ns ufabc-registration-monitor.core
  (:gen-class)
  (:require [clojure.data :refer [diff]]
            [ufabc-registration-monitor.http-client :as http]
            [ufabc-registration-monitor.slack :as slack]
            [ufabc-registration-monitor.utils :as utils]))

(defn maybe-alert [course-id registration-count courses monitored-ids]
  (let [course (first (filter #(= course-id (:id %)) courses))
        open-slots (- (:slots course) registration-count)]
    (when (> open-slots 0)
      {:channel (if (monitored-ids course-id) "#general" "#random")
       :text (str (:name course) " has " open-slots " slots!")})))

(defn get-updates [old new]
  (second (diff old new)))

(defn check-updates! [courses registrations-count {:keys [log-fn! monitored-ids slack-post-message-fn!] :as system}]
  (let [updated-registrations-count (http/get-bookmark! :registrations-count http/bookmark-settings system)]
    (if (= registrations-count updated-registrations-count)
      (log-fn! "Nothing changed")
      (doseq [[course-id registration-count] (get-updates registrations-count updated-registrations-count)]
        (some-> (maybe-alert course-id registration-count courses monitored-ids)
                (utils/log! log-fn!)
                (slack/message! slack-post-message-fn!))))
    updated-registrations-count))

(defn start-worker! [{:keys [log-fn! slack-post-message-fn! sleep-fn! recur?] :as system}]
  (try
    (slack/message! "#random" "Starting!" slack-post-message-fn!)
    (let [courses (http/get-bookmark! :courses http/bookmark-settings system)]
      (loop [registrations-count (http/get-bookmark! :registrations-count http/bookmark-settings system)]
        (sleep-fn! 1000)
        (when @recur?
          (recur (check-updates! courses registrations-count system)))))
    (catch Exception ex (do (utils/log-exception! ex log-fn!)
                            (slack/log-exception! ex slack-post-message-fn!)))))

(defn -main
  [& args]
  (println "Starting...")
  (start-worker! utils/main-system))
