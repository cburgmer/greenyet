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

(defn- application-status [json color-conf]
  (let [color (extract-color json color-conf)
        green-value (color-value color-conf :green-value "green")
        yellow-value (color-value color-conf :yellow-value "yellow")]
    (cond
      (= green-value color) :green
      (= yellow-value color) :yellow
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
