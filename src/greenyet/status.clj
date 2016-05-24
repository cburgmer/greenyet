(ns greenyet.status
  (:require [cheshire.core :as j]
            [clj-http.client :as client]
            [json-path]
            [clojure.string :as str]))

(defn- get-simple-key [json key]
  (get json (keyword key)))

(defn- get-complex-key [json key-conf]
  (if (string? key-conf)
    (get-simple-key json key-conf)
    (let [path (:json-path key-conf)]
      (json-path/at-path path json))))

(defn- color-value [color-conf key default]
  (if (string? color-conf)
    default
    (or (get color-conf key)
        default)))

(defn- status-color [json color-conf]
  (let [color (get-complex-key json color-conf)
        green-value (color-value color-conf :green-value "green")
        yellow-value (color-value color-conf :yellow-value "yellow")]
    (cond
      (= green-value color) :green
      (= yellow-value color) :yellow
      :else :red)))

(defn- component-statuses [json {path :json-path color-conf :color name-conf :name message-conf :message}]
  (when path
    (->> (json-path/at-path path json)
         (map (fn [component]
                {:color (status-color component color-conf)
                 :name (get-simple-key component name-conf)
                 :message (get-simple-key component message-conf)})))))

(defn- application-status [response {color-conf :color
                                     message-conf :message
                                     package-version-conf :package-version
                                     components-conf :components :as m}]
  (if color-conf
    (let [json (j/parse-string (:body response) true)]
      {:color (status-color json color-conf)
       :message (get-simple-key json message-conf)
       :package-version (get-simple-key json package-version-conf)
       :components (component-statuses json components-conf)})
    {:color :green
     :message "OK"}))

(defn- message-for-http-response [response]
  (format "Status %s: %s" (:status response) (:body response)))

(defn- fetch-status [url host-config]
  (try
    (let [response (client/get url)]
      (if (= 200 (:status response))
        (application-status response host-config)
        {:color :red
         :message (message-for-http-response response)}))
    (catch Exception e
      {:color :red
       :message (if-let [response (ex-data e)]
                  (message-for-http-response response)
                  (.getMessage e))})))

(defn- status-url [host {url-template :url}]
  (str/replace url-template #"%host%" (:hostname host)))

(defn with-status [host status-url-config]
  (let [host-config (first (filter #(= (:system %) (:system host)) status-url-config))
        url (status-url host host-config)
        status (fetch-status url host-config)]
    (merge host status {:status-url url})))
