(ns greenyet.view.patchwork
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [greenyet.view.host-component :as host-component]
            [hiccup.core :refer [h html]]))

(defn- collapse-all-green-hosts [host-entries]
  (if (every? (fn [[_ {color :color}]] (= :green color)) host-entries)
    (take 1 host-entries)
    host-entries))

(defn- hosts-for-environment [host-list environment]
  (->> host-list
       (filter (fn [[{host-environment :environment} _]]
                 (= environment host-environment)))
       (sort-by (fn [[{index :index} _]] index))
       collapse-all-green-hosts))

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

(defn- environment-table-to-patchwork [environments table]
  (apply merge-with concat (map (partial zipmap environments) table)))

(defn- table-as-html [environments rows]
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
                  (host-component/render host status))])])]]))


(defn- patchwork-as-html [patchwork]
  (html (for [[environment host-status] patchwork]
          [:div.environment-wrapper
           [:ol.patchwork {:class environment}
            (for [[host status] host-status]
              (host-component/render host status))]])))


(defn- in-template [html template]
  (str/replace template
               "<!-- BODY -->"
               html))

(defn- filter-systems [host-status-pairs systems]
  (let [selected-systems (set (map str/lower-case systems))]
    (filter (fn [[{system :system} _]]
              (contains? selected-systems (str/lower-case system)))
            host-status-pairs)))

(defn- environments [host-status-pairs environments]
  (let [selected-environments (set (map str/lower-case environments))
        all-environments (->> host-status-pairs
                              (map first)
                              (map :environment)
                              distinct)]
    (if (seq selected-environments)
      (filter #(contains? selected-environments (str/lower-case %))
              all-environments)
      all-environments)))

(defn render [host-status-pairs selected-systems selected-environments page-template environment-names]
  (let [the-environments (utils/prefer-order-of environment-names
                                                (environments host-status-pairs selected-environments)
                                                str/lower-case)
        selected-entries (cond-> host-status-pairs
                           selected-systems (filter-systems selected-systems))
        rows (environment-table the-environments selected-entries)

        patchwork (environment-table-to-patchwork the-environments rows)]
    (in-template (patchwork-as-html patchwork)
                 page-template)))
