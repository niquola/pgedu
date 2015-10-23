(ns pgedu.tutorials
  (:require 
            [clojure.java.io :as io] 
            [endophile.core :as epcore]
            [me.raynes.fs :as fs]
            [endophile.hiccup :as ephiccup])
  (:import java.io.File))

(def tutors-path "tutorials")

(defn md-hiccup [md] (ephiccup/to-hiccup (epcore/mp (slurp md))))

(defn tutors-files []
  (fs/list-dir (io/resource tutors-path)))

(defn tutor-parse [tutor-file]
  (md-hiccup tutor-file))

(defn tutor-desc [tutor-file]
  (let [tutor (tutor-parse tutor-file) ]
    {:id (fs/name tutor-file)
     :name (first tutor) 
     :content (rest tutor) 
     }))

(defn tutorials [& args]
  {:data (map tutor-desc (tutors-files))
 })

(comment 
  (map #(epcore/mp (slurp %)) (tutors-files))

  )


