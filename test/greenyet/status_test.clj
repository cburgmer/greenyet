(ns greenyet.status-test
  (:require [cheshire.core :as j]
            [clj-http.fake :as fake]
            [clojure.test :refer :all]
            [greenyet.status :as sut]))

(defn- host-without-color-config [url]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {}})

(defn- host-with-color-config [url key]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {:color key}})

(defn- host-with-status-message-config [url color-key message-key]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {:color color-key
            :message message-key}})

(defn- host-with-version-config [url package-version]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {:color "color"
            :package-version package-version}})

(defn- host-with-component-config [url component-config]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {:color "color"
            :components component-config}})

(defmacro with-fake-resource [url resp-func & body]
  `(fake/with-fake-routes-in-isolation {~url ~resp-func} ~@body))

(defn- json-response [body]
  (fn [_]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (j/generate-string body)}))

(deftest test-with-status
  (testing "should return red for 500"
    (with-fake-resource "http://the_host/not_found" (fn [_] {:status 500
                                                             :body ""})
      (is (= :red
             (:color (sut/fetch-status (host-without-color-config "http://the_host/not_found")))))))

  (testing "should return green for 200"
    (with-fake-resource "http://the_host/found" (fn [_] {:status 200
                                                         :body ""})
      (is (= :green
             (:color (sut/fetch-status (host-without-color-config "http://the_host/found")))))))

  (testing "should return red for color red"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (is (= :red
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" "color")))))))

  (testing "should return yellow for color yellow"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "yellow"})
      (is (= :yellow
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" "color")))))))

  (testing "should return green for color green"
    (with-fake-resource "http://the_host/status.json" (json-response {:status "green"})
      (is (= :green
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" "status")))))))

  (testing "should fail if status key is configured but no JSON is provided"
    (with-fake-resource "http://the_host/status.json" (fn [_] {:status 200
                                                               :body "some body"})
      (is (= :red
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" "status")))))))

  (testing "should only evaluate JSON if configured"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (is (= :green
             (:color (sut/fetch-status (host-without-color-config "http://the_host/status.json")))))))

  (testing "should return status from json-path config"
    (with-fake-resource "http://the_host/status.json" (json-response {:complex ["some garbage" {:color "yellow"}]})
      (is (= :yellow
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.complex[1].color"})))))))

  (testing "should return status with specific color for green status"
    (with-fake-resource "http://the_host/status.json" (json-response {:happy true})
      (is (= :green
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.happy"
                                                                                             :green-value true})))))))

  (testing "should return status with specific color for yellow status"
    (with-fake-resource "http://the_host/status.json" (json-response {:status "pending"})
      (is (= :yellow
             (:color (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.status"
                                                                                             :yellow-value "pending"})))))))

  (testing "messages"
    (testing "simple 200 check"
      (with-fake-resource "http://the_host/found" (fn [_] {:status 200
                                                           :body ""})
        (is (= "OK"
               (:message (sut/fetch-status (host-without-color-config "http://the_host/found")))))))

    (testing "for 500"
      (with-fake-resource "http://the_host/error" (fn [_] {:status 500
                                                           :body "Internal Error"})
        (is (= "Status 500: Internal Error"
               (:message (sut/fetch-status (host-without-color-config "http://the_host/error")))))))

    (testing "for 302"
      (with-fake-resource "http://the_host/redirect" (fn [_] {:status 302
                                                              :body "Found"})
        (is (= "Status 302: Found"
               (:message (sut/fetch-status (host-without-color-config "http://the_host/redirect")))))))

    (testing "for internal exception"
      (with-fake-resource "http://the_host/status.json" (fn [_] {:status 200
                                                                 :body "some body"})
        (is (some? (:message (sut/fetch-status (host-with-color-config "http://the_host/status.json" "status")))))))

    (testing "should return message if configured"
      (with-fake-resource "http://the_host/status.json" (json-response {:status "green"
                                                                        :message "up and running"})
        (is (= "up and running"
               (:message (sut/fetch-status (host-with-status-message-config "http://the_host/status.json"
                                                                           "status"
                                                                           "message"))))))))

  (testing "package-version"
    (testing "should extract value"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :version "the-package-1.0.0"
                                                                        :body ""})
        (is (= "the-package-1.0.0"
               (:package-version (sut/fetch-status (host-with-version-config "http://the_host/status.json"
                                                                            "version")))))))

    (testing "should handle missing value"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :body ""})
        (is (nil? (:package-version (sut/fetch-status (host-with-version-config "http://the_host/status.json"
                                                                               "package-version"))))))))

  (testing "components"
    (testing "return all statuses"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "yellow"
                                                                                 :components [{:status "green"}
                                                                                              {:status "red"}
                                                                                              {:status "yellow"}]})
        (is (= [:green :red :yellow]
               (->> (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                                 {:json-path "$.components[*]"
                                                                  :color "status"}))
                    :components
                    (map :color))))))
    (testing "handles complex color config"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "yellow"
                                                                                 :components [{:status "healthy"}
                                                                                              {:status "whatever"}
                                                                                              {:status "warning"}]})
        (is (= [:green :red :yellow]
               (->> (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                                 {:json-path "$.components[*]"
                                                                  :color {:json-path "$.status"
                                                                          :green-value "healthy"
                                                                          :yellow-value "warning"}}))
                    :components
                    (map :color))))))
    (testing "handles name"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "green"
                                                                                 :components [{:status "green"
                                                                                               :description "child 1"}]})
        (is (= [{:color :green :name "child 1" :message nil}]
               (:components (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                                         {:json-path "$.components[*]"
                                                                          :color "status"
                                                                          :name "description"})))))))
    (testing "handles message"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "green"
                                                                                 :components [{:status "green"
                                                                                               :description "child 1"
                                                                                               :textualStatus "swell"}]})
        (is (= [{:color :green :name "child 1" :message "swell"}]
               (:components (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                                         {:json-path "$.components[*]"
                                                                          :color "status"
                                                                          :name "description"
                                                                          :message "textualStatus"})))))))))
