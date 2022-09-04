(ns ufabc-registration-monitor.utils)

(defn parse-int [s]
  (some->> s
           (re-find #"\d+")
           (Integer.)))

(defn log! [data]
  (println data)
  data)

(defn log-exception! [ex]
  (println {:exception/data (ex-data ex)
            :exception/message (ex-message ex)
            :exception/cause (ex-cause ex)}))
