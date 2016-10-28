(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.tools.logging :as log]
            [greenyet.status :as status]))

(defn empty-statuses []
  [{} (tc/now)])

(defn- update-status [[host-with-statuses last-changed] key new-status]
  (let [old-status (get host-with-statuses key)]
    [(assoc host-with-statuses key new-status)
     (if (= new-status old-status)
       last-changed
       (tc/now))]))

(defn- poll-status [statuses host polling-interval-in-ms]
  (go-loop []
    (log/info (format "Fetching status from %s" (:status-url host)))
    (status/fetch-status host
                         polling-interval-in-ms
                         (fn [status]
                           (log/info (format "Received status %s from %s"
                                             (:color status)
                                             (:status-url host)))
                           (swap! statuses update-status host status)))
    (<! (timeout polling-interval-in-ms))
    (recur)))


(defn start-polling [statuses hosts-with-config polling-interval-in-ms]
  (doall (for [host hosts-with-config]
           (poll-status statuses host polling-interval-in-ms))))
