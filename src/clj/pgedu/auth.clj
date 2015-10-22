(ns pgedu.auth
  (:require [org.httpkit.client :as http]
            [ring.util.codec :as codec]))


(defn read-responce [{body :body}]
  (cond
    (string? body) body
    :else (slurp body)))

(defn request-token [code]
  (read-responce @(http/request
    {:url "https://github.com/login/oauth/access_token"
     :method :post
     :form-params {:client_id "6fe91e73b2db784095aa"
                   :client_secret "f0530c1a2d3efb0520e18997fe15b35926024925"
                   :code code
                   :state "ups"
                   :redirect_uri "http://postgrest.dev.health-samurai.io:3000/auth/callback"}})))

(defn request-profile [access-token]
  (read-responce @(http/request
    {:url "https://api.github.com/user"
     :method :get
     :headers {"Authorization" (str "token " access-token)}})))

(defn $callback [{{code :code} :params :as req}]
  (let [access-token (get (codec/form-decode (request-token code)) "access_token")
        profile (request-profile access-token)]
    {:body (pr-str profile)
     :headers {"Content-Type" "text"} 
     :status 200}))
