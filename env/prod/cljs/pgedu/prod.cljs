(ns pgedu.prod
  (:require [pgedu.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
