(ns ufabc-registration-monitor.utils
  (:require [clj-http.client]
            [clj-slack.chat]))

(def main-system
  {:http-get-fn! clj-http.client/get
   :slack-post-message-fn! (fn [{:keys [channel text]}]
                             (clj-slack.chat/post-message {:api-url "https://slack.com/api"
                                                           :token (System/getenv "slack_token")}
                                                          channel
                                                          text
                                                          {:username "Registration Monitor"}))
   :log-fn! println
   :sleep-fn! (fn [milis] (Thread/sleep milis))
   :active? (atom true)
   :monitored-ids #{670 440}})

(comment
  ((:slack-post-message-fn! main-system) {:channel "#general" :text "Test"})
  ((:http-get-fn! main-system) "https://www.google.com")
  ((:log-fn! main-system) "Hello world")
  )

(defn parse-int
  [s]
  (some->> s
           (re-find #"\d+")
           (Integer.)))

(defn log!
  [data log-fn!]
  (log-fn! data)
  data)

(defn log-exception!
  [ex log-fn!]
  (log-fn! {:exception/data (ex-data ex)
            :exception/message (ex-message ex)
            :exception/cause (ex-cause ex)}))
