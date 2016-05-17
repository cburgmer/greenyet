(ns greenyet.view
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]))

(defn- host-for-environment [host-list environment]
  (first (filter #(= environment (:environment %)) host-list)))

(defn- system-row [environments host-list]
  (map (fn [environment]
         (host-for-environment host-list environment))
       environments))

(defn- environment-table [environments host-list]
  (->> host-list
       (group-by :system)
       (sort (fn [[system1 _] [system2 _]]
               (compare system1 system2)))
       (map (fn [[_ system-host-list]]
              (system-row environments system-host-list)))))


(defn- host-as-html [host]
  [:td {:class (str/join " " ["host" (some-> host
                                             :color
                                             name)])}
   (:hostname host)])

(defn- environment-table-as-html [environments rows]
  (html [:head
         [:meta {:http-equiv "refresh" :content "5"}]
         [:style (str/join "\n" [".host.green { background-color: green; }"
                                 ".host.yellow { background-color: yellow; }"
                                 ".host { background-color: red; }"
                                 ".host:empty { background: gray; }"])]]
        [:body
         [:table
          [:thead
           [:tr
            [:td]
            (for [env environments]
              [:td env])]]
          [:tbody
           (for [row rows]
             [:tr
              [:td (:system (first row))]
              (for [host row]
                (host-as-html host))])]]]))


(defn render [host-list-with-status]
  (let [environments (sort (distinct (map :environment host-list-with-status)))
        rows (environment-table environments host-list-with-status)]
    (environment-table-as-html environments rows)))
