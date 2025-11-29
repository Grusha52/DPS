(ns task2.core-test
  (:require [clojure.test :refer :all]
            [task2.core :refer :all]))

(deftest primes-test
  (testing "Базовые случаи"
    (is (= [2 3 5 7 11 13 17 19 23 29] (take 10 (primes))))
    (is (= 541 (nth (primes) 99))) ; 100-е простое число
    (is (= [] (take 0 (primes))))))

(deftest take-primes-test
  (testing "Функция take-primes"
    (is (= [2 3 5] (take-primes 3)))
    (is (= [] (take-primes 0)))))

(deftest primes-up-to-test
  (testing "Функция primes-up-to"
    (is (= [2 3 5 7] (primes-up-to 10)))
    (is (= [2] (primes-up-to 2)))
    (is (= [] (primes-up-to 1)))))