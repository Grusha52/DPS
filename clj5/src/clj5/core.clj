(ns clj5.core)

(defn make-forks [n]
  (vec (repeatedly n #(ref {:uses 0 :busy? false}))))

(defn philosopher
  [id forks think-ms eat-ms meals restarts-counter]
  (let [n     (count forks)
        left  (forks id)
        right (forks (mod (inc id) n))]
    (dotimes [_ meals]
      (Thread/sleep think-ms)
      (loop []
        (when-not (dosync
                    (if (or (:busy? @left) (:busy? @right))
                      false
                      (do
                        (alter left #(-> % (assoc :busy? true) (update :uses inc)))
                        (alter right #(-> % (assoc :busy? true) (update :uses inc)))
                        true)))
          (swap! restarts-counter inc)
          (recur)))
      (Thread/sleep eat-ms)
      (dosync
        (alter left assoc :busy? false)
        (alter right assoc :busy? false)))))

(defn run
  [{:keys [philosophers think-ms eat-ms meals]}]
  (let [forks (make-forks philosophers)
        restarts (atom 0)
        start (System/nanoTime)
        threads (doall
                  (map #(future
                          (philosopher %
                                       forks
                                       think-ms
                                       eat-ms
                                       meals
                                       restarts))
                       (range philosophers)))]
    (doseq [t threads] @t)
    (let [end (System/nanoTime)]
      {:time-ms (/ (- end start) 1e6)
       :tx-restarts @restarts
       :fork-usage (mapv #(-> @% :uses) forks)})))