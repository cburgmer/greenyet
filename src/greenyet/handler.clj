(ns greenyet.handler
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]]
            [greenyet
             [config :as config]
             [selection :as selection]
             [utils :as utils]]
            [greenyet.view
             [patchwork :as patchwork]
             [styleguide :as styleguide]]
            [ring.middleware
             [content-type :as content-type]
             [not-modified :as not-modified]
             [params :as params]
             [resource :as resource]]
            [ring.util
             [response :refer [header]]
             [time :refer [format-date]]]))

(defn- page-template []
  (-> "index.template.html" io/resource slurp))

(defn- styleguide-template []
  (-> "styleguide.template.html" io/resource slurp))

(defn- environment-names []
  (-> "environment_names.yaml" io/resource slurp yaml/parse-string))


(defn cache-headers [response last-changed]
  (-> response
      (header "Last-Modified" (format-date (.toDate last-changed)))
      (header "Cache-Control" "max-age=0,must-revalidate")))

(defn- selected-host-with-statuses [host-with-statuses params]
  (let [hide-green (= (get params "hideGreen") "true")]
    (selection/filter-hosts host-with-statuses
                            (get params "systems")
                            (get params "environments")
                            hide-green)))

(defn- render-environments [params statuses]
  (let [[host-with-statuses last-changed] @statuses]
    (-> host-with-statuses
        (selected-host-with-statuses params)
        (patchwork/render (page-template)
                          (environment-names)
                          params)
        utils/html-response
        (cache-headers last-changed))))


(defn- status-statistics [statuses]
  (merge {:green 0 :yellow 0 :red 0}
         (frequencies (map :color statuses))))

(defn- statistics [host-with-statuses]
  (let [machine-configs (keys host-with-statuses)
        statuses (vals host-with-statuses)]
    {:environments (count (distinct (map :environment machine-configs)))
     :systems (count (distinct (map :system machine-configs)))
     :machines (count machine-configs)
     :statuses (status-statistics statuses)
     :statuses-by-system (->> host-with-statuses
                              (group-by (fn [[config status]] (:environment config)))
                              (map (fn [[environment host-entries]]
                                     [environment (->> host-entries
                                                       (group-by (fn [[config status]] (:system config)))
                                                       (map (fn [[system entries]]
                                                              [system (->> entries (map second) status-statistics)]))
                                                       (into {}))]))
                              (into {}))}))

(defn- render-status [statuses]
  (let [[host-with-statuses last-changed] @statuses
        machine-configs (keys host-with-statuses)
        statuses (vals host-with-statuses)]
    (-> {:host (.getCanonicalHostName (java.net.InetAddress/getLocalHost))
         :last-changed (str last-changed)
         :statistics (statistics host-with-statuses)
         :greenyet-version (some-> (io/resource "project.clj") slurp read-string (nth 2))} ; the version will be incorrect as long as it's run from ./lein ring server
        utils/json-response)))

(defn- sort-by-system [host-with-statuses]
  (->> host-with-statuses
       (sort-by (fn [[{system :system} _]] system))))

(defn- render-all [params statuses]
  (let [[host-with-statuses last-changed] @statuses]
    (-> host-with-statuses
        (selected-host-with-statuses params)
        (sort-by-system)
        utils/json-response
        (cache-headers last-changed))))

(defn- render-styleguide-entry [params]
  (utils/html-response (styleguide/render (keywordize-keys params) (styleguide-template))))

(defn- render [{params :params uri :uri} statuses]
  (cond
    (and config/development?
         (= "/styleguide" uri)) (render-styleguide-entry params)
    (= "/all.json" uri) (render-all params statuses)
    (= "/status.json" uri) (render-status statuses)
    :else (render-environments params statuses)))


(defn create [statuses]
  (-> (fn [request]
        (render request statuses))
      (utils/wrap-param-lists ["systems" "environments"])
      params/wrap-params
      (resource/wrap-resource "public")
      content-type/wrap-content-type
      not-modified/wrap-not-modified))
