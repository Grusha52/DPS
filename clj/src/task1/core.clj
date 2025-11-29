(ns task1.core)

(defn generate-strings [alphabet n]
  (if (<= n 0)
    [""]
    (reduce (fn [strings _]
              (mapcat (fn [s]
                        (let [last-char (when (seq s) (str (last s)))]
                          (->> alphabet
                               (remove #(= last-char %))
                               (map #(str s %)))))
                      strings))
            [""]
            (range n))))

(defn -main [& args]
  (println "Результат для алфавита [\"a\" \"b\" \"c\"] и N=2:")
  (println (generate-strings ["a" "b" "c"] 2))

  (println "\nТест для N=0:")
  (println (generate-strings ["a" "b" "c"] 0))

  (println "\nТест для N=1:")
  (println (generate-strings ["a" "b" "c"] 1)))