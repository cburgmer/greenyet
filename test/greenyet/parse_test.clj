(ns greenyet.parse-test
  (:require [greenyet.parse :as sut]
            [clojure.test :refer :all]))

(defn- first-non-nil-message [result]
  (some->> result
           second
           flatten
           (remove nil?)
           first))

(deftest test-components
  (testing "should accept simple config with key only"
    (is (= [{:color :green
             :name "the name"
             :message nil}]
           (first (sut/components {:jobs [{:name "the name"
                                           :color "green"}]}
                                  {:components "jobs"})))))

  (testing "should return warning if match is not a list"
    (is (re-find #"components.+'jobs'"
                 (-> (sut/components {:jobs "a_string"}
                                     {:components "jobs"})
                     first-non-nil-message))))

  (testing "should return components for complex config"
    (is (= [{:color :green
             :name "the name"
             :message nil}]
           (first (sut/components {:jobs [{:the_name "the name"
                                           :status "green"}]}
                                  {:components {:json-path "$.jobs"
                                                :name "the_name"
                                                :color "status"}})))))

  (testing "should return warning if match is not a list"
    (is (re-find #"components.+'\{:json-path \"\$\.string\"\}'"
                 (-> (sut/components {:string "a_string"}
                                     {:components {:json-path "$.string"}})
                     first-non-nil-message))))

  (testing "should assume default for name"
    (is (= [{:color :green
             :name "the name"
             :message nil}]
           (first (sut/components {:jobs [{:name "the name"
                                           :status "green"}]}
                                  {:components {:json-path "$.jobs"
                                                :color "status"}})))))

  (testing "should assume default for color"
    (is (= [{:color :green
             :name "the name"
             :message nil}]
           (first (sut/components {:jobs [{:the_name "the name"
                                           :color "green"}]}
                                  {:components {:json-path "$.jobs"
                                                :name "the_name"}})))))

  (testing "should return warning if message is configured but missing"
    (is (re-find #"component message.+'message'"
                 (-> (sut/components {:jobs [{:the_name "the name"
                                              :status "green"}]}
                                     {:components {:json-path "$.jobs"
                                                   :name "the_name"
                                                   :color "status"
                                                   :message "message"}})
                     first-non-nil-message))))

  (testing "should accept null values"
    (is (nil? (-> (sut/components {:jobs [{:the_name "the name"
                                           :status "green"
                                           :message nil}]}
                                  {:components {:json-path "$.jobs"
                                                :name "the_name"
                                                :color "status"
                                                :message "message"}})
                  first-non-nil-message))))

  (testing "should call out default color config on error"
    (is (re-find #"component color.+'color'"
                 (-> (sut/components {:jobs [{:name "the name"
                                              :status "green"}]}
                                     {:components "jobs"})
                     first-non-nil-message))))

  (testing "should call out default name config on error"
    (is (re-find #"component name.+'name'"
                 (-> (sut/components {:jobs [{:the_name "the name"
                                              :color "green"}]}
                                     {:components "jobs"})
                     first-non-nil-message))))

  (testing "should correctly extract complex color with green-value"
    (is (nil? (-> (sut/components {:jobs [{:name "the name"
                                           :status false}]}
                                  {:components {:json-path "$.jobs"
                                                :color {:json-path "$.status"
                                                        :green-value true}}})
                  first-non-nil-message)))))

(deftest test-message
  (testing "should return message"
    (is (= "the_message"
           (first (sut/message {:text "the_message"}
                               {:message "text"})))))

  (testing "should return warning if message is configured but missing"
    (is (re-find #"message.+'text'"
                 (-> (sut/message {}
                                  {:message "text"})
                     second))))

  (testing "should accept null values"
    (is (nil? (-> (sut/message {:text nil}
                               {:message "text"})
                  second)))))

(deftest test-package-version
  (testing "should return package-version"
    (is (= "component-1.2.3"
           (first (sut/package-version {:version "component-1.2.3"}
                                       {:package-version "version"})))))

  (testing "should return warning if package-version is configured but missing"
    (is (re-find #"package-version.+'version'"
                 (-> (sut/package-version {}
                                  {:package-version "version"})
                     second))))

  (testing "should return warning if package-version is null"
    (is (re-find #"package-version.+'version'"
                 (-> (sut/package-version {:version nil}
                                          {:package-version "version"})
                     second))))

  (testing "should be optional"
    (is (nil? (-> (sut/package-version {}
                                       {})
                  second)))))
