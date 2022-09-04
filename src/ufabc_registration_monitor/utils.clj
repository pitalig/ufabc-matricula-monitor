(ns ufabc-registration-monitor.utils)

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
