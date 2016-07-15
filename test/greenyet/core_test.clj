(ns greenyet.core-test
  (:require [greenyet.core :as sut]
            [clojure.test :refer :all]
            [ring.mock.request :refer :all]))

(deftest handler-test
  (testing "forces caches to revalidate"
    (is (= "max-age=0,must-revalidate"
           (-> (sut/handler (request :get ""))
               :headers
               (get "Cache-Control"))))))
