(ns greenyet.parse
  (:require json-path))

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

(defn- component [json {color-conf :color name-conf :name message-conf :message}]
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

(defn components [json {component-conf :components}]
  (when-let [path (:json-path component-conf)]
    (if-let [components-json (json-path/at-path path json)]
      (let [status-results (map #(component % component-conf) components-json)]
        (apply mapv vector status-results))
      [[] (missing-item-warning "components" path)])))

(defn color [json {color-conf :color}]
  (if-let [color (status-color json color-conf)]
    [color nil]
    [:red (missing-item-warning "color" color-conf)]))

(defn package-version [json {package-version-conf :package-version}]
  (get-simple-key-with-warning json package-version-conf "package-version"))

(defn message [json {message-conf :message}]
  (get-simple-key-with-warning json message-conf "message"))
