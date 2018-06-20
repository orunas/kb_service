(defproject kb-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [http-kit "2.2.0"]
                 [ring/ring-devel "1.2.1"]
                 [ring/ring-core "1.2.1"]
                 [compojure "1.1.5"]
                 [org.apache.jena/jena-core "3.6.0"]
                 [org.apache.jena/jena-arq "3.6.0"]
                 [org.apache.jena/jena-tdb "3.6.0"]
                 [org.apache.jena/jena-iri "3.6.0"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-json "0.4.0"]
                 ]
  :uberjar-name "kb-service-standalone.jar"
  :main kb-service.core
  :aot [kb-service.core]
  )
