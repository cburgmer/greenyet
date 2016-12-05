(ns greenyet.poll-test
  (:require [clj-time.core :as tc]
            [clojure.test :refer :all]
            [greenyet
             [poll :as sut]
             [status :as status]]))

(deftest fetch-and-update!-test
  (testing "stores status"
    (let [statuses (atom (sut/empty-statuses))
          host {:hostname "my_host"}]
      (with-redefs [status/fetch-status (fn [host timeout callback]
                                          (callback {:color "green"}))]
        (sut/fetch-and-update! statuses host 42)
        (is (= {:color "green"}
               (-> @statuses
                   first
                   (get host)))))))

  (testing "updates change timestamp"
    (let [statuses (atom (sut/empty-statuses))
          host {:hostname "my_host"}
          time-now (tc/date-time 1986 10 14 4 3 27 456)]
      (with-redefs [status/fetch-status (fn [host timeout callback]
                                          (callback {:color "green"}))
                    tc/now (fn [] time-now)]
        (sut/fetch-and-update! statuses host 42)
        (is (= time-now
               (last @statuses))))))

  (testing "does not update change timestamp when status unchanged"
    (let [host {:hostname "my_host"}
          time-now (tc/date-time 1986 10 14 4 3 27 456)
          statuses (atom [{host {:color "yellow"}} time-now])]
      (with-redefs [status/fetch-status (fn [host timeout callback]
                                          (callback {:color "yellow"}))]
        (sut/fetch-and-update! statuses host 42)
        (is (= time-now
               (last @statuses)))))))
