(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.tools.logging :as log]
            [greenyet.status :as status]))

(defn empty-statuses []
  [{} (tc/now)])

(defn- update-status [[host-with-statuses _] key new-status]
  [(assoc host-with-statuses key new-status) (tc/now)])

(defn- do-update! [statuses host new-status]
  (let [[host-with-statuses _] @statuses
        old-status (get host-with-statuses host)]
    (when-not (= new-status old-status)
      (swap! statuses update-status host new-status))))

(defn fetch-and-update! [statuses host timeout]
  (log/info (format "Fetching status from %s" (:status-url host)))
  (status/fetch-status host
                       timeout
                       (fn [status]
                         (log/info (format "Received status %s from %s"
                                           (:color status)
                                           (:status-url host)))
                         (do-update! statuses host status))))

(defn- poll-status [statuses host polling-interval-in-ms]
  (go-loop []
    (fetch-and-update! statuses host polling-interval-in-ms)
    (<! (timeout polling-interval-in-ms))
    (recur)))


(defn start-polling [statuses hosts-with-config polling-interval-in-ms]
  (doall (for [host hosts-with-config]
           (poll-status statuses host polling-interval-in-ms))))
