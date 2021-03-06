(ns kb-service.core
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.reload :as reload]
            [clojure.pprint]
            [compojure.route :as cr]
            [compojure.core :as cc]
            [org.httpkit.client :as http]
            [kb-service.airport-data :as air]
            [kb-service.jena :as j]
            [clojure.data.json :as json]
            [kb-service.event :as e]
            [environ.core :refer [env]]

    ;[compojure.handler :as ch]
            )
  (import [org.apache.jena.rdf.model Model ModelFactory ResourceFactory]
          [org.apache.jena.query Query QueryFactory QueryExecutionFactory]
          [java.io ByteArrayInputStream StringWriter] ))

;(defonce request (atom nil))
; it should be list of string
(defonce listener (atom nil))

(def config {                                               ;:url-kb "http://localhost:3030/Test2/data"
             :url-kb "http://ec2-35-178-111-83.eu-west-2.compute.amazonaws.com:10035/repositories/Flights/statements"
             ;:kb-authorization nil
             :kb-authorization "Basic ZWE6am9wbDE="
             })



(defonce tst-val (atom {}))

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

(def content-types [["text/turtle" "text/turtle" "TTL"] ["application/ld.json" "application/ld+json" "JSON-LD"]])

(defn parse-content-type
  [content-type-str]
  (->
    (some #(if (re-seq (re-pattern (% 0)) content-type-str) (% 2)) content-types)
    ))

(defn get-format-from-request [req]
  (parse-content-type ((req :headers) "content-type"))
  )

(defn load-request [req]
  (let [t (get-format-from-request req)]
    (j/read-stream-and-output-model (req :body) t "TTL" )
    ))



(defn perceive-data [req]
  ;(reset! tst-val req)
  (let [out-body (load-request req)
        options {:headers {"Content-Type"  "text/turtle; charset=utf-8"
                           "Authorization" (config :kb-authorization)}
                 :body    out-body}]
    ;(clojure.pprint/pprint options)
    (let [{:keys [status headers body error] :as resp} @(http/post (config :url-kb) options)]
      ;(clojure.pprint/pprint resp)
      (if (= status 200)
        (->> (j/load-and-eval-jena-model out-body @listener #(e/push-event out-body))
             (println "Added:")
             ))
      {:status  status
       :headers {"content-type"     (headers :content-type)
                 ;"content-encoding" (headers :content-encoding)
                 }
       :body    body})))

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

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 8087))]
    (println "starting on port:" port)
    ; (reset! airport-data (air/load-flights))
    ;; The #' is useful when you want to hot-reload code
    ;; You may want to take a look: https://github.com/clojure/tools.namespace
    ;; and http://http-kit.org/migration.html#reload

    (let [handler (reload/wrap-reload #'all-routes)]
      ;(run-server handler {:port port})
      (reset! server (run-server handler {:port port}))
      )))


(comment
  (use '[kb-service.core])
  (-main))
;(run-server app {:port 8087})




