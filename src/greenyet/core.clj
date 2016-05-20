(ns greenyet.core
  (:require [clj-time.core :as tc]
            [clj-yaml.core :as yaml]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.java.io :as io]
            [greenyet
             [status :as status]
             [view :as view]]
            [ring.middleware.not-modified :as not-modified]
            [ring.util
             [response :refer [charset content-type header response]]
             [time :refer [format-date]]]))

(import java.io.FileNotFoundException)

(def ^:private hosts-with-status (atom [{} (tc/now)]))

(defn- update-status [[status-map last-changed] key new-status]
  (let [old-status (get status-map key)]
    (if (= new-status old-status)
      [status-map last-changed]
      [(assoc status-map key new-status) (tc/now)])))

(def ^:private config-dir (System/getenv "CONFIG_DIR"))

(def ^:private timeout-in-ms (or (some-> (System/getenv "TIMEOUT")
                                         Integer/parseInt)
                                 5000))

(def ^:private config-params [["CONFIG_DIR" (or config-dir
                                                "")]
                              ["TIMEOUT" timeout-in-ms]
                              ["PORT" (or (System/getenv "PORT")
                                          3000)]])

(defn- poll-status [host status-url-config]
  (go-loop []
    (let [status (status/with-status host status-url-config)]
      (swap! hosts-with-status update-status host status)
      (<! (timeout timeout-in-ms))
      (recur))))


(defn- read-host-list [config-dir]
  (let [host-list-file (io/file config-dir "hosts.yaml")]
    (yaml/parse-string (slurp host-list-file))))

(defn- read-status-url-config [config-dir]
  (let [config-file (io/file config-dir "status_url.yaml")]
    (yaml/parse-string (slurp config-file))))

(defn- start-polling []
  (let [host-list (read-host-list config-dir)
        status-url-config (read-status-url-config config-dir)]
    (doall (for [host host-list]
             (poll-status host status-url-config)))))


(def ^:private page-template (-> "index.template.html" io/resource io/file slurp))

(def ^:private environment-names (-> "environment_names.yaml" io/resource io/file slurp yaml/parse-string))

(defn- render [_]
  (let [[host-with-statuses last-changed] @hosts-with-status]
    (-> (response (view/render (vals host-with-statuses) page-template environment-names))
        (content-type "text/html")
        (header "Last-Modified" (format-date (.toDate last-changed)))
        (charset "UTF-8"))))


(defn init []
  (println "Starting greenyet with config")
  (->> config-params
       (map (fn [[env-var value]]
              (format "  %s: '%s'" env-var value)))
       (map println)
       doall)
  (try
    (start-polling)
    (catch FileNotFoundException e
      (binding [*out* *err*]
        (println (.getMessage e)))
      (System/exit 1))))

(def handler
  (-> render
      not-modified/wrap-not-modified))
