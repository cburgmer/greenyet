(ns greenyet.status
  (:require [cheshire.core :as j]
            json-path
            [org.httpkit.client :as http]))

(defn- missing-item-warning [description config]
  (format "greenyet: Cannot read %s for config '%s'" description config))

(defn- get-simple-key [json key]
  (get json (keyword key)))

(defn- get-simple-key-with-warning [json key description]
  (when key
    (let [value (get-simple-key json key)]
      (if value
        [value nil]
        [value (missing-item-warning description key)]))))

(defn- get-complex-key [json key-conf]
  (if (string? key-conf)
    (get-simple-key json key-conf)
    (let [path (:json-path key-conf)]
      (json-path/at-path path json))))

(defn- configured-color-value [color-conf key default]
  (if (string? color-conf)
    default
    (or (get color-conf key)
        default)))

(defn- status-color [json color-conf]
  (when-let [color-value (get-complex-key json color-conf)]
    (let [green-value (configured-color-value color-conf :green-value "green")
          yellow-value (configured-color-value color-conf :yellow-value "yellow")]
      (cond
        (= green-value color-value) :green
        (= yellow-value color-value) :yellow
        :else :red))))

(defn- component-status [json {color-conf :color name-conf :name message-conf :message}]
  (let [color (status-color json color-conf)
        status-color (or color :red)
        color-error (when-not color
                      (missing-item-warning "component color" color-conf))
        name (get-simple-key json name-conf)
        name-error (when-not name
                     (missing-item-warning "component name" name-conf))
        [message message-error] (get-simple-key-with-warning json message-conf "component message")]
    [{:color status-color
      :name name
      :message message}
     [color-error name-error message-error]]))

(defn- component-statuses [json {path :json-path color-conf :color name-conf :name message-conf :message :as component-conf}]
  (when path
    (if-let [components-json (json-path/at-path path json)]
      (let [status-results (map #(component-status % component-conf) components-json)]
        (apply mapv vector status-results))
      [[] (missing-item-warning "components" path)])))

(defn- status-color-from-components [components]
  (let [colors (map :color components)]
    (let [status-color (if (or (empty? colors)
                               (some #(= % :red) colors))
                         :red
                         (if (some #(= % :yellow) colors)
                           :yellow
                           :green))]
      [status-color nil])))

(defn- overall-status-color [json color-conf components]
  (if color-conf
    (if-let [color (status-color json color-conf)]
      [color nil]
      [:red (missing-item-warning "color" color-conf)])
    (status-color-from-components components)))

(defn- status-from-json [json {color-conf :color
                               message-conf :message
                               package-version-conf :package-version
                               components-conf :components}]
  (let [[components components-error] (component-statuses json components-conf)
        [color color-error] (overall-status-color json color-conf components)
        [package-version package-version-error] (get-simple-key-with-warning json package-version-conf "package-version")
        [message message-error] (get-simple-key-with-warning json message-conf "message")]
    {:color color
     :message (vec (remove nil? (flatten [color-error
                                          package-version-error
                                          components-error
                                          message-error
                                          message])))
     :package-version package-version
     :components components}))

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
