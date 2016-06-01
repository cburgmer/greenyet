(ns greenyet.utils
  (:require [clojure.string :as str]
            [ring.util.response :refer [charset content-type response]]))

(defn- index-of [list item]
  (count (take-while (partial not= item) list)))

(defn prefer-order-of [ordered-references coll-to-sort key-func]
  (sort-by (comp (partial index-of ordered-references)
                 key-func)
           coll-to-sort))


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
