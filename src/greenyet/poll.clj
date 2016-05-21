(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clj-yaml.core :as yaml]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.java.io :as io]
            [greenyet.status :as status]))

(def statuses (atom [{} (tc/now)]))

(defn- update-status [[host-with-statuses last-changed] key new-status]
  (let [old-status (get host-with-statuses key)]
    [(assoc host-with-statuses key new-status)
     (if (= new-status old-status)
       last-changed
       (tc/now))]))

(defn- poll-status [timeout-in-ms host status-url-config]
  (go-loop []
    (let [status (status/with-status host status-url-config)]
      (swap! statuses update-status host status)
      (<! (timeout timeout-in-ms))
      (recur))))


(defn- read-host-list [config-dir]
  (let [host-list-file (io/file config-dir "hosts.yaml")]
    (yaml/parse-string (slurp host-list-file))))

(defn- read-status-url-config [config-dir]
  (let [config-file (io/file config-dir "status_url.yaml")]
    (yaml/parse-string (slurp config-file))))

(defn start-polling [config-dir timeout-in-ms]
  (let [host-list (read-host-list config-dir)
        status-url-config (read-status-url-config config-dir)]
    (doall (for [host host-list]
             (poll-status timeout-in-ms host status-url-config)))))
