(ns greenyet.status-test
  (:require [cheshire.core :as j]
            [clojure.test :refer :all]
            [greenyet.status :as sut]
            [org.httpkit.fake :as fake]))

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

(defn- host-with-component-config-no-overall-status [url component-config]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {:components component-config}})

(defn- host-with-component-config [url component-config]
  {:hostname "the_host"
   :service "the_service"
   :status-url url
   :config {:color "color"
            :components component-config}})

(defmacro with-fake-resource [url resp & body]
  `(fake/with-fake-http [{:url ~url} ~resp]
     ~@body))

(defn- json-response [body]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (j/generate-string body)})

(def timeout 42)

(deftest test-with-status
  (testing "should return red for 500"
    (with-fake-resource "http://the_host/not_found" {:status 500
                                                     :body ""}
      (with-local-vars [status nil]
        (sut/fetch-status (host-without-color-config "http://the_host/not_found")
                          timeout
                          (fn [status]
                            (is (= :red
                                   (:color status))))))))

  (testing "should return green for 200"
    (with-fake-resource "http://the_host/found" {:status 200
                                                 :body ""}
      (sut/fetch-status (host-without-color-config "http://the_host/found")
                        timeout
                        (fn [status]
                          (is (= :green
                                 (:color status)))))))

  (testing "should return red for color red"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" "color")
                        timeout
                        (fn [status]
                          (is (= :red
                                 (:color status)))))))

  (testing "should return yellow for color yellow"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "yellow"})
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" "color")
                        timeout
                        (fn [status]
                          (is (= :yellow
                                 (:color status)))))))

  (testing "should return green for color green"
    (with-fake-resource "http://the_host/status.json" (json-response {:status "green"})
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" "status")
                        timeout
                        (fn [status]
                          (is (= :green
                                 (:color status)))))))

  (testing "should fail if status key is configured but no JSON is provided"
    (with-fake-resource "http://the_host/status.json" {:status 200
                                                       :body "some body"}
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" "status")
                        timeout
                        (fn [status]
                          (is (= :red
                                 (:color status)))))))

  (testing "should only evaluate JSON if configured"
    (with-fake-resource "http://the_host/status.json" (json-response {:color "red"})
      (sut/fetch-status (host-without-color-config "http://the_host/status.json")
                        timeout
                        (fn [status]
                          (is (= :green
                                 (:color status)))))))

  (testing "should return status from json-path config"
    (with-fake-resource "http://the_host/status.json" (json-response {:complex ["some garbage" {:color "yellow"}]})
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.complex[1].color"})
                        timeout
                        (fn [status]
                          (is (= :yellow
                                 (:color status)))))))

  (testing "should return status with specific color for green status"
    (with-fake-resource "http://the_host/status.json" (json-response {:happy true})
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.happy"
                                                                               :green-value true})
                        timeout
                        (fn [status]
                          (is (= :green
                                 (:color status)))))))

  (testing "should return status with specific color for yellow status"
    (with-fake-resource "http://the_host/status.json" (json-response {:status "pending"})
      (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.status"
                                                                               :yellow-value "pending"})
                        timeout
                        (fn [status]
                          (is (= :yellow
                                 (:color status)))))))

  (testing "messages"
    (testing "simple 200 check"
      (with-fake-resource "http://the_host/found" {:status 200
                                                   :body ""}
        (sut/fetch-status (host-without-color-config "http://the_host/found")
                          timeout
                          (fn [status]
                            (is (= "OK"
                                   (:message status)))))))

    (testing "for 500"
      (with-fake-resource "http://the_host/error"  {:status 500
                                                    :body "Internal Error"}
        (sut/fetch-status (host-without-color-config "http://the_host/error")
                          timeout
                          (fn [status]
                            (is (= "Status 500: Internal Error"
                                   (:message status)))))))

    (testing "for 302"
      (with-fake-resource "http://the_host/redirect"  {:status 302
                                                       :body "Found"}
        (sut/fetch-status (host-without-color-config "http://the_host/redirect")
                          timeout
                          (fn [status]
                            (is (= "Status 302: Found"
                                   (:message status)))))))

    (testing "for internal exception"
      (with-fake-resource "http://the_host/status.json"  {:status 200
                                                          :body "some body"}
        (sut/fetch-status (host-with-color-config "http://the_host/status.json" "status")
                          timeout
                          (fn [status]
                            (is (some? (:message status)))))))

    (testing "should return message if configured"
      (with-fake-resource "http://the_host/status.json" (json-response {:status "green"
                                                                        :message "up and running"})
        (sut/fetch-status (host-with-status-message-config "http://the_host/status.json"
                                                           "status"
                                                           "message")
                          timeout
                          (fn [status]
                            (is (= "up and running"
                                   (first (:message status))))))))

    (testing "should handle message list"
      (with-fake-resource "http://the_host/status.json" (json-response {:status "green"
                                                                        :message ["up and running"]})
        (sut/fetch-status (host-with-status-message-config "http://the_host/status.json"
                                                           "status"
                                                           "message")
                          timeout
                          (fn [status]
                            (is (= "up and running"
                                   (first (:message status))))))))

    (testing "should indicate JSON parse error"
      (with-fake-resource "http://the_host/status.json" {:status 200
                                                         :headers {"Content-Type" "application/json"}
                                                         :body "not_json"}
        (sut/fetch-status (host-with-color-config "http://the_host/status.json" "color")
                          timeout
                          (fn [status]
                            (is (re-find #"token.+not_json"
                                         (:message status)))))))

    (testing "should indicate failure to read color"
      (with-fake-resource "http://the_host/status.json" (json-response {})
        (sut/fetch-status (host-with-color-config "http://the_host/status.json" {:json-path "$.color"})
                          timeout
                          (fn [status]
                            (is (re-find #"read color.+\$\.color"
                                         (first (:message status))))))))

    (testing "should indicate failure to read components"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"})
        (sut/fetch-status (host-with-component-config "http://the_host/status.json" {:json-path "$.components"})
                          timeout
                          (fn [status]
                            (is (re-find #"read components.+\$\.components"
                                         (first (:message status))))))))

    (testing "should indicate failure to read color for components"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :components [{}]})
        (sut/fetch-status (host-with-component-config "http://the_host/status.json" {:json-path "$.components"
                                                                                     :color "status"})
                          timeout
                          (fn [status]
                            (is (re-find #"read component color.+status"
                                         (first (:message status))))))))

    (testing "should indicate failure to read package-version"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"})
        (sut/fetch-status (host-with-version-config "http://the_host/status.json" "package")
                          timeout
                          (fn [status]
                            (is (re-find #"read package-version.+package"
                                         (first (:message status)))))))))

  (testing "package-version"
    (testing "should extract value"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :version "the-package-1.0.0"})
        (sut/fetch-status (host-with-version-config "http://the_host/status.json"
                                                    "version")
                          timeout
                          (fn [status]
                            (is (= "the-package-1.0.0"
                                   (:package-version status)))))))

    (testing "should handle missing value"
      (with-fake-resource "http://the_host/status.json" (json-response {:color "green"
                                                                        :body ""})
        (sut/fetch-status (host-with-version-config "http://the_host/status.json"
                                                    "package-version")
                          timeout
                          (fn [status]
                            (is (nil? (:package-version status))))))))

  (testing "components"
    (testing "no overall status"
      (testing "return red if component red"
        (with-fake-resource "http://the_host/with_components.json" (json-response {:components [{:status "green"}
                                                                                                {:status "red"}
                                                                                                {:status "yellow"}]})
          (sut/fetch-status (host-with-component-config-no-overall-status "http://the_host/with_components.json"
                                                                          {:json-path "$.components[*]"
                                                                           :color "status"})
                            timeout
                            (fn [status]
                              (is (= :red
                                     (:color status)))))))
      (testing "return yellow if component yellow"
        (with-fake-resource "http://the_host/with_components.json" (json-response {:components [{:status "green"}
                                                                                                {:status "yellow"}]})
          (sut/fetch-status (host-with-component-config-no-overall-status "http://the_host/with_components.json"
                                                                          {:json-path "$.components[*]"
                                                                           :color "status"})
                            timeout
                            (fn [status]
                              (is (= :yellow
                                     (:color status)))))))
      (testing "return green if all components green"
        (with-fake-resource "http://the_host/with_components.json" (json-response {:components [{:status "green"}
                                                                                                {:status "green"}]})
          (sut/fetch-status (host-with-component-config-no-overall-status "http://the_host/with_components.json"
                                                                          {:json-path "$.components[*]"
                                                                           :color "status"})
                            timeout
                            (fn [status]
                              (is (= :green
                                     (:color status)))))))
      (testing "return red if no components given"
        (with-fake-resource "http://the_host/with_components.json" (json-response {:components []})
          (sut/fetch-status (host-with-component-config-no-overall-status "http://the_host/with_components.json"
                                                                          {:json-path "$.components[*]"
                                                                           :color "status"})
                            timeout
                            (fn [status]
                              (is (= :red
                                     (:color status)))))))
      (testing "return red if unknown component color"
        (with-fake-resource "http://the_host/with_components.json" (json-response {:components [{:status "unknown"}]})
          (sut/fetch-status (host-with-component-config-no-overall-status "http://the_host/with_components.json"
                                                                          {:json-path "$.components[*]"
                                                                           :color "status"})
                            timeout
                            (fn [status]
                              (is (= :red
                                     (:color status))))))))

    (testing "return all statuses"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "yellow"
                                                                                 :components [{:status "green"}
                                                                                              {:status "red"}
                                                                                              {:status "yellow"}]})
        (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                      {:json-path "$.components[*]"
                                                       :color "status"})
                          timeout
                          (fn [status]
                            (is (= [:green :red :yellow]
                                   (->> status
                                        :components
                                        (map :color))))))))
    (testing "handles complex color config"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "yellow"
                                                                                 :components [{:status "healthy"}
                                                                                              {:status "whatever"}
                                                                                              {:status "warning"}]})
        (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                      {:json-path "$.components[*]"
                                                       :color {:json-path "$.status"
                                                               :green-value "healthy"
                                                               :yellow-value "warning"}})
                          timeout
                          (fn [status]
                            (is (= [:green :red :yellow]
                                   (->> status
                                        :components
                                        (map :color))))))))
    (testing "handles name"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "green"
                                                                                 :components [{:status "green"
                                                                                               :description "child 1"}]})
        (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                      {:json-path "$.components[*]"
                                                       :color "status"
                                                       :name "description"})
                          timeout
                          (fn [status]
                            (is (= [{:color :green :name "child 1" :message nil}]
                                   (:components status)))))))
    (testing "handles message"
      (with-fake-resource "http://the_host/with_components.json" (json-response {:color "green"
                                                                                 :components [{:status "green"
                                                                                               :description "child 1"
                                                                                               :textualStatus "swell"}]})
        (sut/fetch-status (host-with-component-config "http://the_host/with_components.json"
                                                      {:json-path "$.components[*]"
                                                       :color "status"
                                                       :name "description"
                                                       :message "textualStatus"})
                          timeout
                          (fn [status]
                            (is (= [{:color :green :name "child 1" :message "swell"}]
                                   (:components status)))))))))
