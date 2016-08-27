(ns greenyet.selection-test
  (:require [greenyet.selection :as sut]
            [clojure.test :refer :all]))

(deftest test-filter-hosts
  (testing "hide green"
    (testing "includes green systems by default"
      (is (= 2
             (count (sut/filter-hosts {{:system "red system"} {:color :red}
                                       {:system "green system"} {:color :green}}
                                      nil
                                      nil
                                      false)))))

    (testing "removes green systems"
      (is (= 1
             (count (sut/filter-hosts {{:system "red system"} {:color :red}
                                       {:system "green system"} {:color :green}}
                                      nil
                                      nil
                                      true)))))

    (testing "does not remove partially green system"
      (is (= 2
             (count (sut/filter-hosts {{:system "my system" :hostname "host_1"} {:color :green}
                                       {:system "my system" :hostname "host_2"} {:color :yellow}}
                                      nil
                                      nil
                                      true)))))))
