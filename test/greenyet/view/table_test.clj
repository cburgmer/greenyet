(ns greenyet.view.table-test
  (:require [clojure
             [string :as str]
             [test :refer :all]]
            [greenyet.view.table :as sut]))

(deftest test-render
  (testing "does not interpret html as regex"
    (is (str/includes? (sut/render {{:system "my system"
                                     :host "host"
                                     :environment "production"}
                                     {:status :green
                                     :message "$"}}
                                   nil
                                   "<!-- BODY -->"
                                   [])
                       "my system"))))
