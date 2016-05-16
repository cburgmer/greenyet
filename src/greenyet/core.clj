(ns greenyet.core
  (:require [hiccup.core :refer [html]]
            [clojure.string :as str]))

(def dummy-host-list [{:host "abcd1234" :environment "DEV" :system "MySystem"}
                      {:host "xyz123" :environment "PROD" :system "MySystem"}
                      {:host "fgh456" :environment "DEV" :system "AnotherSystem"}])

(def host-list-with-status (map #(assoc % :color :yellow) dummy-host-list))


(defn- host-for-environment [host-list environment]
  (first (filter #(= environment (:environment %)) host-list)))

(defn- system-row [environments host-list]
  (map (fn [environment]
         (host-for-environment host-list environment))
       environments))

(defn- environment-table [environments host-list]
  (->> host-list
       (group-by :system)
       (map (fn [[_ system-host-list]]
              (system-row environments system-host-list)))))


(defn- host-as-html [host]
  [:td {:class (str/join " " ["host" (some-> host
                                             :color
                                             name)])}
   (:host host)])

(defn- environment-table-as-html [environments rows]
  (html [:head
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


(defn handler [x]
  (let [environments (distinct (map :environment host-list-with-status))
        rows (environment-table environments host-list-with-status)]
    {:body (environment-table-as-html environments rows)}))
