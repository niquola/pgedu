(ns pgedu.state
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cognitect.transit :as t]
   [pgedu.utils :as mu]))

(def socket (atom nil))
(def pendings (atom []))
(def tutorials (atom nil))

(def state (atom {:tutorials [] }))

(defn decode [str]
  (-> (t/reader :json)
      (t/read str)))

(defn encode [obj]
  (-> (t/writer :json)
      (t/write obj)))

(defn update-css [data]
  (aset (.getElementById js/document "stylo")
        "innerHTML"
        data))

(defn dispatch [resp]
  (.log js/console "dispatch !!!" (pr-str (:data resp)))
  (swap! state assoc-in [:tutorials] resp )
  #_(reset! state resp)
  )

(defn request [msg]
  (if @socket
    (.send @socket (encode msg)) 
    (swap! pendings conj (encode msg))))

(defn on-message [txt]
  (.log js/console "ON MESSAGE")
  (let [[act data] (decode txt)]
    (.log js/console "Data arrived" (pr-str [act data]))
    (cond (= :css act)  (update-css data))
    (= :responce act)  (dispatch (:data data))
    ))

(defn init-socket []
  (and @socket (.close @socket))
  (mu/open-socket
   "/_channel"
   {:onopen    (fn [ws] (reset! socket ws) 
                        (doseq [msg @pendings] (.send ws msg)))
    :onmessage (fn [ev] (on-message (.-data ev)))
    :onclose init-socket}))

(comment
  (and @socket (.close @socket))
  (init-socket))
