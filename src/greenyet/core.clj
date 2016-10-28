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

(defn init []
  (try
    (poll/start-polling statuses (config/hosts-with-config) config/polling-interval-in-ms)
    (catch FileNotFoundException e
      (binding [*out* *err*]
        (println (.getMessage e))
        (println)
        (println config-help))
      (System/exit 1)))
  (println "Starting greenyet with config")
  (println (config/config-params-as-string)))

(def handler
  (handler/create statuses))
