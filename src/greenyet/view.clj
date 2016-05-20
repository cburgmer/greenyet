(ns greenyet.view
  (:require [clojure.string :as str]
            [hiccup.util :refer [escape-html]]
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
   (escape-html (:hostname host))
   (when (:message host)
     [:span.message (escape-html (:message host))])
   (when (:components host)
     [:ol.components
      (for [comp (:components host)]
       [:li {:class (str/join " " ["component" (some-> comp
                                                        :color
                                                        name)])
              :title (:message comp)}
        (:name comp)])])])

(defn- environment-table-as-html [environments rows]
  (html [:head
         [:meta {:http-equiv "refresh" :content "5"}]
         [:style (str/join "\n" [".host.green { background-color: green; }"
                                 ".host.yellow { background-color: yellow; }"
                                 ".host { background-color: red; height: 50px; }"
                                 ".host:empty { background: lightgray; }"
                                 ".components { background: white; margin: 10px; }"
                                 ".component.green::before { background-color: green; }"
                                 ".component.yellow::before { background-color: yellow; }"
                                 ".component::before { content: ''; display: inline-block; background-color: red; width: 1.2rem; height: 1.2rem; vertical-align: middle; margin-right: 5px; }"
                                 ".component { display: inline-block; height: 1.2rem; line-height: 1.2rem; margin: 6px; }"
                                 ".message { display: block; padding-top: 4px; font-style: italic; font-size: 0.9rem; overflow: hidden; text-overflow: ellipsis; width: 400px; max-height: 8em; margin: auto; }"
                                 "body { font-family: sans-serif; }"
                                 "table { border-collapse: separate; border-spacing: 0; width: 100%; table-layout: fixed; }"
                                 "td { text-align: center; padding: 5px; border: solid white 0; border-width: 2px 4px 2px 0; }"
                                 "td:first-child { text-align: left; padding: 5px 5px 5px 0; width: 200px; word-wrap: break-word; }"
                                 "ol { padding: 0; list-style: none; }"
                                 ".project-link { font-size: 0.8rem; text-decoration: none; color: gray; letter-spacing: 0.2rem; }"])]]
        [:body
         [:a.project-link {:href "https://github.com/cburgmer/greenyet"} "Green yet?"]
         [:table
          [:thead
           [:tr
            [:td]
            (for [env environments]
              [:td (escape-html env)])]]
          [:tbody
           (for [row rows]
             [:tr
              [:td (escape-html (some :system row))]
              (for [host row]
                (host-as-html host))])]]]))


(defn render [host-list-with-status]
  (let [environments (sort (distinct (map :environment host-list-with-status)))
        rows (environment-table environments host-list-with-status)]
    (environment-table-as-html environments rows)))
