(ns jpt.job-test
  (:require [jpt.job :refer :all]
            [jpt.core :refer [app]]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [ring.mock.request :as mock]))

(defn clear-job-db-fixture [f]
  (f)
  (reset! job-db {})
  (reset! job-id 1))

(use-fixtures :each clear-job-db-fixture)

(defn- parse-body
  [body]
  (if body
    (json/parse-string (slurp body) true)))

(defn- random-job-progress
  []
  (mod (System/currentTimeMillis) 1000))

(deftest test-get-jobs
  (testing "Testing GET request to /jobs"
    (testing "Should return empty list"
      (let [response (app (mock/request :get "/jobs"))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (count body) 0))))))

(deftest test-add-jobs
  (testing  "Testing POST request to /jobs/add-job"
    (testing "Submit new job"
      (let [response (app (-> (mock/request :post "/jobs/add-job")
                              (mock/content-type "application/json")
                              (mock/json-body {:total (random-job-progress)})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (> (:id body) 0))))

    (testing "Submit new job without total value"
      (let [response (app (-> (mock/request :post "/jobs/add-job")
                              (mock/content-type "application/json")
                              (mock/json-body {:size (random-job-progress)})))
            body (parse-body (:body response))]
        (is (= (:status response) 500))))))

(deftest stress-test-add-jobs
  (testing "Submit lots of new jobs and count result"
    (let [jobs-created
          (count
           (pmap #(app (-> (mock/request :post "/jobs/add-job")
                           (mock/content-type "application/json")
                           (mock/json-body {:total %})))
                 (range 10000)))
          response (app (mock/request :get "/jobs"))
          body (parse-body (:body response))]
      (is (= (:status response) 200))
      (is (= (count body) jobs-created)))))

(deftest test-get-job
  (testing "Testing POST request to /jobs"
    (testing "Should return null"
      (let [response (app (-> (mock/request :post "/jobs")
                              (mock/content-type "application/json")
                              (mock/json-body {:id 1})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= body nil))))
    (testing "Should return job by id"
      (let [job (add-new-job {:total 100})
            response (app (-> (mock/request :post "/jobs")
                              (mock/content-type "application/json")
                              (mock/json-body {:id (:id job)})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= body {:id (:id job)
                     :progress 0
                     :total 100}))))))

(deftest test-inc-progress
  (testing "Testing POST request to /jobs/inc-progress"
    (testing "Should return null progress"
      (let [response (app (-> (mock/request :post "/jobs/inc-progress")
                              (mock/content-type "application/json")
                              (mock/json-body {:id 1
                                               :value 10})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (:progress body) nil))))
    (testing "Increase progress"
      (let [job (add-new-job {:total 100})
            response (app (-> (mock/request :post "/jobs/inc-progress")
                              (mock/content-type "application/json")
                              (mock/json-body {:id (:id job)
                                               :value 10})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (:progress body) 10))))
    (testing "Progress should not be greater than total"
      (let [job (add-new-job {:total 100})
            response (app (-> (mock/request :post "/jobs/inc-progress")
                              (mock/content-type "application/json")
                              (mock/json-body {:id (:id job)
                                               :value 200})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (:progress body) 100))))))

(deftest stress-test-inc-progress
  (testing "Submit lots of concurent inc progress to single job"
    (let [job (add-new-job {:total 10000000})
          inc-count
          (count
           (pmap #(app (-> (mock/request :post "/jobs/inc-progress")
                          (mock/content-type "application/json")
                          (mock/json-body {:id (:id job)
                                           :value %})))
                 (take 1000 (iterate int 1))))
          response (app (-> (mock/request :post "/jobs")
                            (mock/content-type "application/json")
                            (mock/json-body {:id (:id job)})))
          body (parse-body (:body response))]
      (is (= (:status response) 200))
      (is (= body {:id (:id job)
                   :progress inc-count
                   :total (:total job)})))))

(deftest test-set-progress
  (testing "Testing POST request to /jobs/set-progress"
    (testing "Should return null progress"
      (let [response (app (-> (mock/request :post "/jobs/set-progress")
                              (mock/content-type "application/json")
                              (mock/json-body {:id 1
                                               :value 10})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (:progress body) nil))))
    (testing "Set progress"
      (let [job (add-new-job {:total 200})
            response (app (-> (mock/request :post "/jobs/set-progress")
                              (mock/content-type "application/json")
                              (mock/json-body {:id (:id job)
                                               :value 55})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (:progress body) 55))))
    (testing "Progress should not be greater than total"
      (let [job (add-new-job {:total 300})
            response (app (-> (mock/request :post "/jobs/inc-progress")
                              (mock/content-type "application/json")
                              (mock/json-body {:id (:id job)
                                               :value 700})))
            body (parse-body (:body response))]
        (is (= (:status response) 200))
        (is (= (:progress body) 300))))))

(deftest stress-test-set-progress
  (testing "Submit lots of concurent set progress to single job"
    (let [job (add-new-job {:total 510})
          inc-count
          (count
           (pmap #(app (-> (mock/request :post "/jobs/set-progress")
                           (mock/content-type "application/json")
                           (mock/json-body {:id (:id job)
                                            :value %})))
                 (take 1000 (iterate int 500))))
          response (app (-> (mock/request :post "/jobs")
                            (mock/content-type "application/json")
                            (mock/json-body {:id (:id job)})))
          body (parse-body (:body response))]
      (is (= (:status response) 200))
      (is (= body {:id (:id job)
                   :progress 500
                   :total (:total job)})))))
