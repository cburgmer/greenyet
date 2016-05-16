(ns greenyet.core
  (:require [hiccup.core :refer [html]]))

(def dummy-host-list [{:host "abcd1234" :environment "DEV" :system "MySystem"}
                      {:host "xyz123" :environment "PROD" :system "MySystem"}])


(defn- environment-entry [host-list environment]
  (first (filter #(= environment (:environment %)) host-list)))

(defn handler [x]
  (let [environments (distinct (map :environment dummy-host-list))
        row (map (partial environment-entry dummy-host-list) environments)]
    {:body (html [:table
                  [:thead
                   [:tr
                    (cons [:td]
                          (map (fn [env] [:td env]) environments))]]
                  [:tbody
                   [:tr
                    (cons [:td (:system (first dummy-host-list))]
                          (map (fn [host] [:td (:host host)]) row))]]])}))
