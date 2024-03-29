(ns ufabc-registration-monitor.core-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.test :refer [match?]]
            [ufabc-registration-monitor.core :as core]))

(deftest id->course-test
  (is (match? (m/equals {:id 1 :name "Quantum Physics" :slots 86})
              (core/id->course 1 [{:id 1 :name "Quantum Physics" :slots 86}
                                  {:id 2 :name "Algorithms" :slots 90}]))
      "Returns course with that id")

  (is (match? (m/equals {:id 1 :name "Quantum Physics" :slots 86})
              (core/id->course 1 [{:id 1 :name "Quantum Physics" :slots 86}
                                  {:id 1 :name "Algorithms" :slots 90}]))
      "When there are repeated ids, returns the first one")

  (is (nil? (core/id->course 10 [{:id 1 :name "Quantum Physics" :slots 86}]))
      "When not found, returns nil"))

(deftest get-updates-test
  (is (match? (m/equals {:b 3 :c 3})
              (core/get-updates {:a 1 :b 2} {:a 1 :b 3 :c 3}))
      "Returns a map with new or updated keyvals")

  (is (nil? (core/get-updates {:a 1} {:a 1}))
      "Returns nil when there are no changes")

  (is (nil? (core/get-updates {:a 1} {}))
      "Removed keyvals are not considered updates"))

(deftest alert-for-open-slots-test
  (is (nil? (core/maybe-alert 1 10 [{:id 1 :slots 10}] #{}))
      "When registration count is equal than slots, there are NO open slots, so return nil")

  (is (nil? (core/maybe-alert 1 10 [{:id 1 :slots 9}] #{}))
      "When registration count is smaller than slots, there are NO open slots, so return nil")

  (is (match? (m/equals {:channel "#random", :text "Quantum Physics has 1 slots!"})
              (core/maybe-alert 1 10 [{:id 1 :name "Quantum Physics" :slots 11}] #{}))
      "When registration count is equal slots, there are open slots, so return an alert to random")

  (is (match? (m/equals {:channel "#random", :text "Quantum Physics has 1 slots!"})
              (core/maybe-alert 1 10 [{:id 1 :name "Quantum Physics" :slots 11}] #{}))
      "When registration count is smaller than slots, there are open slots, so return an alert to random")

  (is (match? (m/equals {:channel "#general", :text "Algorithms has 1 slots!"})
              (core/maybe-alert 2 10 [{:id 2 :name "Algorithms" :slots 11}] #{2}))
      "When there are open slots and the course is monitored, so return an alert to random and another to general"))
