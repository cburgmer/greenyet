(ns greenyet.view.table-test
  (:require [clojure
             [string :as str]
             [test :refer :all]]
            [greenyet.view.table :as sut]))

(defn a-host-entry
  ([hostname]
   {:system "my system"
    :hostname hostname
    :environment "production"})
  ([hostname idx]
   (assoc (a-host-entry hostname)
          :index idx)))

(defn a-status
  ([]
   {:color :green})
  ([color]
   {:color color}))

(deftest test-render
  (testing "does not interpret html as regex"
    (is (str/includes? (sut/render {(a-host-entry "host") {:status :green :message "$"}}
                                   nil
                                   "<!-- BODY -->"
                                   [])
                       "my system")))

  (testing "orders the hosts by index"
    (is (re-matches #".*first_host.*second_host.*last_host.*"
                    (sut/render {(a-host-entry "last_host" 42) (a-status :green)
                                 (a-host-entry "first_host" 0) (a-status :green)
                                 (a-host-entry "second_host" 10) (a-status :yellow)}
                                nil
                                "<!-- BODY -->"
                                []))))

  ; (testing "shows only first host if all are green for the given environment and system"
  ;   (let [html (sut/render {(a-host-entry "last_host" 42) (a-status)
  ;                           (a-host-entry "first_host" 0) (a-status)
  ;                           (a-host-entry "second_host" 10) (a-status)}
  ;                          nil
  ;                          "<!-- BODY -->"
  ;                          [])]
  ;     (is (= 1
  ;            (count (re-seq #"class=\"host " html))))
  ;     (is (re-matches #".*first_host.*"
  ;                     html))))
)
