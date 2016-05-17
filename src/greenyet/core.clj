(ns greenyet.core
  (:require [hiccup.core :refer [html]]
            [clj-http.client :as client]
            [clojure.string :as str]))


(def dummy-host-list [{:host "localhost" :environment "DEV" :system "MySystem"}
                      {:host "localhost" :environment "PROD" :system "MySystem"}
                      {:host "localhost" :environment "DEV" :system "AnotherSystem"}])


(defn- fetch-status [url]
  (try
    (let [status (:status (client/get url))]
      (if (= 200 status)
        :green
        :red))
    (catch Exception _
      :red)))

(defn- status-url [host]
  (format "http://%s:8000/found" (:host host)))

(defn- with-status [host]
  (let [status (fetch-status (status-url host))]
    (assoc host
           :color status)))


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


(defn- render [host-list-with-status]
  (let [environments (distinct (map :environment host-list-with-status))
        rows (environment-table environments host-list-with-status)]
    (environment-table-as-html environments rows)))

(defn handler [_]
  (let [host-list-with-status (map with-status dummy-host-list)]
    {:body (render host-list-with-status)}))
