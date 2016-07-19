(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clojure.core.async :refer [<! >! alts!! chan go go-loop timeout]]
            [clojure.tools.logging :as log]
            [greenyet.status :as status]))

(def statuses (atom [{} (tc/now)]))


(defn- update-status [[host-with-statuses last-changed] key new-status]
  (let [old-status (get host-with-statuses key)]
    [(assoc host-with-statuses key new-status)
     (if (= new-status old-status)
       last-changed
       (tc/now))]))

(defn- fetch-status-with-timeout [host timeout-in-ms]
  (let [channel (chan)]
    (log/info (format "Fetching status from %s" (:status-url host)))
    (go
      (>! channel (status/fetch-status host)))
    (let [[status _] (alts!! [channel (timeout timeout-in-ms)])]
      (if status
        (do
          (log/info (format "Received status %s from %s"
                            (:color status)
                            (:status-url host)))
          status)
        (do
          (log/info (format "Request timed out for %s" (:status-url host)))
          {:color :red
           :message (format "Request timed out after %s milliseconds" timeout-in-ms)})))))

(defn- poll-status [host polling-interval-in-ms]
  (go-loop []
    (go
      (let [status (fetch-status-with-timeout host
                                              polling-interval-in-ms)]
        (swap! statuses update-status host status)))
    (<! (timeout polling-interval-in-ms))
    (recur)))


(defn start-polling [hosts-with-config polling-interval-in-ms]
  (doall (for [host hosts-with-config]
           (poll-status host polling-interval-in-ms))))
