(ns greenyet.status
  (:require [clj-http.client :as client]
            [clojure.string :as str]))

(defn- fetch-status [url]
  (try
    (let [status (:status (client/get url))]
      (if (= 200 status)
        :green
        :red))
    (catch Exception _
      :red)))

(defn- status-url [host status-url-config]
  (let [host-config (first (filter #(= (:system %) (:system host)) status-url-config))
        url-template (:url host-config)]
    (str/replace url-template #"%host%" (:hostname host))))

(defn with-status [host status-url-config]
  (let [status (fetch-status (status-url host status-url-config))]
    (assoc host
           :color status)))
