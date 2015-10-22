(ns pgedu.channels
  (:require [org.httpkit.server :as ohs]
            [simply.formats :as fmt]))

(defonce clients (atom {}))

(defn encode [clj] (fmt/to-transit clj))

(defn decode [txt] (fmt/from-transit txt))

(defn add-client [ch usr]
  (swap! clients assoc ch {:subscriptions [] :user usr}))

(defn rm-client [ch]
  (swap! clients dissoc ch))

(defn subscribe [ch topic]
  (swap! clients (fn [cls] (update-in cls [ch] conj topic))))

(defn notify [resp]
  (println (pr-str resp))
  (let [msg (encode resp)]
    (doseq [[ch prop] @clients]
      (ohs/send! ch (encode resp)))))

(def routes {})

(defn dispatch [ch msg]
  (println "Message from client" msg)
  (let [client (get @clients ch)
        req    (merge msg {:user (:user client) :client client})
        pth    (into (:path req) [(:method req)])
        hndl   (get-in routes pth)]
    (if hndl
      (notify [:responce (merge (hndl req) (select-keys req [:path :method]))]) 
      (ohs/send! ch (encode {:status 404 :data {:message (str  "Handler " (pr-str pth) " not found")}})))))

(defn on-msg [ch txt]
  (dispatch ch (decode txt)))

(defn channel [{user :current-user params :params :as req}]
  (ohs/with-channel req ch
    (add-client ch user)
    (ohs/on-receive ch (fn [msg] (on-msg ch msg)))
    (ohs/on-close ch  (fn [st] (rm-client ch)))))
