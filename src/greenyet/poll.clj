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

(defn fetch-and-update! [statuses host timeout]
  (log/info (format "Fetching status from %s" (:status-url host)))
  (status/fetch-status host
                       timeout
                       (fn [status]
                         (log/info (format "Received status %s from %s"
                                           (:color status)
                                           (:status-url host)))
                         (swap! statuses update-status host status))))

(defn- poll-status [statuses host polling-interval-in-ms]
  (go-loop []
    (fetch-and-update! statuses host polling-interval-in-ms)
    (<! (timeout polling-interval-in-ms))
    (recur)))


(defn start-polling [statuses hosts-with-config polling-interval-in-ms]
  (doall (for [host hosts-with-config]
           (poll-status statuses host polling-interval-in-ms))))
