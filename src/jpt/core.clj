(ns jpt.core
  (:gen-class)
  (:require [org.httpkit.server :as http]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.api.sweet :refer :all]
            [clojure.tools.logging :as log]
            [jpt.job :refer [job-routes]]))

(def environment (System/getenv "environment"))

(defn production
  []
  (= environment "production"))

(defn wrap-default-exception
  [^Exception e data request]
  (let [error (hash-map :error (.getMessage e)
                        :data (:error data))]
    (if (production)
      (log/error error))
    {:status 500
     :body error}))

(defapi app
  {:swagger
   {:ui "/"
    :spec "/swagger.json"
    :data {:basePath "/"
           :info {:title "Job Process Tracker"
                  :version "0.1.0"}
           :tags [{:name "Jobs"
                   :description "Sumbit new jobs, track progress"}]}
    :options {:ui {:validatorUrl nil}}}
   :exceptions {:handlers
                {:compojure.api.exception/default wrap-default-exception
                 :compojure.api.exception/request-parsing wrap-default-exception
                 :compojure.api.exception/request-validation
                 wrap-default-exception}}}
  (middleware [wrap-params
               [wrap-json-body {:keywords? true}]]
              (context "/jobs" []
                       :tags ["Jobs"]
                       job-routes)))

(defn -main
  []
  (http/run-server app {:port 3000}))
