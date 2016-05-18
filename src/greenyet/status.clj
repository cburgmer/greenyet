(ns greenyet.status
  (:require [cheshire.core :as j]
            [clj-http.client :as client]
            [clojure.string :as str]))

(defn- application-status [json color-conf]
  (let [color (get json (keyword color-conf))]
    (cond
      (= "green" color) :green
      (= "yellow" color) :yellow
      :else :red)))

(defn- fetch-status [url {color-conf :color}]
  (try
    (let [response (client/get url)]
      (if (= 200 (:status response))
        (if color-conf
          (application-status (j/parse-string (:body response) true)
                              color-conf)
          :green)
        :red))
    (catch Exception _
      :red)))

(defn- status-url [host {url-template :url}]
  (str/replace url-template #"%host%" (:hostname host)))

(defn with-status [host status-url-config]
  (let [host-config (first (filter #(= (:system %) (:system host)) status-url-config))
        url (status-url host host-config)
        status (fetch-status url host-config)]
    (assoc host
           :color status)))
