(ns task2.core)

(defn primes
  []
  (letfn [(sieve [table n]
            (if-let [factors (get table n)]
              ;; n - составное число, обновляем таблицу
              (let [new-table (reduce (fn [t factor]
                                        (let [next-multiple (+ n factor)]
                                          (update t next-multiple conj factor)))
                                      (dissoc table n)
                                      factors)]
                (recur new-table (inc n)))
              ;; n - простое число
              (lazy-seq (cons n (sieve (assoc table (* n n) [n])
                                       (inc n))))))]
    (sieve {} 2)))

;; Вспомогательные функции
(defn take-primes [n]
  (take n (primes)))

(defn primes-up-to [limit]
  (take-while #(<= % limit) (primes)))

;; Демо функция
(defn demo []
  (println "=== Простые числа ===")
  (println "Первые 10:" (take 10 (primes)))
  (println "Простые до 50:" (primes-up-to 50))
  (println "20-е простое число:" (nth (primes) 19)))

(defn -main [& args]
  (demo))