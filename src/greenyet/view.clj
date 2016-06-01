(ns greenyet.view
  (:require [clojure.string :as str]
            [greenyet
             [host-component :as host-component]
             [utils :as utils]]
            [hiccup.core :refer [h html]]))

(defn- hosts-for-environment [host-list environment]
  (->> host-list
       (filter (fn [[{host-environment :environment} _]]
                 (= environment host-environment)))
       (sort-by (fn [[{index :index} _]] index))))

(defn- system-row [environments host-list]
  (map (fn [environment]
         (hosts-for-environment host-list environment))
       environments))

(defn- environment-table [environments host-list]
  (->> host-list
       (group-by (fn [[{system :system} _]] system))
       (sort (fn [[system1 _] [system2 _]]
               (compare system1 system2)))
       (map (fn [[_ system-host-list]]
              (system-row environments system-host-list)))))


(defn- environment-table-as-html [environments rows]
  (html [:table
         [:colgroup {:span 1}]
         [:colgroup.environments {:span (count environments)}]
         [:thead
          [:tr
           [:td.system-name]
           (for [env environments]
             [:td (h env)])]]
         [:tbody
          (for [row rows]
            [:tr
             [:td.system-name
              (let [system-name (h (some :system (mapcat first row)))]
                [:a {:href (str/join ["?systems=" (h system-name)])}
                 (h system-name)])]
             (for [cell row]
               [:td.hosts
                (for [[host status] cell]
                  (when status
                    (host-component/render host status)))])])]]))

(defn- in-template [html template]
  (str/replace template
               "<!-- BODY -->"
               html))

(defn- filter-systems [host-status-pairs selected-systems]
  (if selected-systems
    (filter (fn [[{system :system} _]]
              (contains? (set selected-systems) system))
            host-status-pairs)
    host-status-pairs))

(defn render [host-status-pairs selected-systems page-template environment-names]
  (let [environments (utils/prefer-order-of environment-names
                                      (->> host-status-pairs
                                           (map first)
                                           (map :environment)
                                           distinct)
                                      str/lower-case)
        selected-entries (filter-systems host-status-pairs selected-systems)
        rows (environment-table environments selected-entries)]
    (in-template (environment-table-as-html environments rows)
                 page-template)))
