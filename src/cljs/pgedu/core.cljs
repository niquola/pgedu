(ns pgedu.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [clojure.string :as string]
              [route-map.core :as rm]
              [pgedu.state :as state]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(defn socket-url
  ([pth] (let [l (.-location js/window)] (str "ws://" (.-host l) pth)))
  ([port pth] (let [l (.-location js/window)] (str "ws://" (.-hostname l) ":" port pth))))

(def settings
  {:client_id "6fe91e73b2db784095aa" 
   :redirect_uri "http://postgrest.dev.health-samurai.io:3000/auth/callback"
   :scope "user:email"
   :state "ups"})

(defn to-query-string [m]
  (string/join "&" (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))) m)))

(defn auth-link []
  (str "https://github.com/login/oauth/authorize?"
       (to-query-string settings)))

(.log js/console (auth-link))

(defn home-page []
  [:div
   [:div.menu
    [:div.brand "PGedu"]]
   [:div.promo
    [:h1 "Interactive PostgreSQL tutorials"]
    [:a.btn.btn-success {:href (auth-link)} "Register Now"]]])

(defn about-page []
  [:div [:h2 "About pgedu"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn tutor-content [tutor]
  [:div 
      [:h2 (:title tutor)]
      (for [part (:parts tutor)]
       [:div
        [:b (:title part)] 
        [:p (:content part)] ]
       )])

(defn tutor-page [& args]

  (state/request {:path ["tutorials/" ] :method :GET}) 
  (let [tutor ((:tutorials @state/state) (:id (first args)))]
    [:div
     [:div.back-link
      [:a {:href "/#/tutorials"} "<- Back"]]
     (tutor-content tutor) 
     ]))

(defn tutors-page []
  (state/request {:path ["tutorials"] :method :GET}) 
  (fn []
    [:div
     [:h1 "Tutorials page"]
     [:pre (pr-str (:tutorials @state/state))]
     (for [tut (:tutorials @state/state)]
       [:div
        [:h3
          [:a {:href (str "#tutorials/" (:id tut))}
           [:b (second (:name tut))]]]
        [:div ]
        [:div (:structure tut)]
       ])
]
    ))

(def routes
  {:GET  #'home-page
   "tutorials" {:GET #'tutors-page
                [:id] {:GET #'tutor-page}}
   "about"  {:GET #'about-page}})

(defn dispatch [event]
  (.log js/console "Dispatch:" event)
  (if-let [m (rm/match [:GET (.-token event)] routes)]
    (let [mws (mapcat :mw (:parents m)) ]

      (when (every? (fn [f] (f event)) mws)
        (.log js/console (pr-str (:params m)))
        (session/put! :current-page (fn [& args] 
                                      ((:match m) (:params m))))))
    (session/put! :current-page (fn [& args] [:h1 (str "Paget " (.-token event) " not found")]))))


(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen EventType/NAVIGATE dispatch)
    (.setEnabled true)))

(defn current-page [] [:div [(session/get :current-page)]])

(defn mount-root []
  (.log js/console "mount-root")
  (reagent/render (current-page) (.-body js/document)))

(defn init! []
  (hook-browser-navigation!)
  (state/init-socket)
  (mount-root))
