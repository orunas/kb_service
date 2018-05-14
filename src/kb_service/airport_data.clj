(ns kb-service.airport-data
  (:require                                                 ;[clojure.data.csv :as csv]
            [clojure-csv.core :as csv]
            [clojure.java.io :as io]))


(defn load-flights []
  ;(keyword (nth  4))
  (->>
    (csv/parse-csv (slurp "C:/Users/Arunas/Dropbox/darbas/travel planning/airports.dat"))
    (remove #(= (nth % 4) "\\N") )
    (map #(update % 9 read-string))
    ;(take 10)
    (reduce #(assoc %1 (keyword (nth %2 4))
                       (zipmap
                         [:id :name :city :country :iata :icao :latitude :longitude :altitude :timezone :dst :tz-db-timezone :type :source]
                         %2)) {})    ))