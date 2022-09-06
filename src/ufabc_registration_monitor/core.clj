(ns ufabc-registration-monitor.core
  (:gen-class)
  (:require [clojure.data :refer [diff]]
            [ufabc-registration-monitor.http-client :as http]
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
      (slack-post-message-fn! {:channel "#random" :text "Nothing changed"})
      (doseq [[course-id registration-count] (get-updates registrations-count updated-registrations-count)]
        (some-> (maybe-alert course-id registration-count courses monitored-ids)
                (utils/log! log-fn!)
                slack-post-message-fn!)))
    updated-registrations-count))

(defn start-worker!
  [{:keys [log-fn! slack-post-message-fn! sleep-fn! active?] :as system}]
  (try
    (slack-post-message-fn! {:channel "#random" :text "Starting!"})
    (let [courses (http/get-bookmark! :courses http/bookmark-settings system)
          initial-registration-count (http/get-bookmark! :registrations-count http/bookmark-settings system)]
      (loop [registrations-count initial-registration-count]
        (sleep-fn! 1000)
        (when @active?
          (recur (check-updates! courses registrations-count system)))))
    (catch Exception ex (utils/log-exception! ex log-fn!))))

(defn -main
  [& args]
  (println "Starting...")
  (start-worker! utils/main-system))
