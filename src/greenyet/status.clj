(ns greenyet.status
  (:require [cheshire.core :as j]
            [clj-http
             [client :as client]
             [util :refer [parse-content-type]]]
            [clojure.string :as str]))

(defn- application-status [json]
  (if (= "green" (:color json))
    :green
    :red))

(defn- content-type [response]
  (-> response
      :headers
      (get "Content-Type")
      parse-content-type
      :content-type))

(defn- fetch-status [url]
  (try
    (let [response (client/get url)]
      (if (= 200 (:status response))
        (if (= :application/json (content-type response))
          (application-status (j/parse-string (:body response)))
          :green)
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
