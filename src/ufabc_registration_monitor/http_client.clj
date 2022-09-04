(ns ufabc-registration-monitor.http-client
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clj-http.client]
            [ufabc-registration-monitor.utils :as utils]
            [clojure.string :as string]))

(defn coerce-registrations [parsed-response]
  (-> parsed-response
      (utils/map-keys utils/parse-int)
      (utils/map-vals #(map utils/parse-int %))))

(defn coerce-registrations-count [parsed-response]
  (-> parsed-response
      (utils/map-kv utils/parse-int)))

(def bookmark-settings
  {:registrations {:used false
                   :url "https://matricula.ufabc.edu.br/cache/matriculas.js"
                   :doc "Map where the keys are student ids and the value is a list with ids of the courses that they are registered"
                   :sample-path "resources/registrations_sample.txt"
                   :coerce-fn coerce-registrations}
   :registrations-count {:used true
                         :url "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                         :doc "Map where the keys are course ids and the value is the number of registered students in that course"
                         :sample-path "resources/registrations_count_sample.txt"
                         :coerce-fn coerce-registrations-count}
   :courses {:used true
             :url "https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
             :doc "List of maps with information about each course"
             :sample-path "resources/courses_sample.txt"}})

(s/fdef http-get!
        :args (s/cat :url ::url
                     :kwargs (s/keys* :req-un [::max-retries ::base-interval-sec]))
        :ret (s/keys :opt [::body ::status]))
(defn http-get!
  "Send a http get to an url.
   If it fails, will retry `max-retries` times with an incremental sleep between retries."
  [url & {:keys [max-retries base-interval-sec]
          :or {max-retries 5, base-interval-sec 5}}]
  (loop [t 0]
    (let [result (try (clj-http.client/get url {:insecure? true})
                      (catch Exception e
                        (if (< max-retries t)
                          (throw e)
                          nil)))]
      (or
        result
        (do
          (Thread/sleep (* t base-interval-sec 1000))
          (recur (inc t)))))))

(defn bookmarks->url [endpoint bookmarks]
  (-> bookmarks endpoint :url))

(s/fdef parse-response
        :args (s/cat :raw-response (s/keys :req-un [::body])))
(defn parse-response [raw-response]
  (-> raw-response
      :body
      (string/replace-first #".*=" "")
      (json/parse-string)))
