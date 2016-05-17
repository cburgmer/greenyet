(ns greenyet.core
  (:require [hiccup.core :refer [html]]
            [clj-http.client :as client]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.string :as str]))


(defn- fetch-status [url]
  (try
    (let [status (:status (client/get url))]
      (if (= 200 status)
        :green
        :red))
    (catch Exception _
      :red)))

(defn- status-url [host status-url-config]
  (let [host-config (first (filter #(= (:system %) (:system host)) status-url-config))
        url-template (:url host-config)]
    (str/replace url-template #"%host%" (:hostname host))))

(defn- with-status [host status-url-config]
  (let [status (fetch-status (status-url host status-url-config))]
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
   (:hostname host)])

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


(defn- read-host-list [config-dir]
  (let [host-list-file (io/file config-dir "hosts.yaml")]
    (yaml/parse-string (slurp host-list-file))))

(defn- read-status-url-config [config-dir]
  (let [config-file (io/file config-dir "status_url.yaml")]
    (yaml/parse-string (slurp config-file))))

(def status-url-config (read-status-url-config "example"))

(println status-url-config)


(defn handler [_]
  (let [host-list (read-host-list "example")
        host-list-with-status (map (fn [host] (with-status host status-url-config))
                                   host-list)]
    {:body (render host-list-with-status)}))
