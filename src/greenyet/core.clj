(ns greenyet.core
  (:require [clj-yaml.core :as yaml]
            [clojure
             [string :as str]
             [walk :refer [keywordize-keys]]]
            [clojure.java.io :as io]
            [greenyet
             [config :as config]
             [poll :as poll]
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
             [time :refer [format-date]]])
  (:import java.io.FileNotFoundException))

(defn- page-template []
  (-> "index.template.html" io/resource slurp))

(defn- styleguide-template []
  (-> "styleguide.template.html" io/resource slurp))

(defn- environment-names []
  (-> "environment_names.yaml" io/resource slurp yaml/parse-string))


(defn- render-environments [params]
  (let [[host-with-statuses last-changed] @poll/statuses]
    (-> (utils/html-response (patchwork/render host-with-statuses
                                               (utils/query-param-as-vec params "systems")
                                               (utils/query-param-as-vec params "environments")
                                               (page-template)
                                               (environment-names)))
        (header "Last-Modified" (format-date (.toDate last-changed)))
        (header "Cache-Control" "max-age=0,must-revalidate"))))

(defn- render-styleguide-entry [params]
  (utils/html-response (styleguide/render (keywordize-keys params) (styleguide-template))))

(defn- render [{params :params uri :uri}]
  (if (and config/development?
           (= "/styleguide" uri))
    (render-styleguide-entry params)
    (render-environments params)))


(def ^:private config-help (str/join "\n"
                                     ["To kick off, why don't you create a file hosts.yaml with"
                                      ""
                                      "- hostname: localhost"
                                      "  system: greenyet"
                                      "  environment: Development"
                                      ""
                                      "and a status_url.yaml with"
                                      ""
                                      "- url: http://%hostname%:3000/"
                                      "  system: greenyet"
                                      ""]))

(defn init []
  (try
    (poll/start-polling (config/hosts-with-config) config/polling-interval-in-ms)
    (catch FileNotFoundException e
      (binding [*out* *err*]
        (println (.getMessage e))
        (println)
        (println config-help))
      (System/exit 1)))
  (println "Starting greenyet with config")
  (println (config/config-params-as-string)))

(def handler
  (-> render
      params/wrap-params
      (resource/wrap-resource "public")
      content-type/wrap-content-type
      not-modified/wrap-not-modified))
