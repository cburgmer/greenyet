(ns greenyet.view.patchwork
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [greenyet.view.host-component :as host-component]
            [hiccup.core :refer [h html]]))

(defn- collapse-all-green-hosts [hosts]
  (let [sorted-hosts (sort-by (fn [[{index :index} _]] index) hosts)]
    (if (every? (fn [[_ {color :color}]] (= :green color)) sorted-hosts)
      (take 1 sorted-hosts)
      sorted-hosts)))

(defn- hosts-for-environment [host-list environment]
  (->> host-list
       (filter (fn [[{host-environment :environment} _]]
                 (= environment host-environment)))
       (group-by (fn [[{system :system} _]] system))
       (mapcat (fn [[_ hosts]]
                 (collapse-all-green-hosts hosts)))
       (sort-by (fn [[{system :system} _]] system))))

(defn- environments [host-status-pairs]
  (->> host-status-pairs
       (map first)
       (map :environment)
       distinct))

(defn systems-by-environment [environment-names host-status-pairs]
  (let [the-environments (utils/prefer-order-of environment-names
                                                (environments host-status-pairs)
                                                str/lower-case)]
    (zipmap the-environments
            (map #(hosts-for-environment host-status-pairs %) the-environments))))


(defn- patchwork-as-html [patchwork params]
  (html
   [:header
    (if-not (empty? params)
      [:a.reset-selection {:href "?"}
       "Show all"]
      [:span.reset-selection
       "Show all"])
    (if-not (get params "hideGreen")
      [:a.hide-green {:href (utils/link-select params "hideGreen" "true")}
       "Hide green systems"]
      [:span.hide-green
       "Hide green systems"])]
   (for [[environment host-status] patchwork]
     [:div.environment-wrapper
      [:ol.patchwork {:class environment}
       (for [[host status] host-status]
         (host-component/render host status params))]])))


(defn- in-template [html template]
  (str/replace template
               "<!-- BODY -->"
               html))

(defn render [host-status-pairs page-template environment-names params]
  (let [patchwork (systems-by-environment environment-names host-status-pairs)]
    (in-template (patchwork-as-html patchwork params)
                 page-template)))
