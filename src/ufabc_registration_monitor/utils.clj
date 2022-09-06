(ns ufabc-registration-monitor.utils
  (:require [clj-http.client]
            [clj-slack.chat]))

(def main-system
  {:http-get-fn! clj-http.client/get
   :slack-post-message-fn! clj-slack.chat/post-message
   :log-fn! println
   :sleep-fn! (fn [milis] (Thread/sleep milis))
   :recur? (atom true)
   :monitored-ids #{670 ; Fenômenos de Transporte A-noturno (Santo André)
                    809 ; Materiais e Suas Propriedades A1-noturno (Santo André)
                    679 ; Fenômenos de Transporte B1-noturno (São Bernardo)
                    931 ; Habitação e Assentamentos Humanos A-noturno (Santo André)
                    425 ; Ecologia do Ambiente Urbano A-noturno (Santo André)
                    440 ; Compostagem A-noturno (Santo André)
                    }})

(defn parse-int [s]
  (some->> s
           (re-find #"\d+")
           (Integer.)))

(defn log! [data log-fn!]
  (log-fn! data)
  data)

(defn log-exception! [ex log-fn!]
  (log-fn! {:exception/data (ex-data ex)
            :exception/message (ex-message ex)
            :exception/cause (ex-cause ex)}))
