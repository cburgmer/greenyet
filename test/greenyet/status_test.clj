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

(defn- a-url-config-with-color [url key]
  [{:service "the_service"
    :url url
    :color key}])

(defn- a-url-config-with-status-and-message [url color-key message-key]
  [{:service "the_service"
    :url url
    :color color-key
    :message message-key}])

(defn- a-url-config-with-package-version [url package-version]
  [{:service "the_service"
    :url url
    :color "color"
    :package-version package-version}])

(defn- a-url-config-with-components [url component-config]
  [{:service "the_service"
    :url url
    :color "color"
    :components component-config}])

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
             (:color (sut/with-status a-host (a-url-config "http://%host%/not_found")))))))

  (testing "should return green for 200"
    (with-fake-resource "http://the_host/found" (fn [_] {:status 200
                                                         :body ""})
      (is (= :green
             (:color (sut/with-status a-host (a-url-config "http://%host%/found")))))))

  (testing "should return red for color red"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (is (= :red
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" "color")))))))

  (testing "should return yellow for color yellow"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "yellow"})
      (is (= :yellow
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" "color")))))))

  (testing "should return green for color green"
    (with-fake-resource "http://the_host/status.json" (json-response {:status "green"})
      (is (= :green
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" "status")))))))

  (testing "should fail if status key is configured but no JSON is provided"
    (with-fake-resource "http://the_host/status.json" (fn [_] {:status 200
                                                               :body "some body"})
      (is (= :red
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" "status")))))))

  (testing "should only evaluate JSON if configured"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (is (= :green
             (:color (sut/with-status a-host (a-url-config "http://%host%/status.json")))))))

  (testing "should return status from json-path config"
    (with-fake-resource "http://the_host/status.json" (json-response {:complex ["some garbage" {:color "yellow"}]})
      (is (= :yellow
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" {:json-path "$.complex[1].color"})))))))

  (testing "should return status with specific color for green status"
    (with-fake-resource "http://the_host/status.json" (json-response {:happy true})
      (is (= :green
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" {:json-path "$.happy"
                                                                                                   :green-value true})))))))

  (testing "should return status with specific color for yellow status"
    (with-fake-resource "http://the_host/status.json" (json-response {:status "pending"})
      (is (= :yellow
             (:color (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" {:json-path "$.status"
                                                                                                   :yellow-value "pending"})))))))

  (testing "messages"
    (testing "simple 200 check"
      (with-fake-resource "http://the_host/found" (fn [_] {:status 200
                                                           :body ""})
        (is (= "OK"
               (:message (sut/with-status a-host (a-url-config "http://%host%/found")))))))

    (testing "for 500"
      (with-fake-resource "http://the_host/error" (fn [_] {:status 500
                                                           :body "Internal Error"})
        (is (= "Status 500: Internal Error"
               (:message (sut/with-status a-host (a-url-config "http://%host%/error")))))))

    (testing "for 302"
      (with-fake-resource "http://the_host/redirect" (fn [_] {:status 302
                                                              :body "Found"})
        (is (= "Status 302: Found"
               (:message (sut/with-status a-host (a-url-config "http://%host%/redirect")))))))

    (testing "for internal exception"
      (with-fake-resource "http://the_host/status.json" (fn [_] {:status 200
                                                                 :body "some body"})
        (is (some? (:message (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" "status")))))))

    (testing "should return message if configured"
      (with-fake-resource "http://the_host/status.json" (json-response {:status "green"
                                                                        :message "up and running"})
        (is (= "up and running"
               (:message (sut/with-status a-host (a-url-config-with-status-and-message "http://%host%/status.json"
                                                                                       "status"
                                                                                       "message"))))))))

  (testing "package-version"
    (testing "should extract value"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :version "the-package-1.0.0"
                                                                        :body ""})
        (is (= "the-package-1.0.0"
               (:package-version (sut/with-status a-host (a-url-config-with-package-version "http://%host%/status.json"
                                                                                            "version")))))))

    (testing "should handle missing value"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :body ""})
        (is (nil? (:package-version (sut/with-status a-host (a-url-config-with-package-version "http://%host%/status.json"
                                                                                               "package-version"))))))))

  (testing "components"
    (testing "return all statuses"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "yellow"
                                                                                 :components [{:status "green"}
                                                                                              {:status "red"}
                                                                                              {:status "yellow"}]})
        (is (= [:green :red :yellow]
               (->> (sut/with-status a-host (a-url-config-with-components "http://the_host/with_components.json"
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
               (->> (a-url-config-with-components "http://the_host/with_components.json"
                                                  {:json-path "$.components[*]"
                                                   :color {:json-path "$.status"
                                                           :green-value "healthy"
                                                           :yellow-value "warning"}})
                    (sut/with-status a-host)
                    :components
                    (map :color))))))
    (testing "handles name"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "green"
                                                                                 :components [{:status "green"
                                                                                               :description "child 1"}]})
        (is (= [{:color :green :name "child 1" :message nil}]
               (:components (sut/with-status a-host (a-url-config-with-components "http://the_host/with_components.json"
                                                                                  {:json-path "$.components[*]"
                                                                                   :color "status"
                                                                                   :name "description"})))))))
    (testing "handles message"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "green"
                                                                                 :components [{:status "green"
                                                                                               :description "child 1"
                                                                                               :textualStatus "swell"}]})
        (is (= [{:color :green :name "child 1" :message "swell"}]
               (:components (sut/with-status a-host (a-url-config-with-components "http://the_host/with_components.json"
                                                                                  {:json-path "$.components[*]"
                                                                                   :color "status"
                                                                                   :name "description"
                                                                                   :message "textualStatus"}))))))))

  (testing "should return status url"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (is (= "http://the_host/status.json"
             (:status-url (sut/with-status a-host (a-url-config-with-color "http://%host%/status.json" "color"))))))))
