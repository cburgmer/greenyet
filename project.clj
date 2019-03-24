(defproject greenyet "2.0.0"
  :description "Microservices status dashboard"
  :url "https://github.com/cburgmer/greenyet"
  :license {:name "BSD 2-Clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :plugins [[lein-ring "0.9.7"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.5.0-alpha.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jdmk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [org.clojure/core.async "0.2.374"]
                 [http-kit "2.3.0"]
                 [clj-time "0.15.1"]
                 [hiccup "1.0.5"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.8.1"]
                 [json-path "1.0.1"]]
  :profiles {:dev {:dependencies [[http-kit.fake "0.2.2"]
                                  [ring-mock "0.1.5"]]
                   :resource-paths ["resources" "test/resources"]
                   :jvm-opts ["-Dgreenyet.environment=development"]}}
  :ring {:handler greenyet.core/handler
         :init greenyet.core/init})
