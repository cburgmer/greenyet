(ns greenyet.parse
  (:require json-path
            [clojure.string :as str]))

(defn- missing-item-warning [description config]
  (format "greenyet: Cannot read %s for config '%s'" description config))

(defn- get-simple-key [json key]
  (get json (keyword key)))

(defn- optional-value [json key description]
  (when key
    (if (contains? json (keyword key))
      [(get-simple-key json key) nil]
      [nil (missing-item-warning description key)])))

(defn- optional-non-nil-value [json key description]
  (when key
    (let [value (get-simple-key json key)]
      (if (nil? value)
        [nil (missing-item-warning description key)]
        [value nil]))))

(defn- get-complex-key [json key-conf]
  (if (string? key-conf)
    (get-simple-key json key-conf)
    (let [path (:json-path key-conf)]
      (json-path/at-path path json))))

(defn- key-path-to-str [path]
  (->> path
       (map #(if (keyword? %)
               (name %)
               (str %)))))

(defn- get-entries [json key-conf]
  (if (string? key-conf)
    (let [match (get-simple-key json key-conf)]
      (when (sequential? match)
        (map-indexed (fn [idx value] [[key-conf idx] value]) match)))
    (let [path (:json-path key-conf)
          match (json-path/query path json)]
      (cond
        (sequential? match) (map (fn [m] [(key-path-to-str (:path m)) (:value m)]) match)
        (sequential? (:value match)) (map-indexed (fn [idx value]
                                                    [(-> (:path match)
                                                         key-path-to-str
                                                         vec
                                                         (conj idx))
                                                     value])
                                                  (:value match))))))

(defn- configured-color-value [color-conf key default]
  (if (string? color-conf)
    default
    (or (get color-conf key)
        default)))

(defn- status-color [json color-conf]
  (let [color-value (get-complex-key json color-conf)]
    (when-not (nil? color-value)
      (let [green-value (configured-color-value color-conf :green-value "green")
            yellow-value (configured-color-value color-conf :yellow-value "yellow")]
        (cond
          (= green-value color-value) :green
          (= yellow-value color-value) :yellow
          :else :red)))))

(defn- component [key json {user-color-conf :color user-name-conf :name message-conf :message}]
  (let [color-conf (or user-color-conf "color")
        name-conf user-name-conf
        color (status-color json color-conf)
        status-color (or color :red)
        color-error (when-not color
                      (missing-item-warning "component color" color-conf))
        [name name-error] (if name-conf
                            (optional-non-nil-value json name-conf "component name")
                            [(str/join " " key) nil])
        [message message-error] (optional-value json message-conf "component message")]
    [{:color status-color
      :name name
      :message message}
     [color-error name-error message-error]]))

(defn components [json {component-conf :components}]
  (when component-conf
    (if-let [components-json (get-entries json component-conf)]
      (if (sequential? components-json)
        (let [status-results (map (fn [[key entry]] (component key entry component-conf)) components-json)]
          (apply mapv vector status-results))
        [[] [(format "greenyet: List expected from components for config '%s'" component-conf)]])
      [[] [(missing-item-warning "components" component-conf)]])))

(components {:jobs [{:status "green"}]}
                {:components {:json-path "$.jobs[*]"
                              :color "status"}})

(components {:jobs [{:color "green"}]}
            {:components {:json-path "$.jobs"}})

(component [0] {:color "green"} {})

(defn color [json {color-conf :color}]
  (when color-conf
    (if-let [color (status-color json color-conf)]
      [color nil]
      [:red (missing-item-warning "color" color-conf)])))

(defn package-version [json {package-version-conf :package-version}]
  (optional-non-nil-value json package-version-conf "package-version"))

(defn message [json {message-conf :message}]
  (optional-value json message-conf "message"))
