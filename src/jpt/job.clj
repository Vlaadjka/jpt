(ns jpt.job
  (:gen-class)
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(def job-expire-period-seconds 60)
(def job-id (atom 1))
(def job-db (atom []))

(defn- current-unix-timestamp
  "Current unix timestamp (seconds)"
  []
  (quot (System/currentTimeMillis) 1000))

(defn- unexpired-jobs
  "Returns unexpired jobs which are created/updated less that expire-period-seconds ago"
  [jobs expire-period-seconds]
  (let [timestamp (current-unix-timestamp)]
    (filter #(<= (- timestamp (get % :updated-at))
                 expire-period-seconds)
            jobs)))

(defn get-jobs
  "Removes all expired jobs from db and returns new list"
  []
  (map
   #(dissoc % :updated-at)
   (swap! job-db unexpired-jobs job-expire-period-seconds)))

(defn add-new-job
  "Adds new job to db with 0 progress and returns new jobs map"
  [{total :total}]
  (let [job {:id (swap! job-id inc)
             :total total
             :progress 0
             :updated-at (current-unix-timestamp)}]
    (swap! job-db conj job)
    job))

(defroutes job-routes
  (GET "/" []
       (ok (get-jobs)))
  (POST "/add-job" []
        :body [body {:total Long}]
        (ok {:id (:id (add-new-job body))})))
