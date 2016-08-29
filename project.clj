(defproject logbook "0.1.0-SNAPSHOT"
  :description "Release log"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/core.async "0.2.374"]
                 [reagent "0.6.0-rc"]
                 [cljs-http "0.1.41"]]

  :plugins [[lein-figwheel "0.5.6"]
            [lein-cljsbuild "1.1.3"]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "logbook.core/run"}
                :compiler {:main logbook.core
                           :asset-path "js"
                           :output-to "resources/public/js/main.js"
                           :output-dir "resources/public/js"
                           :source-map-timestamp true}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/main.js"
                           :main logbook.core
                           :optimizations :advanced
                           :pretty-print false}}]}
  :figwheel {})
