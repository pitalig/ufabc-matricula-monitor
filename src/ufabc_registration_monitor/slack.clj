(ns ufabc-registration-monitor.slack
  (:require [clj-slack.chat :as chat]))

(def connection {:api-url "https://slack.com/api"
                 :token (System/getenv "slack_token")})

(defn message!
  ([{:keys [channel text]}] (message! channel text))
  ([channel text]
   (try (chat/post-message connection channel text {:username "Matrícula Bot"})
        (catch Exception ex (println {:exception/data (ex-data ex)
                                      :exception/message (ex-message ex)
                                      :exception/cause (ex-cause ex)})))))
