(ns greenyet.view
  (:require [clojure.string :as str]
            [hiccup.core :refer [h html]]))

(defn- index-of [list item]
  (count (take-while (partial not= item) list)))

(defn- prefer-order-of [ordered-references coll-to-sort key-func]
  (sort-by (comp (partial index-of ordered-references)
                 key-func)
           coll-to-sort))


(defn- hosts-for-environment [host-list environment]
  (filter (fn [[{host-environment :environment} _]]
            (= environment host-environment))
          host-list))

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


(defn- message [{message :message}]
  (if (vector? message)
    (str/join ", " message)
    message))

(def ^:private color-by-importance [:red :yellow :green])

(defn host-as-html [host status]
  [:div {:class (str/join " " ["host" (some-> status
                                              :color
                                              name
                                              h)])}
   [:span.host-name (h (:hostname host))]
   [:a {:href (h (:status-url host))}
    (h (if (:package-version status)
         (:package-version status)
         (:system host)))]
   (when (:message status)
     [:span.message (h (message status))])
   (when (:components status)
     (let [components-id (h (str/join ["components-" (:system host) "-" (:environment host)]))]
       [:a.show-components {:href (str/join ["#" components-id]) }
        [:ol.components {:id components-id}
         (for [comp (prefer-order-of color-by-importance (:components status) :color)]
           [:li {:class (str/join " " ["component" (some-> comp
                                                           :color
                                                           name
                                                           h)])
                 :title (h (message comp))
                 :data-name (h (:name comp))}
            (h (:name comp))])
         [:li.more]]]))])

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
                    (host-as-html host status)))])])]]))

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
  (let [environments (prefer-order-of environment-names
                                      (->> host-status-pairs
                                           (map first)
                                           (map :environment)
                                           distinct)
                                      str/lower-case)
        selected-entries (filter-systems host-status-pairs selected-systems)
        rows (environment-table environments selected-entries)]
    (in-template (environment-table-as-html environments rows)
                 page-template)))
