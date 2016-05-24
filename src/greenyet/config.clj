(ns greenyet.config
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- status-url [host {url-template :url}]
  (str/replace url-template #"%hostname%" (:hostname host)))

(defn with-config [host status-url-config]
  (let [host-config (first (filter #(= (:system %) (:system host)) status-url-config))
        url (status-url host host-config)]
    (assoc host
           :status-url url
           :config host-config)))

(defn- read-host-list [config-dir]
  (let [host-list-file (io/file config-dir "hosts.yaml")]
    (yaml/parse-string (slurp host-list-file))))

(defn- read-status-url-config [config-dir]
  (let [config-file (io/file config-dir "status_url.yaml")]
    (yaml/parse-string (slurp config-file))))


(defn hosts-with-config [config-dir]
  (let [host-list (read-host-list config-dir)
        status-url-config (read-status-url-config config-dir)]
    (map #(with-config % status-url-config) host-list)))
