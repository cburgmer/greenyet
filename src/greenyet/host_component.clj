(ns greenyet.host-component
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [hiccup.core :refer [h]]))

(defn- message [{message :message}]
  (if (vector? message)
    (str/join ", " message)
    message))

(def ^:private color-by-importance [:red :yellow :green])

(defn render [host status]
  [:div {:class (str/join " " ["host" (some-> status
                                              :color
                                              name
                                              h)])}
   [:a.host-name {:href (h (:status-url host))}
    (h (:hostname host))]
   (when (:package-version status)
     (h (:package-version status)))
   (when (:message status)
     [:span.message (h (message status))])
   (when (:components status)
     (let [components-id (h (str/join ["components-" (:index host)]))]
       [:a.show-components {:href (str/join ["#" components-id]) }
        [:ol.components {:id components-id}
         (for [comp (utils/prefer-order-of color-by-importance (:components status) :color)]
           [:li {:class (str/join " " ["component" (some-> comp
                                                           :color
                                                           name
                                                           h)])
                 :title (h (message comp))
                 :data-name (h (:name comp))}
            (h (:name comp))])
         [:li.more]]]))])
