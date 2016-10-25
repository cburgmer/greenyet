(ns greenyet.utils
  (:require [clojure.string :as str]
            [ring.util
             [codec :as codec]
             [response :refer [charset content-type response]]]))

(defn- index-of [list item]
  (count (take-while (partial not= item) list)))

(defn prefer-order-of [ordered-references coll-to-sort key-func]
  (sort-by (comp (partial index-of ordered-references)
                 key-func)
           coll-to-sort))


(defn- param-list-value [value]
  (let [value-vector (if (string? value)
                       (vector value)
                       value)]
    (seq (mapcat #(str/split % #",") value-vector))))

(defn- param-lists-request [request keys]
  (let [query-params (->> request
                          :query-params
                          (map (fn [[key value]]
                                 [key (if (contains? keys key)
                                        (param-list-value value)
                                        value)]))
                          (into {}))]
    (merge-with merge request {:query-params query-params
                               :params query-params})))

(defn wrap-param-lists [handler keys]
  (let [key-set (set keys)]
    (fn [request]
      (handler (param-lists-request request key-set)))))


(defn link-select [params key value]
  (let [query (-> params
                  (assoc key value)
                  codec/form-encode)]
    (format "?%s" query)))


(defn html-response [body]
  (-> (response body)
      (content-type "text/html")
      (charset "UTF-8")))


(defn json-response [json]
  (-> (response json)
      (content-type "application/json")
      (charset "UTF-8")))