(ns pgedu.utils
  (:require [goog.net.XhrIo :as xhr]
            [goog.string :as gstring]
            [goog.i18n.DateTimeFormat :as dtf]
            [cognitect.transit :as t]))

(def nbsp (gstring/unescapeEntities "&nbsp;"))
(def black-circle (gstring/unescapeEntities "&#11044;"))
(def circle (gstring/unescapeEntities "&#9711;"))
(def left (gstring/unescapeEntities "&#x2770;"))

(def format-map
  (let [f goog.i18n.DateTimeFormat.Format]
    {:FULL_DATE (.-FULL_DATE f)
     :FULL_DATETIME (.-FULL_DATETIME f)
     :FULL_TIME (.-FULL_TIME f)
     :LONG_DATE (.-LONG_DATE f)
     :LONG_DATETIME (.-LONG_DATETIME f)
     :LONG_TIME (.-LONG_TIME f)
     :MEDIUM_DATE (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME (.-MEDIUM_TIME f)
     :SHORT_DATE (.-SHORT_DATE f)
     :SHORT_DATETIME (.-SHORT_DATETIME f)
     :SHORT_TIME (.-SHORT_TIME f)}))

(defn format-date
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum or a formatting string like \"dd MMMM yyyy\""
  [date-format date]
  (.format (goog.i18n.DateTimeFormat.
            (or (date-format format-map) date-format))
           (js/Date. date)))

(defn fmt-time [d] (when d (str (.getHours d) ":" (.getMinutes d) ":" (.getSeconds d))))

(defn yes-no [b] (if b "yes" "no"))

(defn t-read [str]
  (-> (t/reader :json) (t/read str)))

(defn t-write [obj]
  (-> (t/writer :json) (t/write obj)))

(defn socket-url
  ([pth]
   (let [l (.-location js/window)]
     (str "ws://" (.-host l) pth)))
  ([port pth]
   (let [l (.-location js/window)]
     (str "ws://" (.-hostname l) ":" port pth))))

(defn open-socket [pth opts]
  (let [ws (js/WebSocket. (socket-url pth))]
    (.log js/console "Connecting: " ws)
    (when-let [h (:oninit opts)]
      (aset ws "onopen" (h ws)))

    (when-let [h (:onopen opts)]
      (aset ws "onopen" (fn [_] (h ws))))

    (doseq [ev [:onmessage :onclose]]
      (when-let [h (get opts ev)]
        (aset ws (name ev) h)))
    ws))

(defn ws-send [ws msg] (when ws (.send ws (t-write msg))))

(def ids (atom 1000))
(defn next-id [] (swap! ids inc))

(defn on-enter [f]
  (fn [ev]
    (let [key-pressed (.-which ev)]
      (condp = key-pressed 13 (f) nil))))

(defn xhr-data [ev] (-> ev .-target .getResponseText))
(defn xhr-success? [ev] (-> ev .-target .isSuccess))

(defn redirect [pth]
  (aset js/window.location "hash" (str "#" pth)))

(defn post [pth content cb eb]
  (let [l (.-location js/window)
        url (str "//" (.-host l) pth)
        handle (fn [ev]
                 (.log js/console ev)
                 (let [data (xhr-data ev)
                       data (and data (t-read data))]
                   (if (xhr-success? ev)
                     (cb data)
                     (eb data))))]
    (xhr/send url
              handle
              "POST"
              (t-write content)
              #js {"Content-Type" "application/transit+json"})))

(defn log [& x] (.log js/console (pr-str x)))

