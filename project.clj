(defproject ag-csv-to-db "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/tools.cli "0.3.3"]]

  :main ag-csv-to-db.core
  :aot [ag-csv-to-db.core])
