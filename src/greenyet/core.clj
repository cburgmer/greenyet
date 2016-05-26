(ns greenyet.core
  (:require [clj-yaml.core :as yaml]
            [clojure
             [string :as str]
             [walk :refer [keywordize-keys]]]
            [clojure.java.io :as io]
            [greenyet
             [config :as config]
             [poll :as poll]
             [view :as view]
             [styleguide :as styleguide]]
            [ring.middleware
             [not-modified :as not-modified]
             [params :as params]
             [resource :as resource]]
            [ring.util
             [response :refer [charset content-type header response]]
             [time :refer [format-date]]])
  (:import java.io.FileNotFoundException))

(def ^:private config-dir (System/getenv "CONFIG_DIR"))

(def ^:private timeout-in-ms (or (some-> (System/getenv "TIMEOUT")
                                         Integer/parseInt)
                                 5000))

(def ^:private config-params [["CONFIG_DIR" (or config-dir
                                                "")]
                              ["TIMEOUT" timeout-in-ms]
                              ["PORT" (or (System/getenv "PORT")
                                          3000)]])

(def development? (= "development" (System/getProperty "greenyet.environment")))

(defn- page-template []
  (-> "index.template.html" io/resource slurp))

(defn- styleguide-template []
  (-> "styleguide.template.html" io/resource slurp))

(defn- environment-names []
  (-> "environment_names.yaml" io/resource slurp yaml/parse-string))

(defn- query-param-as-vec [params key]
  (let [value (get params key)
        value-vector (if (string? value)
                     (vector value)
                     value)]
    (seq (mapcat #(str/split % #",") value-vector))))

(defn- html-response [body]
  (-> (response body)
      (content-type "text/html")
      (charset "UTF-8")))


(defn- render [{params :params uri :uri}]
  (if (and development?
           (= "/styleguide" uri))
    (html-response (styleguide/render (keywordize-keys params) (styleguide-template)))
    (let [[host-with-statuses last-changed] @poll/statuses]
      (-> (html-response (view/render host-with-statuses
                                 (query-param-as-vec params "systems")
                                 (page-template)
                                 (environment-names)))
          (header "Last-Modified" (format-date (.toDate last-changed)))))))


(defn init []
  (println "Starting greenyet with config")
  (->> config-params
       (map (fn [[env-var value]]
              (format "  %s: '%s'" env-var value)))
       (map println)
       doall)
  (try
    (poll/start-polling (config/hosts-with-config config-dir) timeout-in-ms)
    (catch FileNotFoundException e
      (binding [*out* *err*]
        (println (.getMessage e)))
      (System/exit 1))))

(def handler
  (-> render
      params/wrap-params
      (resource/wrap-resource "public")
      not-modified/wrap-not-modified))
