(ns greenyet.view.host-component
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [hiccup.core :refer [h]]))

(defn- message [{message :message}]
  (if (vector? message)
    (str/join ", " message)
    message))

(def ^:private color-by-importance [:red :yellow :green])

(defn render [host status]
  [:li.patch.status-patch {:class (str/join " " [(some-> status
                                                         :color
                                                         name
                                                         h)])}
   [:span.system [:a {:href (str/join ["?systems=" (h (:system host))])}
                  (h (:system host))]]
   [:span.environment (h (:environment host))]
   [:span.state [:span {:class (h (case
                                      (some-> status
                                              :color
                                              name
                                              h)
                                    "green" "fa fa-check fa-5x"
                                    "red"   "fa fa-times fa-5x"
                                    "fa fa-minus fa-5x"
                                    ))}]]

   [:span.detail (h (or (message status) "No details to report"))
    (when (:components status)
      (let [components-id (h (str/join ["components-" (:index host)]))]

        [:ol.joblist {:id components-id }
         (for [comp (utils/prefer-order-of color-by-importance (:components status) :color)]
           [:li.job {:class (str/join " " [(some-> comp
                                                   :color
                                                   name
                                                   h)])
                     :title (h (message comp))
                     :data-name (h (:name comp))}

            [:span.jobstatus {:class (case
                                         (some-> comp :color name h)
                                       "green" "fa fa-check"
                                       "red"   "fa fa-times"
                                       "fa fa-minus"
                                       )}]
            (h (:name comp))])
         ]))
    ]

   (when (:package-version status)
     [:span.meta-data (h (:package-version status))])
   [:span.url [:a {:href (h (:status-url host))}
               (h (:hostname host))]]


   ])
