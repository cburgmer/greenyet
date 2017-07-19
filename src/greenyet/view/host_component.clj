(ns greenyet.view.host-component
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [hiccup.core :refer [h]]))

(def ^:private color-by-importance [:red :yellow :green])

(defn- message [{message :message}]
  (if (vector? message)
    (str/join ", " message)
    message))

(defn- status-symbol [{color :color}]
  [:span.symbol {:class (some-> color
                                name
                                h)}])

(defn- render-details [host status params]
  [:span.detail {:onscroll "storeScrollPosition(this)"}
    (h (or (message status) "No details to report"))
    (when (:components status)
      [:ol.joblist
       (for [comp (utils/prefer-order-of color-by-importance (:components status) :color)]
         [:li.job {:title (h (message comp))}
          (status-symbol comp)
          (h (:name comp))])])])

(defn- render-collapsed-hosts [host status params]
  [:span.detail-static {:onscroll "storeScrollPosition(this)"}
    [:ol.hostlist
       (for [comp (:collapsed-hosts status)]
         [:li.host
            [:div.meta-data (h (:package-version comp))]
            [:div.url [:a {:href (h (:status-url comp))}
               (h (:hostname comp))]]
          ])]])

(defn render [host status params]
  [:li.patch.status-patch {:id    (h (hash host))
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
  
   (if (:collapsed-hosts status)
    (render-collapsed-hosts host status params)
    [:span.state (status-symbol status)])

   (when-not (:collapsed-hosts status)
    (render-details host status params))

   (when (:collapsed-hosts status)
    [:span.collapsed (h "COLLAPSED")])

   (when-not (:collapsed-hosts status)
    [:span.meta-data (h (:package-version status))])

   (when-not (:collapsed-hosts status)
     [:span.url [:a {:href (h (:status-url host))}
               (h (:hostname host))]])])
    
    
               
