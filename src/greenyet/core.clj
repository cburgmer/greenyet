(ns greenyet.core
  (:require [hiccup.core :refer [html]]))

(def dummy-host-list [{:host "abcd1234" :environment "DEV" :system "MySystem"}
                      {:host "xyz123" :environment "PROD" :system "MySystem"}
                      {:host "fgh456" :environment "DEV" :system "AnotherSystem"}])


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


(defn- environment-table-as-html [environments rows]
  (html [:table
         [:thead
          [:tr
           (cons [:td]
                 (map (fn [env] [:td env]) environments))]]
         [:tbody
          (map (fn [row]
                 [:tr
                  (cons [:td (:system (first row))]
                        (map (fn [host] [:td (:host host)]) row))])
               rows)]]))


(defn handler [x]
  (let [environments (distinct (map :environment dummy-host-list))
        rows (environment-table environments dummy-host-list)]
    {:body (environment-table-as-html environments rows)}))
