(ns ufabc-registration-monitor.integration
  (:require [clojure.test :refer :all]
            [ufabc-registration-monitor.core :as core]))

(defn update-registrations-count [course-1-count course-2-count mock-http-responses]
  (swap! mock-http-responses
         assoc
         "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
         {:body (str "contagemMatriculas={\"1\":\"" course-1-count "\",
                                          \"2\":\"" course-2-count "\"};")})
  (Thread/sleep 100))

(deftest integration
  (let [active? (atom true)
        mock-http-responses (atom {"https://matricula.ufabc.edu.br/cache/todasDisciplinas.js"
                                   {:body "todasDisciplinas=[{\"nome\":\"Quantum Physics\",
                                                              \"vagas\":100,
                                                              \"id\":1},
                                                             {\"nome\":\"Algorithms\",
                                                              \"vagas\":50,
                                                              \"id\":2}];"}
                                   "https://matricula.ufabc.edu.br/cache/contagemMatriculas.js"
                                   {:body "contagemMatriculas={\"1\":\"100\",
                                                               \"2\":\"50\"};"}})
        slack-messages (atom [])
        worker (future (core/start-worker! {:http-get-fn! (fn [url _params] (get @mock-http-responses url))
                                            :slack-post-message-fn! (fn [message] (swap! slack-messages conj message))
                                            :log-fn! #(when false (println (str "Log: " %)))
                                            :sleep-fn! #(when false (println (str "Sleep: " %)))
                                            :active? active?
                                            :monitored-ids #{2}}))]

    (Thread/sleep 100)
    (is (= [{:channel "#random", :text "Starting!"}]
           @slack-messages)
        "Sends a message in #random to notify that it's starting")

    (testing "When one new slot is available, sends correct message in random"
      (update-registrations-count 99 50 mock-http-responses)
      (is (= [{:channel "#random", :text "Starting!"}
              {:channel "#random", :text "Quantum Physics has 1 slots!"}]
             @slack-messages)))

    (testing "When 10 new slots are available, sends correct message in random"
      (update-registrations-count 90 50 mock-http-responses)
      (is (= [{:channel "#random", :text "Starting!"}
              {:channel "#random", :text "Quantum Physics has 1 slots!"}
              {:channel "#random", :text "Quantum Physics has 10 slots!"}]
             @slack-messages)))

    (testing "When one new slot is available in a monitored course, sends correct message in general"
      (update-registrations-count 90 49 mock-http-responses)
      (is (= [{:channel "#random", :text "Starting!"}
              {:channel "#random", :text "Quantum Physics has 1 slots!"}
              {:channel "#random", :text "Quantum Physics has 10 slots!"}
              {:channel "#general", :text "Algorithms has 1 slots!"}]
             @slack-messages)))

    (reset! active? false)))
