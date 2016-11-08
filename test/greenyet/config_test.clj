(ns greenyet.config-test
  (:require [greenyet.config :as sut]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(defn- with-config [status-url hosts]
  (fn [config-dir file-name]
    (io/reader (char-array (if (= file-name "status_url.yaml")
                             status-url
                             hosts)))))

(defn- an-entry [entry]
  (let [lines (map (fn [[key val]]
                     (format "%s: %s" key val))
                   entry)]
    (str/join "\n" (cons (format "- %s" (first lines))
                         (map #(format "  %s" %) (rest lines))))))

(defn yaml-list [& args]
  (str/join "\n" (map an-entry args)))


(deftest hosts-with-config-test
  (testing "returns an emoty list for an empty config"
    (is (= []
           (first (sut/hosts-with-config (with-config "" ""))))))
  (testing "compiles an entry"
    (is (= [{:system "my_system"
             :status-url "http://my_host/some_url"
             :hostname "my_host"
             :environment "prod"
             :index 0
             :config {:system "my_system"
                      :url "http://%hostname%/some_url"}}]
           (first (sut/hosts-with-config (with-config
                                           (yaml-list {"system" "my_system"
                                                       "url" "http://%hostname%/some_url"})
                                           (yaml-list {"hostname" "my_host"
                                                       "system" "my_system"
                                                       "environment" "prod"})))))))
  (testing "finds no errors"
    (is (= []
           (last (sut/hosts-with-config (with-config
                                          (yaml-list {"system" "my_system"
                                                      "url" "http://%hostname%/some_url"})
                                          (yaml-list {"hostname" "my_host"
                                                      "system" "my_system"
                                                      "environment" "prod"})))))))
  (testing "throws error on missing parameters in hosts.yaml entry"
    (is (= ["Missing 'hostname' for entry {:system \"my_system\", :environment \"prod\"}"]
           (last (sut/hosts-with-config (with-config
                                          (yaml-list {"system" "my_system"
                                                      "url" "http://%hostname%/some_url"})
                                          (yaml-list {"system" "my_system"
                                                      "environment" "prod"}))))))
    (is (= ["Missing 'system' for entry {:hostname \"my_host\", :environment \"prod\"}"]
           (last (sut/hosts-with-config (with-config
                                          (yaml-list {"system" "my_system"
                                                      "url" "http://%hostname%/some_url"})
                                          (yaml-list {"hostname" "my_host"
                                                      "environment" "prod"})))))))
  (testing "throws error on missing parameters in status_url.yaml entry"
    (is (= ["Missing 'system' for entry {:url \"http://%hostname%/some_url\"}"]
           (last (sut/hosts-with-config (with-config
                                          (yaml-list {"url" "http://%hostname%/some_url"})
                                          (yaml-list {"hostname" "my_host"
                                                      "system" "my_system"
                                                      "environment" "prod"}))))))))
