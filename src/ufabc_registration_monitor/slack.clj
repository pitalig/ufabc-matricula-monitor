(ns ufabc-registration-monitor.slack)

(def connection {:api-url "https://slack.com/api"
                 :token (System/getenv "slack_token")})

(defn message!
  ([{:keys [channel text]} slack-post-message-fn!] (message! channel text slack-post-message-fn!))
  ([channel text slack-post-message-fn!]
   (try (slack-post-message-fn! connection channel text {:username "Matrícula Bot"})
        (catch Exception ex (println {:exception/data (ex-data ex)
                                      :exception/message (ex-message ex)
                                      :exception/cause (ex-cause ex)})))))

(defn log-exception! [ex slack-post-message-fn!]
  (slack-post-message-fn! "#random"
                          (str ":fire: :fire: :fire: :fire: \n"
                               "data: " (ex-data ex) "\n"
                               "message: " (ex-message ex) "\n"
                               "cause: " (ex-cause ex) "\n"))
  ex)
