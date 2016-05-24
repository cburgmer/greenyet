(ns greenyet.poll
  (:require [clj-time.core :as tc]
            [clojure.core.async :refer [<! go-loop timeout]]
            [greenyet.status :as status]))

(def statuses (atom [{} (tc/now)]))


(defn- update-status [[host-with-statuses last-changed] key new-status]
  (let [old-status (get host-with-statuses key)]
    [(assoc host-with-statuses key new-status)
     (if (= new-status old-status)
       last-changed
       (tc/now))]))

(defn- poll-status [host timeout-in-ms]
  (go-loop []
    (let [status (status/fetch-status host)]
      (swap! statuses update-status host status)
      (<! (timeout timeout-in-ms))
      (recur))))


(defn start-polling [hosts-with-config timeout-in-ms]
  (doall (for [host hosts-with-config]
           (poll-status host timeout-in-ms))))
