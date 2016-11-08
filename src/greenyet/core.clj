(ns greenyet.core
  (:require [clojure.string :as str]
            [greenyet
             [config :as config]
             [handler :as handler]
             [poll :as poll]])
  (:import java.io.FileNotFoundException))

(def statuses (atom (poll/empty-statuses)))

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

(defn- read-config []
  (try
    (config/hosts-with-config)
    (catch FileNotFoundException e
      (binding [*out* *err*]
        (println (.getMessage e))
        (println)
        (println config-help))
      (System/exit 1))))

(defn init []
  (let [[host-entries errors] (read-config)]
    (if (empty? errors)
      (poll/start-polling statuses host-entries config/polling-interval-in-ms)
      (do
        (binding [*out* *err*]
          (println "Found the following issues. Please assist.")
          (println (str/join "\n" errors)))
        (System/exit 1))))
  (println "Starting greenyet with config")
  (println (config/config-params-as-string)))

(def handler
  (handler/create statuses))
