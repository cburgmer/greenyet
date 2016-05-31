(ns greenyet.utils
  (:require [clojure.string :as str]
            [ring.util.response :refer [charset content-type response]]))

(defn query-param-as-vec [params key]
  (let [value (get params key)
        value-vector (if (string? value)
                       (vector value)
                       value)]
    (seq (mapcat #(str/split % #",") value-vector))))

(defn html-response [body]
  (-> (response body)
      (content-type "text/html")
      (charset "UTF-8")))
