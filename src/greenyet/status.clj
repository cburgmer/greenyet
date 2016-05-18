(ns greenyet.status
  (:require [cheshire.core :as j]
            [clj-http.client :as client]
            [json-path]
            [clojure.string :as str]))

(defn- extract-color [json color-conf]
  (if (string? color-conf)
    (get json (keyword color-conf))
    (let [path (:json-path color-conf)]
      (json-path/at-path path json))))

(defn- color-value [color-conf key default]
  (if (string? color-conf)
    default
    (or (get color-conf key)
        default)))

(defn- status-color [json color-conf]
  (let [color (extract-color json color-conf)
        green-value (color-value color-conf :green-value "green")
        yellow-value (color-value color-conf :yellow-value "yellow")]
    (cond
      (= green-value color) :green
      (= yellow-value color) :yellow
      :else :red)))

(defn- status-message [json message-conf]
  (get json (keyword message-conf)))

(defn- application-status [response {color-conf :color message-conf :message}]
  (if color-conf
          (let [json (j/parse-string (:body response) true)]
            {:color (status-color json color-conf)
             :message (status-message json message-conf)})
          {:color :green
           :message "OK"}))

(defn- fetch-status [url host-config]
  (try
    (let [response (client/get url)]
      (if (= 200 (:status response))
        (application-status response host-config)
        {:color :red}))
    (catch Exception e
      {:color :red
       :message (if-let [data (ex-data e)]
                  (format "Status %s: %s" (:status data) (:body data))
                  (.getMessage e))})))

(defn- status-url [host {url-template :url}]
  (str/replace url-template #"%host%" (:hostname host)))

(defn with-status [host status-url-config]
  (let [host-config (first (filter #(= (:system %) (:system host)) status-url-config))
        url (status-url host host-config)
        status (fetch-status url host-config)]
    (merge host status)))
