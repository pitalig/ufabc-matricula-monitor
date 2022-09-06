(ns ufabc-registration-monitor.slack
  (:require [ufabc-registration-monitor.utils :as utils]))

; Todo move to system
(def connection {:api-url "https://slack.com/api"
                 :token "xoxb-307935473364-4033970968818-WWQr5o5gB5gG5DqrNaTtOqZH"})

(defn message!
  ([{:keys [channel text]} slack-post-message-fn!] (message! channel text slack-post-message-fn!))
  ([channel text slack-post-message-fn!]
   (slack-post-message-fn! connection channel text {:username "Matrícula Bot"})))

(defn log-exception! [ex slack-post-message-fn!]
  (slack-post-message-fn! "#random"
                          (str "Exception! \n"
                               "data: " (ex-data ex) "\n"
                               "message: " (ex-message ex) "\n"
                               "cause: " (ex-cause ex) "\n"))
  ex)

(comment
  (message! "#general" "foo" (:slack-post-message-fn! utils/main-system)))
