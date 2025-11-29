(defproject pfilter "0.1.0-SNAPSHOT"
  :description "Parallel lazy filter using futures"
  :url "https://example.com"
  :license {:name "EPL-2.0"}

  :dependencies [[org.clojure/clojure "1.11.1"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :main pfilter.core

  :profiles {:dev {:global-vars {*warn-on-reflection* true}}})