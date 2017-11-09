(defproject era "0.2.3"
  :description "Cross-platform time tools"
  :url "https://github.com/chbrown/era-clojure"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]]
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds [{:source-paths ["src"]
                        :compiler {:optimizations :simple
                                   :pretty-print false}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-dir "target/test"
                                   :output-to "target/test/main.js"
                                   :main era.runner
                                   :optimizations :whitespace}}]}
  :profiles {:test {:doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}
                    :plugins [[lein-doo "0.1.8"]
                              [lein-cloverage "1.0.10"]]}})
