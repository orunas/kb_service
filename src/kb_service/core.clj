(ns kb-service.core
  (:gen-class)
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.reload :as reload]
            [compojure.route :as cr]
            [compojure.core :as cc]
            [org.httpkit.client :as http]
            [kb-service.airport-data :as air]
            [clojure.data.json :as json]
    ;[compojure.handler :as ch]
            )
  (import [org.apache.jena.rdf.model Model ModelFactory ResourceFactory]
          [org.apache.jena.query Query QueryFactory QueryExecutionFactory]
          [java.io ByteArrayInputStream StringWriter] ))

;(defonce request (atom nil))
; it should be list of string
(defonce listener (atom nil))

(defonce events (atom clojure.lang.PersistentQueue/EMPTY))

(comment defn load-jena-model-and-out
  ([data-input] (load-jena-model-and-out data-input @listener))
  ([data-input query]
   (with-open [sr (StringWriter.)]
     (let*
       [m (ModelFactory/createDefaultModel)]
       (.read m data-input nil "TTL")
       (.write m sr "TTL")
       (query-model m query))
     ;(doto        (ModelFactory/createDefaultModel)        (.read data-input nil "TTL")        (.write sr "TTL")        (query-model query))
     (.toString sr))))

(comment defn load-jena-model-and-out2
  [data-input query]
  (let*
   [sr (StringWriter.)]
   (try
     (do
       (let*
         [model (ModelFactory/createDefaultModel)]
         (.read model data-input nil "TTL")
         (query-model model query)
         (.write model sr "TTL")
         (.toString sr))
       )
     (finally (.close sr))
     )))

; Query query = QueryFactory.create(queryString) ;
;QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
;boolean result = qexec.execAsk() ;

(comment defn load-jena-model-from-str-and-out
  [data-input query]
  (load-jena-model-and-out2 (ByteArrayInputStream. (.getBytes data-input)) query))

(defn get-subscribers
  ([] @listener))

(defn get-subscribers-w [request]
  {:status 200
   :body  (get-subscribers)})

(defn post-subscribers [req]
  (reset! listener (slurp (req :body))))

(defn post-subscribers-w [req]
  (do
    (post-subscribers req)
    {:status 200
     :body (get-subscribers)}))

(defn get-events []
  (seq @events))

(defn get-events-w [request]
  {:status 200
   :body   (str (get-events))})

(defn peek-event []
  (if-let [val (peek @events)]
    (do
      (swap! events pop)
      val)))

(defn peek-event-w [request]
  {:status 200
   :headers {"Context-Type" "text/turtle; charset=utf-8"}
   :body (peek-event)})

(defmulti push-event class)

(defmethod push-event java.io.InputStream [req]
  (if-let [b (req :body)]
    (push-event (slurp b))))
(defmethod  push-event String [req]
  (swap! events conj req))

(defn query-model
  [model query]
  (if (not (nil? query))
    (let* [qex (QueryExecutionFactory/create (QueryFactory/create query) model)]
      (try
        (.execAsk qex)
        (finally (.close qex))))
    false))

(defn load-and-eval-jena-model [data-input query]
  (let*
    [m (ModelFactory/createDefaultModel)]
    (.read m (ByteArrayInputStream. (.getBytes data-input)) nil "TTL")
    (if (query-model m query)
      (push-event data-input)
      nil  )))

(defn perceive-data [req]
  ;(reset! request req)
  ;  (println "Request received")
  ;(print req)
  ;(println "parsed body")
  (let [b (slurp (req :body) )
        options {:headers {"Content-Type" "text/turtle; charset=utf-8"}
                 :body b}
        url "http://localhost:3030/Test2/data"
        {:keys [status headers body error] :as resp} @(http/post url options)
        ]
    (if (= status 200)
      (println "Added:" (load-and-eval-jena-model b @listener)))
    {:status (if status status 500)
     :headers {"Context-Type" "text/turtle; charset=utf-8"}}
    ;(assoc resp :status 200)
 ;{:status  status :headers {"Content-Type" "text/turtle; charset=utf-8"}      :body    res}
    ))

(defonce airport-data (atom {}))

(defn get-airport [id]
  ;(println "id:" id)
  {:status  200
   :headers {"Context-Type" "application/json"}
   :body (json/write-str ((keyword id) @airport-data))})

(cc/defroutes all-routes
              (cc/GET "/" [] "<h1>Hello World</h1>")
              (cc/GET "/airport/:id" [id] (get-airport id))
              (cc/POST "/data" [req] perceive-data)
              (cc/GET "/subscription" [] get-subscribers-w)
              (cc/POST "/subscription" [req] post-subscribers)
              (cc/GET "/queue/events" [] get-events-w)
              (cc/POST "/queue/events/peek" [req] peek-event-w)
              (cc/POST "/queue/events/push" [req] push-event))


(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  (println "starting")
  (reset! airport-data (air/load-flights))
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload

  (let [handler (reload/wrap-reload #'all-routes)]
    (reset! server (run-server handler {:port 8087}))))


(comment
  (use '[kb-service.core])
  (-main))
;(run-server app {:port 8087})




