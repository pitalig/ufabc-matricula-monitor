(ns ufabc-registration-monitor.utils-test
  (:require [clojure.test :refer :all]
            [ufabc-registration-monitor.utils :as utils]
            [clojure.spec.test.alpha :as stest]))


(stest/instrument)

(deftest map-keys
  (is (= {123 "456"}
         (utils/map-keys {"123" "456"} utils/parse-int)))
  (is (= {"123" 456}
         (utils/map-keys {123 456} str))))

(deftest map-vals
  (is (= {"123" 456}
         (utils/map-vals {"123" "456"} utils/parse-int)))
  (is (= {123 "456"}
         (utils/map-vals {123 456} str))))

(deftest map-kv
  (is (= {123 456}
         (utils/map-kv {"123" "456"} utils/parse-int)))
  (is (= {"123" "456"}
         (utils/map-kv {123 456} str))))

(deftest parse-int
  (is (= 123 (utils/parse-int "123")))
  (is (= 123 (utils/parse-int "aaa123")))
  (is (= nil (utils/parse-int "aaa"))))

(stest/unstrument)
