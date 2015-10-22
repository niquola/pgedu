(ns pgedu.state
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cognitect.transit :as t]
   [pgedu.utils :as mu]))

(def socket (atom nil))

(def state (atom {
                  :tutorials [{:id 0 :title "The Ins and Outs of Databases"
                               :parts [{:title "Introducing SQL"
                                        :content "SQL is a Standard - BUT....
                                        Although SQL is an ANSI (American National Standards Institute) standard, there are different versions of the SQL language.
                                        However, to be compliant with the ANSI standard, they all support at least the major commands (such as SELECT, UPDATE, DELETE, INSERT, WHERE) in a similar manner." 
                                        :test "Some task for part 1" } 
                                       {:title "Database Usage"  
                                        :content "Using SQL in Your Web Site
                                        To build a web site that shows data from a database, you will need:
                                        An RDBMS database program (i.e. MS Access, SQL Server, MySQL)
                                                 To use a server-side scripting language, like PHP or ASP
                                                 To use SQL to get the data you want
                                                 To use HTML / CSS " 
                                        :test "Some task for part 2" }
                                       {:title "SQL Language"
                                        :content "SQL is a standard language for accessing and manipulating databases."
                                        :test "Some task for part 3" }
                                       ]}
                              {:id 1 :title "Managing Data"
                               :parts [{:title "Introducing SQL"
                                        :content "SQL is a Standard - BUT....
                                        Although SQL is an ANSI (American National Standards Institute) standard, there are different versions of the SQL language.
                                        However, to be compliant with the ANSI standard, they all support at least the major commands (such as SELECT, UPDATE, DELETE, INSERT, WHERE) in a similar manner." 
                                        :test "Some task for part 1" } 
                                       {:title "Database Usage"  
                                        :content "Using SQL in Your Web Site
                                        To build a web site that shows data from a database, you will need:
                                        An RDBMS database program (i.e. MS Access, SQL Server, MySQL)
                                                 To use a server-side scripting language, like PHP or ASP
                                                 To use SQL to get the data you want
                                                 To use HTML / CSS " 
                                        :test "Some task for part 2" }
                                       {:title "SQL Language"
                                        :content "SQL is a standard language for accessing and manipulating databases."
                                        :test "Some task for part 3" }
                                       ] }
                              {:id 2 :title "Managing Databases and Tables"
                               :parts [{:title "Introducing SQL"
                                        :content "SQL is a Standard - BUT....
                                        Although SQL is an ANSI (American National Standards Institute) standard, there are different versions of the SQL language.
                                        However, to be compliant with the ANSI standard, they all support at least the major commands (such as SELECT, UPDATE, DELETE, INSERT, WHERE) in a similar manner." 
                                        :test "Some task for part 1" } 
                                       {:title "Database Usage"  
                                        :content "Using SQL in Your Web Site
                                        To build a web site that shows data from a database, you will need:
                                        An RDBMS database program (i.e. MS Access, SQL Server, MySQL)
                                                 To use a server-side scripting language, like PHP or ASP
                                                 To use SQL to get the data you want
                                                 To use HTML / CSS " 
                                        :test "Some task for part 2" }
                                       {:title "SQL Language"
                                        :content "SQL is a standard language for accessing and manipulating databases."
                                        :test "Some task for part 3" }
                                       ]
                               }
                              ]
                  }))

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
  (.log js/console "dispatch" (pr-str (:data resp))))

(defn request [msg]
  (.log js/console "request" (pr-str msg))
  (and @socket (.send @socket (encode msg))))

(defn on-message [txt]
  (let [[act data] (decode txt)]
    (.log js/console "Data arrived" (pr-str [act data]))
    (cond (= :css act)  (update-css data))))

(defn init-socket []
  (and @socket (.close @socket))
  (mu/open-socket
   "/_channel"
   {:onopen    (fn [ws] (reset! socket ws))
    :onmessage (fn [ev] (on-message (.-data ev)))
    :onclose init-socket}))

(comment
  (and @socket (.close @socket))
  (init-socket))
