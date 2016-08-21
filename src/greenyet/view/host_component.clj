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

(defn render [host status]
  [:li.patch.status-patch {:class (some-> status
                                          :color
                                          name
                                          h)}
   [:span.system [:a {:href (str/join ["?systems=" (h (:system host))])}
                  (h (:system host))]]
   [:a.environment {:href (str/join ["?environments=" (h (:environment host))])}
    (h (:environment host))]
   [:span.state (status-symbol status)]

   [:span.detail (h (or (message status) "No details to report"))
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
