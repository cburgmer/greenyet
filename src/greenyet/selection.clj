(ns greenyet.selection
  (:require [clojure.string :as str]))

(defn- filter-by-systems [host-status-pairs selected-systems]
  (let [systems (set (map str/lower-case selected-systems))]
    (filter (fn [[{system :system} _]]
              (contains? systems (str/lower-case system)))
            host-status-pairs)))

(defn- filter-by-environments [host-status-pairs selected-environments]
  (let [environments (set (map str/lower-case selected-environments))]
    (filter (fn [[{environment :environment} _]]
              (contains? environments (str/lower-case environment)))
            host-status-pairs)))

(defn filter-hosts [host-status-pairs selected-systems selected-environments]
  (cond-> host-status-pairs
    selected-systems (filter-by-systems selected-systems)
    selected-environments (filter-by-environments selected-environments)))
