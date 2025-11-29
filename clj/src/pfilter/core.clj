(ns pfilter.core)

;; Parallel, lazy filter using futures. Each future processes a block of elements.
;; Options: :block-size (default 100), :prefetch (how many futures to start ahead, default 2)

(defn pfilter
  "Returns a lazy sequence of items from coll for which (pred item) is true.
   Work is performed in blocks of size `block-size` inside futures. To enable
   overlap between blocks create `prefetch` futures ahead of time (default 2).

   Usage: (pfilter pred coll) or (pfilter pred coll {:block-size 50 :prefetch 4})"
  ([pred coll] (pfilter pred coll {:block-size 100 :prefetch 2}))
  ([pred coll {:keys [block-size prefetch] :or {block-size 100 prefetch 2}}]
   (let [blocks (partition-all block-size coll)
         futures-seq (map (fn [blk] (future (doall (filter pred blk)))) blocks)
         futures-seq (concat (doall (take prefetch futures-seq)) (drop prefetch futures-seq))]
     (mapcat deref futures-seq))))

;; ----------------------------
;; Performance helpers
;; ----------------------------

(defn measure-ms
  "Return [result ms] where result is (f) and ms is elapsed milliseconds."
  [f]
  (let [t0 (System/nanoTime)
        r  (f)
        t1 (System/nanoTime)]
    [r (quot (- t1 t0) 1000000)]))

(defn slow-pred
  "A predicate that simulates expensive work by sleeping `ms` milliseconds per call."
  [ms]
  (fn [x]
    (Thread/sleep ms)
    (even? x)))

;; ----------------------------
;; CLI entrypoint
;; ----------------------------

(defn -main [& _]
  (println "Parallel filter demo:")
  (let [n 80
        delay-ms 30
        xs (range n)
        pred (slow-pred delay-ms)
        [_ seq-ms] (measure-ms #(doall (filter pred xs)))
        params {:block-size 10 :prefetch 4}
        [_ par-ms] (measure-ms #(doall (pfilter pred xs params)))]
    (println "Sequential:" seq-ms "ms")
    (println "Parallel  :" par-ms "ms with params" params)))