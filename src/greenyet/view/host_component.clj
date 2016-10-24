(ns greenyet.view.host-component
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [hiccup.core :refer [h]]))

(defn- message [{message :message}]
  (if (vector? message)
    (str/join ", " message)
    message))

(defn- status-symbol [{color :color}]
  [:span.symbol {:class (some-> color
                                name
                                h)}])

(def ^:private color-by-importance [:red :yellow :green])

(defn render [host status params]
  [:li.patch.status-patch {:id    (h (:system host))
                           :class (some-> status
                                          :color
                                          name
                                          h)
                           :onmouseenter  "storeHoverState(this)",
                           :onmouseleave  "removeHoverState(this)"}
   [:span.system [:a {:href (utils/link-select params "systems" (h (:system host)))}
                  (h (:system host))]]
   [:a.environment {:href (utils/link-select params "environments" (h (:environment host)))}
    (h (:environment host))]
   [:span.state (status-symbol status)]

   [:span.detail {:onscroll "storeScrollPosition(this)"}
    (h (or (message status) "No details to report"))
    (when (:components status)
      [:ol.joblist
       (for [comp (utils/prefer-order-of color-by-importance (:components status) :color)]
         [:li.job {:title (h (message comp))}

          (status-symbol comp)
          (h (:name comp))])])]

   (when (:package-version status)
     [:span.meta-data (h (:package-version status))])
   [:span.url [:a {:href (h (:status-url host))}
               (h (:hostname host))]]])
