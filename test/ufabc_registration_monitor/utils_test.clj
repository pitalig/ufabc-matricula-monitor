(ns ufabc-registration-monitor.utils-test
  (:require [clojure.test :refer :all]
            [ufabc-registration-monitor.utils :as utils]
            [clojure.spec.test.alpha :as stest]))


(stest/instrument)

(deftest parse-int
  (is (= 123 (utils/parse-int "123")))
  (is (= 123 (utils/parse-int "aaa123")))
  (is (= nil (utils/parse-int "aaa"))))

(stest/unstrument)
