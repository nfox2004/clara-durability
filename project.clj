(defproject clara-durability-help "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}


  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [com.cerner/clara-rules "0.15.2"]
                 [org.clojure/data.fressian "0.2.1"]]


  :main ^:skip-aot clara-durability-help.core

  :source-paths ["src/clj"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
