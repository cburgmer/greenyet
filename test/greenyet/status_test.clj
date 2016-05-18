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

(defn- a-url-config-with-status [url key]
  [{:service "the_service"
    :url url
    :color key}])

(defn- json-response [body]
  (fn [_]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (j/generate-string body)}))

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
    (fake/with-fake-routes-in-isolation {"http://the_host/status.json" (json-response {:color "red"})}
      (is (= :red
             (:color (sut/with-status a-host (a-url-config-with-status "http://%host%/status.json" "color")))))))

  (testing "should return yellow for color yellow"
    (fake/with-fake-routes-in-isolation {"http://the_host/status.json" (json-response {:color "yellow"})}
      (is (= :yellow
             (:color (sut/with-status a-host (a-url-config-with-status "http://%host%/status.json" "color")))))))

  (testing "should return green for color green"
    (fake/with-fake-routes-in-isolation {"http://the_host/status.json" (json-response {:status "green"})}
      (is (= :green
             (:color (sut/with-status a-host (a-url-config-with-status "http://%host%/status.json" "status")))))))

  (testing "should fail if status key is configured but no JSON is provided"
    (fake/with-fake-routes-in-isolation {"http://the_host/status.json" (fn [_] {:status 200
                                                                                :body "some body"})}
      (is (= :red
             (:color (sut/with-status a-host (a-url-config-with-status "http://%host%/status.json" "status")))))))

  (testing "should only evaluate JSON if configured"
    (fake/with-fake-routes-in-isolation {"http://the_host/status.json" (json-response {:color "red"})}
      (is (= :green
             (:color (sut/with-status a-host (a-url-config "http://%host%/status.json"))))))))
