(ns ufabc-matricula-monitor.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [ufabc-matricula-monitor.core :as core]))

(stest/instrument)

(deftest secure-get!
  (is (let [{:keys [body status]} (core/secure-get! "https://www.google.com")]
        (and (string? body)
             (= 200 status)))))

(stest/unstrument)
