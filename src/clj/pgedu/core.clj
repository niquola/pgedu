(ns pgedu.core
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.resource :as rmr]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [org.httpkit.server :as ohs]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [route-map.core :as rm]

            [pgedu.styles :as css]
            [pgedu.channels :as chan]
            [pgedu.auth :as auth]

            [simply.formats :as sf]
            [environ.core :refer [env]])
  (:gen-class))

(defn home-page [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body
   (html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      (include-css "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.5/css/bootstrap.min.css")
      (include-css "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.4.0/css/font-awesome.min.css")
      [:style {:id "stylo" :type "text/css"} (css/get-style)]]
     [:body#app
      [:div "..."]
      (include-js "js/app.js")]])})

(def routes
  {:GET       #'home-page
   "auth"     {"callback" {:GET #'auth/$callback}}
   "style"    {:GET #'css/$style}
   "_channel" {:GET #'chan/channel}})

(defn notfound [meth uri]
  {:status  404
   :headers {"Content-Type" "text"}
   :body    (str " Not found " meth " " uri)})

(defn dispatch [{meth :request-method uri :uri :as req}]
  (println "\n\nHTTP:  " meth " "  uri)
  (if-let [rt (rm/match [meth uri] routes)]
    ((:match rt) req)
    (notfound meth uri)))

(def defaults (merge site-defaults {:security {:anti-forgery false}}))

(defn process-body [req]
  (if (and
       (= (get-in req [:headers "content-type"]) "application/transit+json")
       (not (nil? (:body req))))
    (assoc req :data (sf/from-transit (:body req)))
    req))

(defn wrap-magic-format [h]
  (fn [req]
    (let [req (process-body req)
          res (h req)]
      (println "BODY:" (:data req))
      (if (coll? (:body res))
        (-> res
            (update-in [:body] sf/to-transit)
            (update-in [:headers "Content-Type"] (constantly "application/transit+json")))
        res))))

(def app
  (let [handler (-> #'dispatch
                    (wrap-magic-format)
                    (wrap-defaults defaults)
                    (rmr/wrap-resource "public"))]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))

(defn start []
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (ohs/run-server #'app {:port port})))

(defn -main [] (start))

(comment
  (stop)
  (def stop (start))

  (zipmap)
  (zipmap (range 0 20) (range 0 20))

  (lazy-seq))
