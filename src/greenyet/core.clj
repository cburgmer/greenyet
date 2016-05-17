(ns greenyet.core
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [greenyet
             [status :as status]
             [view :as view]]))

(defn- read-host-list [config-dir]
  (let [host-list-file (io/file config-dir "hosts.yaml")]
    (yaml/parse-string (slurp host-list-file))))

(defn- read-status-url-config [config-dir]
  (let [config-file (io/file config-dir "status_url.yaml")]
    (yaml/parse-string (slurp config-file))))


(def ^:private config-dir (System/getenv "CONFIG_DIR"))

(def ^:private config-params [["CONFIG_DIR" (or config-dir
                                                "")]
                              ["PORT" (or (System/getenv "PORT")
                                          3000)]])

(defn help []
  (println "Starting greenyet with config")
  (->> config-params
       (map (fn [[env-var value]]
              (format "  %s: '%s'" env-var value)))
       (map println)
       doall))

(defn handler [_]
  (let [host-list (read-host-list config-dir)
        status-url-config (read-status-url-config config-dir)
        host-list-with-status (map (fn [host] (status/with-status host status-url-config))
                                   host-list)]
    {:body (view/render host-list-with-status)}))
