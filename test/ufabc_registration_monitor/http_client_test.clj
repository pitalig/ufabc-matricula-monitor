(ns ufabc-registration-monitor.http-client-test
  (:require [clj-http.client]
            [clojure.test :refer :all]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.test :refer [match?]]
            [ufabc-registration-monitor.http-client :as http]))

(deftest parse-response
  (testing "Every bookmark can be parsed using the corresponding samples and coercer-fn"
    (doseq [[name settings] http/bookmark-settings]
      (is (match? (m/equals (:coerced-sample settings))
                  (http/parse-response (:sample settings) (:coerce-fn settings)))
          (str "Can parse " name)))))
