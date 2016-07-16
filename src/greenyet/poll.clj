(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clojure.core.async :refer [<! >! go go-loop timeout chan alts!!]]
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
    (go
      (>! channel (status/fetch-status host)))
    (let [[status _] (alts!! [channel (timeout timeout-in-ms)])]
      (or
        status
        {:color :red
         :message (format "Request timed out after %s milliseconds" timeout-in-ms)}))))

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
