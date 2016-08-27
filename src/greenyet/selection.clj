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

(defn- remove-green-systems [host-status-pairs]
  (->> host-status-pairs
       (group-by (fn [[{system :system} _]] system))
       (map second)
       (remove (fn [hosts]
                 (every? (fn [[_ {color :color}]] (= :green color))
                         hosts)))
       (apply concat)))

(defn filter-hosts [host-status-pairs selected-systems selected-environments hide-green]
  (cond-> host-status-pairs
    selected-systems (filter-by-systems selected-systems)
    selected-environments (filter-by-environments selected-environments)
    hide-green remove-green-systems))
