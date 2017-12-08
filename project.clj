(defproject jpt "0.1.0"
  :description "Job Progress Tracker"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [ring/ring-core "1.6.0"]
                 [ring/ring-json "0.1.2"]
                 [ring/ring-mock "0.3.2"]
                 [metosin/compojure-api "1.1.11"]
                 [cheshire "5.8.0"]
                 [http-kit "2.2.0"]]
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler jpt.core/app}
  :main ^:skip-aot jpt.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[jonase/eastwood "0.2.4"]
                             [lein-bikeshed "0.4.0"]]}})
