(ns simply.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as cs]
    [honeysql.core :as sql]
    [clj-time.core :as t]
    [clj-time.coerce :as tc]
    [simply.formats :as sf]
    [environ.core :as env])
  (:import (org.joda.time DateTime)
           (java.sql Timestamp)
           (java.util Date)
           (org.postgresql.jdbc4 Jdbc4Array)
           (org.postgresql.util PGobject)))

;;; coerce
(defn- sql-time-to-clj-time [sql-time]
  (tc/from-sql-time sql-time))

(defn- clj-time-to-sql-time [clj-time]
  (tc/to-sql-time clj-time))

(defn- quote-seq [v]
  (str "{" (cs/join "," (map #(str "\"" % "\"") v)) "}"))

(defn- map-map [m map-fn]
  (reduce (fn [new-map [k v]]
            (assoc new-map k (map-fn v)))
          {} m))

(defmulti to-jdbc class)

(defmethod to-jdbc clojure.lang.PersistentArrayMap [m] (sf/to-json m))
(defmethod to-jdbc clojure.lang.PersistentHashMap [m] (sf/to-json m))

(defmethod to-jdbc clojure.lang.Keyword [v]
  (name v))

(defmethod to-jdbc org.joda.time.DateTime [v]
  (clj-time-to-sql-time v))

(defmethod to-jdbc java.util.Date [v]
  (java.sql.Timestamp. (.getTime v)))

(defmethod to-jdbc clojure.lang.PersistentVector [v]
  (quote-seq v))

(defmethod to-jdbc clojure.lang.PersistentList [v]
  (quote-seq v))

(defmethod to-jdbc clojure.lang.PersistentHashSet [s]
  (quote-seq s))

(defmethod to-jdbc :default [v] v)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(extend-protocol jdbc/IResultSetReadColumn
  org.postgresql.jdbc4.Jdbc4Array
  (result-set-read-column [pgobj metadata idx]
    (vec (.getArray pgobj))))

(defmulti from-jdbc class)

(defmethod from-jdbc clojure.lang.PersistentArrayMap [m] (map-map m from-jdbc))
(defmethod from-jdbc clojure.lang.PersistentHashMap [m] (map-map m from-jdbc))

(defmethod from-jdbc org.postgresql.util.PGobject [v]
  (if (or (= (.getType v) "json") (= (.getType v) "jsonb"))
    (sf/from-json (.toString v))
    (.toString v)))                     ;

(defmethod from-jdbc org.postgresql.jdbc4.Jdbc4Array [v]
  (vec (.getArray v)))

(defmethod from-jdbc java.sql.Timestamp [v]
  (sql-time-to-clj-time v))

(defmethod from-jdbc :default [v] v)

;;; database
(def ^:dynamic *db* "postgresql://aidbox:aidbox@localhost:5432/test?stringtype=unspecified")

(defn conn-to [x]
  (println "DB" x)
  (str (env/env :database-url) x "?stringtype=unspecified"))

(defmacro with-db-spec [conn & body]
  `(binding [*db* ~conn] ~@body))

(defmacro with-db [db & body]
  `(with-db-spec (conn-to ~db) ~@body))

(defmacro with-connection [[binding] & body]
  `(if-let [conn# (:connection *db*)]
     (let [~(symbol binding) conn#] ~@body)
     (with-open [conn#  (jdbc/get-connection *db*)]
       (let [~(symbol binding) conn#] ~@body))))

(defmacro transaction  [& body]
  `(jdbc/with-db-transaction  [t-db# *db*]
     (with-db-spec t-db# ~@body)))

(defmacro rollback-transaction  [& body]
  `(jdbc/with-db-transaction  [t-db# *db*]
     (jdbc/db-set-rollback-only! t-db#)
     (with-db-spec t-db# ~@body)
     (println "ROLLBACK;")))

(defn- coerce-query-args [sql]
  (let [[stmt & args] (if (coll? sql) sql [sql])
        coerced-args (map to-jdbc args)]
    (into [stmt] coerced-args)))

(defmacro report-actual-sql-error [& body]
  `(try
     ~@body
     (catch java.sql.SQLException e#
       (if (.getNextException e#) ;; rethrow exception containing SQL error
         (let [msg# (.getMessage (.getNextException e#))]
           (throw (java.sql.SQLException.
                    (str (cs/replace (.getMessage e#)
                                     "Call getNextException to see the cause." "")
                         "\n" msg#))))
         (throw e#)))))

(defn exec! [sql]
  (jdbc/execute! sql {:transaction? false}))

(defn create-db [db-name template]
  (jdbc/execute!
    *db*
    [(str "CREATE DATABASE " db-name " TEMPLATE = " template)]
    :transaction? false))

(defn drop-db [db-name]
  (jdbc/execute!
    *db*
    [(str "DROP DATABASE IF EXISTS " db-name)]
    :transaction? false))

(defn e! [& cmd]
  (println "SQL:" cmd)
  (time
    (report-actual-sql-error
      (if (vector? (first cmd))
        (apply jdbc/execute! *db* cmd)
        (jdbc/execute! *db* cmd)))))

(defn e*! [hsql]
  (e! (sql/format hsql)))

(defn q [sql]
  (println "SQL:" sql)
  (time
    (report-actual-sql-error
      (jdbc/query *db* (coerce-query-args sql) :row-fn from-jdbc))))

(defn q* [hsql]
  (q (sql/format hsql)))

(defn q-one* [hsql]
  (first (q* hsql)))

(defn q-val* [hsql]
  (when-let [res (first (q* hsql))]
    (first (vals res))))

(defn row-to-jdbc [m]
  (map-map m to-jdbc))

(defn i! [tbl & row-maps]
  (let [coerced-rows (map row-to-jdbc row-maps)]
    (println "INSERT INTO" tbl (pr-str coerced-rows))
    (time
      (report-actual-sql-error
        ;; perform insert and coerce results from jdbc
        (map from-jdbc
             (apply jdbc/insert! *db* tbl coerced-rows))))))

(defn d! [tbl & args]
  (println "DELETE FROM:" tbl (pr-str args))
  (time
    (report-actual-sql-error
      (first (apply jdbc/delete! *db* tbl (coerce-query-args args))))))

(defn u!
  "{:update table-name :set values :where clause}"
  [hsql]
  (e!  (sql/format (update-in hsql  [:set] row-to-jdbc))))

(defn u-one!
  [tbl ent]
  (u! {:update tbl :set ent :where  [:= :id (:id ent)]}))

(defn qualified-name [tbl]
  (let [nm (name tbl)]
    (if (> (.indexOf nm ".") -1)
     (cs/split nm #"\.")
     ["public" nm])))


(defn table-exists? [tbl]
  (let [[sch tbl] (qualified-name tbl)]
    (q-one* {:select [:*]
             :from [:information_schema.tables]
             :where [:and
                     [:= :table_name tbl]
                     [:= :table_schema sch]]})))

(defn init-db []
  (when-not (table-exists? :app.schema)
    (e! "create schema if not exists app")
    (e!
      (jdbc/create-table-ddl
        :app.schema
        [:name :text "PRIMARY KEY"]
        [:created_at :timestamp "DEFAULT CURRENT_TIMESTAMP"]))))

(defn migration-exists? [nm]
  (q-one* {:select [:*] :from [:app.schema] :where [:= :name nm]}))

(defmacro migrate-up [nm & body]
  `(when-not (migration-exists? ~(str nm))
     (println "migrate-up" ~(str nm))
     ~@body
     (i! :app.schema {:name ~(str nm)})))

(defmacro migrate-down [nm & body]
  `(when (migration-exists? ~(str nm))
     (println "migrate-down" ~(str nm))
     ~@body
     (d! :app.schema ["name=?" ~(str nm)])))

(defn create-table [& args]
  (e! (apply jdbc/create-table-ddl args)))

(defn raw [& args]
  "proxy to honey.sql/raw"
  (apply sql/raw args))

(defn *create [tbl attrs] (first (i! tbl attrs)))

(defn *update
  ([tbl attrs]
   (u! {:update tbl :set attrs :where [:= :id (:id attrs)]})))

(defn *find [tbl id]
  (q-one* {:select [:*] :from   [tbl] :where [:= :id id]}))

(defn *destroy [tbl id]
  (when-let [item (*find tbl id)]
    (d! tbl ["id = ?" id])
    item))

(defn *all [tbl]
  (q* {:select [:*] :from [tbl]}))
