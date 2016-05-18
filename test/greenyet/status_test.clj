(ns greenyet.status-test
  (:require [cheshire.core :as j]
            [clj-http.fake :as fake]
            [clojure.test :refer :all]
            [greenyet.status :as sut]))

(def a-host {:hostname "the_host"
             :service "the_service"})

(defn- a-url-config [url]
  [{:service "the_service"
    :url url}])

(deftest test-with-status
  (testing "should return red for 500"
    (fake/with-fake-routes-in-isolation {"http://the_host/not_found" (fn [_] {:status 500
                                                                              :body ""})}
      (is (= :red
             (:color (sut/with-status a-host (a-url-config "http://%host%/not_found")))))))

  (testing "should return green for 200"
    (fake/with-fake-routes-in-isolation {"http://the_host/found" (fn [_] {:status 200
                                                                          :body ""})}
      (is (= :green
             (:color (sut/with-status a-host (a-url-config "http://%host%/found")))))))

  (testing "should return red for color red"
    (fake/with-fake-routes-in-isolation {"http://the_host/status.json" (fn [_] {:status 200
                                                                                :headers {"Content-Type" "application/json"}
                                                                                :body (j/generate-string {:color "red"})})}
      (is (= :red
             (:color (sut/with-status a-host (a-url-config "http://%host%/status.json"))))))))
