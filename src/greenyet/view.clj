(ns greenyet.view
  (:require [clojure.string :as str]
            [hiccup.util :refer [escape-html]]
            [hiccup.core :refer [html]]))

(defn- index-of [list item]
  (count (take-while (partial not= item) list)))

(defn- prefer-order-of [ordered-references coll-to-sort key-func]
  (sort-by (comp (partial index-of ordered-references)
                 key-func)
           coll-to-sort))


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

(def color-by-importance [:red :yellow :green])

(defn- host-as-html [host]
  [:td {:class (str/join " " ["host" (some-> host
                                             :color
                                             name)])}
   (escape-html (:hostname host))
   (when (:message host)
     [:span.message (escape-html (:message host))])
   (when (:components host)
     [:ol.components
      (for [comp (prefer-order-of color-by-importance (:components host) :color)]
        [:li {:class (str/join " " ["component" (some-> comp
                                                        :color
                                                        name)])
              :title (:message comp)
              :data-name (:name comp)}
         (:name comp)])
      [:li.more]])])

(defn- environment-table-as-html [environments rows]
  (html [:table
         [:thead
          [:tr
           [:td]
           (for [env environments]
             [:td (escape-html env)])]]
         [:tbody
          (for [row rows]
            [:tr
             [:td
              (let [system-name (escape-html (some :system row))]
                [:a {:href (str/join ["?systems=" system-name])}
                 system-name])]
             (for [host row]
               (host-as-html host))])]]))


(defn- filter-systems [host-list-with-status selected-systems]
  (if selected-systems
    (filter (fn [{system :system}]
              (contains? (set selected-systems) system))
            host-list-with-status)
    host-list-with-status))

(defn render [host-list-with-status selected-systems page-template environment-names]
  (let [environments (prefer-order-of environment-names
                                      (distinct (map :environment host-list-with-status))
                                      str/lower-case)
        selected-hosts (filter-systems host-list-with-status selected-systems)
        rows (environment-table environments selected-hosts)]
    (str/replace page-template
                 #"<!-- BODY -->"
                 (environment-table-as-html environments rows))))
