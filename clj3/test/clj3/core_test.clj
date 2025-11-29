(ns clj3.core_test
  (:require [clojure.test :refer :all]
            [clj3.core :refer :all]))

(deftest test-pfilter-finite
  (is (= (filter even? (range 20)) (doall (pfilter even? (range 20))))))

(deftest test-pfilter-lazy-infinite
  (let [inf (range)
        pf (pfilter #(= 0 (mod % 3)) inf {:block-size 7 :prefetch 3})]
    (is (= (take 10 (filter #(= 0 (mod % 3)) (range))) (take 10 pf)))))

(deftest test-pfilter-preserves-order
  (let [xs (range 50)
        pf (pfilter odd? xs {:block-size 5 :prefetch 4})]
    (is (= (filter odd? xs) (doall pf)))))

(deftest test-performance-parallel-vs-seq
  (let [n 80
        delay-ms 30
        xs (range n)
        pred (slow-pred delay-ms)
        [_ seq-ms] (measure-ms #(doall (filter pred xs)))
        params {:block-size 10 :prefetch 4}
        [_ par-ms] (measure-ms #(doall (pfilter pred xs params)))]
    (println "Sequential ms:" seq-ms "Parallel ms:" par-ms)
    (is (< par-ms seq-ms))))