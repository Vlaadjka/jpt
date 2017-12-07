(defproject jpt "0.1.0"
  :description "Job Progress Tracker"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot jpt.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
