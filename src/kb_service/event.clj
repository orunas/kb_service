(defonce events (atom clojure.lang.PersistentQueue/EMPTY))

(ns kb-service.event)

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

(comment
  '["<http://travelplanning.ex/Event/Event20180625115301123>"
    "<http://travelplanning.ex/Event/date>" {}
    "<http://travelplanning.ex/Event/body>" ttl-as-string-or-graph-uri
    :a "<http://travelplanning.ex/EventType>"
    :e/processed false])