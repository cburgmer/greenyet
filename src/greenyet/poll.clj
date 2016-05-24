(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clj-yaml.core :as yaml]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [greenyet.status :as status]))

(def statuses (atom [{} (tc/now)]))

(defn- update-status [[host-with-statuses last-changed] key new-status]
  (let [old-status (get host-with-statuses key)]
    [(assoc host-with-statuses key new-status)
     (if (= new-status old-status)
       last-changed
       (tc/now))]))

(defn- poll-status [timeout-in-ms host]
  (go-loop []
    (let [status (status/with-status host)]
      (swap! statuses update-status host status)
      (<! (timeout timeout-in-ms))
      (recur))))

(defn- status-url [host {url-template :url}]
  (str/replace url-template #"%host%" (:hostname host)))

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

(defn- hosts-with-config [config-dir]
  (let [host-list (read-host-list config-dir)
        status-url-config (read-status-url-config config-dir)]
    (map #(with-config % status-url-config) host-list)))


(defn start-polling [config-dir timeout-in-ms]
  (doall (for [host (hosts-with-config config-dir)]
           (poll-status timeout-in-ms host))))
