(ns ufabc-registration-monitor.slack
  (:require [clj-slack.chat :as chat]))

(def connection {:api-url "https://slack.com/api"
                 :token (System/getenv "slack_token")})

(defn message! [channel text]
  (try (chat/post-message connection channel text {:username "Matrícula Bot"})
       (catch Exception ex (println (ex-data ex)))))
