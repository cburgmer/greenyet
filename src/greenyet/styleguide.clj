(ns greenyet.styleguide
  (:require [clojure.string :as str]
            [greenyet.view :as view]
            [hiccup.core :refer [html]]))

(defn- in-template [html template]
  (str/replace template
               "<!-- BODY -->"
               html))

(defn- a-component-status [color name]
  {:color color
   :name (or name "a component")})

(defn- n-component-statuses [count-param color component-name]
  (let [count (if count-param
                (Integer/parseInt count-param)
                0)]
    (take count
          (repeat (a-component-status color component-name)))))

(defn- a-host-entry [{:keys [color message system package-version no-green-components no-yellow-components no-red-components component-name]}]
  (view/host-as-html {:status-url "/internal/status"
                      :system system}
                     {:color color
                      :package-version package-version
                      :message message
                      :components (seq (concat (n-component-statuses no-green-components :green component-name)
                                               (n-component-statuses no-yellow-components :yellow component-name)
                                               (n-component-statuses no-red-components :red component-name)))}))

(defn render [{:keys [no-hosts] :as params} template]
  (let [entry-count (if no-hosts
                      (Integer/parseInt no-hosts)
                      1)]
    (in-template (html [:table
                        [:colgroup.environments {:span entry-count}]
                        [:tbody
                         [:tr
                          (take entry-count
                                (repeat (a-host-entry params)))]]])
                 template)))
