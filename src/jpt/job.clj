(ns jpt.job
  (:gen-class)
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]))

(def job-expire-period-seconds 60)
(def job-id (atom 1))
(def job-db (atom {}))

(defn- current-unix-timestamp
  "Current unix timestamp (seconds)"
  []
  (quot (System/currentTimeMillis) 1000))

(defn- unexpired-jobs
  "Returns jobs which are created/updated less that expire-period-seconds ago"
  [jobs expire-period-seconds]
  (let [timestamp (current-unix-timestamp)
        jobs (into {}
                   (filter #(<= (- timestamp (:updated-at (val %)))
                                expire-period-seconds)
                           jobs))]
    (if (empty? jobs)
      {}
      jobs)))

(defn get-jobs
  "Removes all expired jobs from db and returns new list"
  []
  (swap! job-db unexpired-jobs job-expire-period-seconds))

(defn add-new-job
  "Adds new job to db with 0 progress and returns new jobs map"
  [{total :total}]
  (let [id (swap! job-id inc)
        job {:id id
             :total total
             :progress 0
             :updated-at (current-unix-timestamp)}]
    (swap! job-db assoc (keyword (str id)) job)
    job))

(defn get-job
  "Return single job by id"
  [id]
  ((keyword (str id)) (get-jobs)))

(defn inc-progress
  "Increases job progress"
  [{:keys [id value]}]
  ((keyword (str id))
   (swap! job-db
          (fn [all-jobs id value]
            (let [jobs (unexpired-jobs
                        all-jobs
                        job-expire-period-seconds)]
              (if-let [job ((keyword (str id)) jobs)]
                (let [new-progress (+ (:progress job) value)
                      job {:id id
                           :total (:total job)
                           :progress (if (<= new-progress (:total job))
                                       new-progress
                                       (:total job))
                           :updated-at (current-unix-timestamp)}]
                  (assoc jobs (keyword (str id)) job))
                jobs)) )
          id
          value)))

(defn set-progress
  "Set job progress"
  [{:keys [id value]}]
  ((keyword (str id))
   (swap! job-db
          (fn [all-jobs id value]
            (let [jobs (unexpired-jobs
                        all-jobs
                        job-expire-period-seconds)]
              (if-let [job ((keyword (str id)) jobs)]
                (let [job {:id id
                           :total (:total job)
                           :progress (if (<= value (:total job))
                                       value
                                       (:total job))
                           :updated-at (current-unix-timestamp)}]
                  (assoc jobs (keyword (str id)) job))
                jobs)) )
          id
          value)))

(defroutes job-routes
  (GET "/" []
       (ok (map
            #(dissoc (val %) :updated-at)
            (get-jobs))))
  (POST "/" []
        :body [body {:id Long}]
        (ok (dissoc (get-job (:id body)) :updated-at)))
  (POST "/add-job" []
        :body [body {:total Long}]
        (ok {:id (:id (add-new-job body))}))
  (POST "/inc-progress" []
        :body [body {:id Long
                     :value Long}]
        (ok {:progress (:progress (inc-progress body))}))
  (POST "/set-progress" []
        :body [body {:id Long
                     :value Long}]
        (ok {:progress (:progress (set-progress body))})))
