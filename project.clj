(defproject era "0.2.0-SNAPSHOT"
  :description "Cross-platform time tools"
  :url "https://github.com/chbrown/era-clojure"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles {:test {:plugins [[lein-cloverage "1.0.9"]]}})
