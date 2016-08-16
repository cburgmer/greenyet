(ns greenyet.status
  (:require [cheshire.core :as j]
            json-path
            [org.httpkit.client :as http]))

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
    (if color
      (let [color (cond
                    (= green-value color) :green
                    (= yellow-value color) :yellow
                    :else :red)]
        [color nil])
      [:red (format "Cannot read color for config %s" color-conf)])))

(defn- component-statuses [json {path :json-path color-conf :color name-conf :name message-conf :message}]
  (when path
    (->> (json-path/at-path path json)
         (map (fn [component]
                {:color (first (status-color component color-conf))
                 :name (get-simple-key component name-conf)
                 :message (get-simple-key component message-conf)})))))

(defn- status-color-from-components [components]
  (let [colors (map :color components)]
    (let [status-color (if (or (empty? colors)
                               (some #(= % :red) colors))
                         :red
                         (if (some #(= % :yellow) colors)
                           :yellow
                           :green))]
      [status-color nil])))

(defn- overall-status-color [json color-conf components-conf]
  (if color-conf
    (status-color json color-conf)
    (status-color-from-components (component-statuses json components-conf))))

(defn- status-from-json [json {color-conf :color
                               message-conf :message
                               package-version-conf :package-version
                               components-conf :components}]
  (let [[color color-message] (overall-status-color json color-conf components-conf)]
    {:color color
     :message (or color-message (get-simple-key json message-conf))
     :package-version (get-simple-key json package-version-conf)
     :components (component-statuses json components-conf)}))

(defn- application-status [response {color-conf :color
                                     components-conf :components
                                     :as config}]
  (if (or color-conf components-conf)
    (let [json (j/parse-string (:body response) true)]
      (status-from-json json config))
    {:color :green
     :message "OK"}))

(defn- message-for-http-response [response]
  (format "Status %s: %s" (:status response) (:body response)))


(defn- http-get [status-url timeout-in-ms callback]
  (http/get status-url
            {:headers {"Accept" "application/json"}
             :follow-redirects false
             :user-agent "greenyet"
             :timeout timeout-in-ms}
            callback))

(defn- identify-status [response timeout-in-ms config]
  (try
    (if (instance? org.httpkit.client.TimeoutException (:error response))
      {:color :red
       :message (format "Request timed out after %s milliseconds" timeout-in-ms)}
      (if (= 200 (:status response))
        (application-status response config)
        {:color :red
         :message (message-for-http-response response)}))
    (catch Exception e
      {:color :red
       :message (if-let [response (ex-data e)]
                  (message-for-http-response response)
                  (.getMessage e))})))

(defn fetch-status [{:keys [status-url config]} timeout-in-ms callback]
  (http-get status-url
            timeout-in-ms
            (fn [response]
              (callback (identify-status response timeout-in-ms config)))))
