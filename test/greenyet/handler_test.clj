(ns greenyet.handler-test
  (:require [greenyet.handler :as sut]
            [cheshire.core :as j]
            [clj-time.core :as tc]
            [ring.mock.request :refer :all]
            [clojure.test :refer :all]))

(defn- the-app
  ([] (the-app {}))
  ([statuses] (sut/create (atom [statuses (tc/date-time 2016 11 10)]))))

(defn- json-body [response]
  (j/parse-string (:body response) true))

(deftest create-test
  (testing "/status.json"
    (testing "returns the host name"
      (let [app (the-app)]
        (is (contains? (-> (app (request :get "/status.json"))
                           json-body)
                       :host))))
    (testing "returns the version of greenyet"
      (let [app (the-app)]
        (is (contains? (-> (app (request :get "/status.json"))
                           json-body)
                       :greenyet-version))))
    (testing "returns time of last update"
      (let [app (the-app)]
        (is (= "2016-11-10T00:00:00.000Z"
               (-> (app (request :get "/status.json"))
                   json-body
                   :last-changed)))))
    (testing "contains number of environments"
      (let [app (the-app {{:environment "first"} {}
                          {:environment "second"} {}})]
        (is (= 2
               (-> (app (request :get "/status.json"))
                   json-body
                   :statistics
                   :environments)))))
    (testing "contains number of systems"
      (let [app (the-app {{:system "first"} {}
                          {:system "second"} {}
                          {:system "third"} {}})]
        (is (= 3
               (-> (app (request :get "/status.json"))
                   json-body
                   :statistics
                   :systems)))))
    (testing "contains number of machines"
      (let [app (the-app {{:url 1} {}
                          {:url 2} {}
                          {:url 3} {}
                          {:url 4} {}})]
        (is (= 4
               (-> (app (request :get "/status.json"))
                   json-body
                   :statistics
                   :machines)))))
    (testing "contains status statistics"
      (let [app (the-app {{:url 1} {:color "red"}
                          {:url 2} {:color "yellow"}
                          {:url 3} {:color "yellow"}})]
        (is (= {:green 0
                :yellow 2
                :red 1}
               (-> (app (request :get "/status.json"))
                   json-body
                   :statistics
                   :statuses)))))))
