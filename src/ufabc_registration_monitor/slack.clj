(ns ufabc-registration-monitor.slack
  (:require [ufabc-registration-monitor.utils :as utils]))

(def connection {:api-url "https://slack.com/api"
                 :token "xoxb-307935473364-4033970968818-WWQr5o5gB5gG5DqrNaTtOqZH"})

(defn message!
  [{:keys [channel text]} slack-post-message-fn!]
  (slack-post-message-fn! connection channel text {:username "Matrícula Bot"}))

(comment
  (message! {:channel "#general" :text "foo"} (:slack-post-message-fn! utils/main-system)))
