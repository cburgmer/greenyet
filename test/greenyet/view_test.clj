(ns greenyet.view-test
  (:require [greenyet.view :as sut]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(deftest test-render
  (testing "does not interpret html as regex"
    (is (str/includes? (sut/render [{:system "my system"
                                     :host "host"
                                     :environment "production"
                                     :status :green
                                     :message "$"}]
                                   nil
                                   "<!-- BODY -->"
                                   [])
                       "my system"))))
