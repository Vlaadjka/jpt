(ns jpt.job-test
  (:require [jpt.job :refer :all]
            [jpt.core :refer [app]]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [ring.mock.request :as mock]))

(defn clear-job-db-fixture [f]
  (f)
  (reset! job-db [])
  (reset! job-id 1))

(use-fixtures :each clear-job-db-fixture)

(defn- parse-body
  [body]
  (json/parse-string (slurp body) true))

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
