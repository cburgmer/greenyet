(ns greenyet.view.styleguide
  (:require [clojure.string :as str]
            [greenyet.view.host-component :as host-component]
            [hiccup.core :refer [html]]))

(defn- in-template [html template]
  (str/replace template
               "<!-- BODY -->"
               html))

(defn- a-component-status [color name]
  {:color color
   :name (or name "Component")})

(defn- n-component-statuses [count-param color component-name]
  (let [count (if count-param
                (Integer/parseInt count-param)
                0)]
    (take count
          (repeat (a-component-status color component-name)))))

(defn- a-host-entry [{:keys [environment color message system package-version no-green-components no-yellow-components no-red-components component-name]}]
  (host-component/render {:status-url "/internal/status"
                          :hostname "greenyet-prod.net:8080"
                          :system system
                          :environment environment}
                         {:color (keyword color)
                          :package-version package-version
                          :message message
                          :components (seq (concat (n-component-statuses no-green-components :green component-name)
                                                   (n-component-statuses no-yellow-components :yellow component-name)
                                                   (n-component-statuses no-red-components :red component-name)))}))

(defn- a-cell [{:keys [color other-machine-color] :as params}]
  (if other-machine-color
    [(a-host-entry params) (a-host-entry (assoc params :color other-machine-color))]
    [(a-host-entry params)]))

(defn render [{:keys [no-hosts] :as params} template]
  (let [entry-count (if no-hosts
                      (Integer/parseInt no-hosts)
                      1)]
    (in-template (html [:div.environment-wrapper
                        [:ol.patchwork
                         (take entry-count
                               (map (fn [h] (into [:td] h))
                                    (repeat (a-cell params))))]])
                 template)))
