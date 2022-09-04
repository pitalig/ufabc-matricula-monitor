(ns ufabc-registration-monitor.utils)

(defn map-keys [m f]
  (reduce-kv #(assoc %1 (f %2) %3) {} m))

(defn map-vals [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn map-kv [m f]
  (reduce-kv #(assoc %1 (f %2) (f %3)) {} m))

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
