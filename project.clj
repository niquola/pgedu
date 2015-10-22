(defproject pgedu "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [

                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145" :scope "provided"]

                 [cheshire "5.5.0"]
                 [com.cognitect/transit-clj "0.8.283"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [environ "1.0.1"]
                 [garden "1.3.0-SNAPSHOT"]
                 [hiccup "1.0.5"]
                 [http-kit "2.1.19"]
                 [prone "0.8.2"]

                 [reagent "0.5.1"]
                 [reagent-forms "0.5.12"]
                 [reagent-utils "0.1.5"]
                 [ring "1.4.0"]
                 [ring-middleware-format "0.6.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [route-map "0.0.2"]

                 ;; db section
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.postgresql/postgresql "9.4-1204-jdbc41"]
                 [honeysql "0.6.2"]

                 ;; data
                 [prismatic/schema "1.0.1"]
                 ]

  :plugins [[lein-environ "1.0.1"]
            [lein-ancient "0.6.4"]
            [lein-asset-minifier "0.2.2"]]

  :ring {:handler pgedu.handler/app
         :uberwar-name "pgedu.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "pgedu.jar"

  :main pgedu.core

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.4.0"]
                                  [lein-figwheel "0.4.1"]
                                  [org.clojure/tools.nrepl "0.2.11"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [pjstadig/humane-test-output "0.7.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.4.0"]
                             [lein-cljsbuild "1.1.0"]]

                   :figwheel {:http-server-root "public"
                              :server-port 3001
                              :nrepl-port 7002
                              :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                              :css-dirs ["resources/public/css"]
                              :ring-handler pgedu.core/app}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "pgedu.dev"
                                                         :source-map true}}}}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
