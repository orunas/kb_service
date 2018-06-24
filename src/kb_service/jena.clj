(ns kb-service.jena
  (:require [clojure.data.json :as json])
  (:import [org.apache.jena.rdf.model Model ModelFactory ResourceFactory]
           [org.apache.jena.query Query QueryFactory QueryExecutionFactory ResultSetFormatter]
           [java.io ByteArrayInputStream ByteArrayOutputStream StringWriter]
           ))

(defn query-model
  [model query]
  (if (not (nil? query))
    (let* [qex (QueryExecutionFactory/create (QueryFactory/create query) model)]
      (try
        (.execAsk qex)
        (finally (.close qex))))
    false))

(defn load-and-eval-jena-model [data-input query f]
  (let*
    [m (ModelFactory/createDefaultModel)]
    (.read m (ByteArrayInputStream. (.getBytes data-input)) nil "TTL")
    (if (query-model m query)
      (f)
      nil  )))

; "JSON-LD", "TTL"
;(j/read-and-output-model (json/write-str tv1) "JSON-LD" "TTL")

(defn read-stream-and-output-model
  [data-input-stream input-format output-format]
  (let*
    [m (ModelFactory/createDefaultModel)
     sr (StringWriter. )]
    ; (println "input-format" input-format)
    (.read m data-input-stream nil input-format)
    (.write m sr output-format)
    (.toString sr)))

(defn read-and-output-model
  [data-input-str input-format output-format]
  (read-stream-and-output-model (ByteArrayInputStream. (.getBytes data-input-str)) input-format output-format))



