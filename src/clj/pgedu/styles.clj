(ns pgedu.styles
  (:require [garden.core :refer [css]]
            [garden.units :as u :refer [px pt em]]
            [clojure.test]
            [garden.stylesheet :refer  [at-media]]
            [pgedu.channels :as mch]))

(def enums
  {:background-color {:gray "#eee"
                      :light-gray "#f2f2f2"
                      :blue-gray-lighten5 "#eceff1"
                      :blue-gray-lighten4 "#cfd8dc"
                      :alert "#e53935"
                      :alert-text "#ffebee"}
   :color {:normal "#555"
           :critical "#e53935"
           :info "green"
           :ok "#00c853"
           :red "red"
           :green "green"
           :alert-bg "#e53935"
           :alert-text "#ffebee"
           :blue-gray-lighten5 "#eceff1"
           :blue-gray-lighten4 "#cfd8dc"
           :blue-gray-lighten3 "#b0bec5"
           :blue-gray-darken1 "#546e7a"
           :blue-gray-darken2 "#455a64"
           :black "#333"
           :dark-gray "#555"
           :gray "#888"
           :blue   "#2196f3"
           :orange "#ff8f00"
           :light-gray "#bdbdbd"
           :unimportant "#999"}
   :font-size {:xxl (px 61)
               :xl (px 44)
               :l  (px 21)
               :m  (px 15)
               :s  (px 13)
               :xs  (px 11)}
   :font-weight {:bold "bold" :regular "normal" :light "lighter"}
   :letter-spacing {:normal "normal" :tight "-0.06em" :loose "0.06em"} 
   :padding {:large (px 15)
             :normal (px 10)
             :far (px 20)}})

(defn cget [what key]
  (get-in enums [what key]))

(defn mk-attr [attr default]
  (fn -tmp
    ([m] (-tmp m default))
    ([m val] (assoc m attr (if (keyword? val) (cget attr val) val)))))

(def bg  (mk-attr :background-color "white"))
(def padded  (mk-attr :padding :normal))
(def pointer  (mk-attr :cursor "pointer"))
(def center-text  (mk-attr :text-align "center"))
(def color  (mk-attr :color :normal))
(defn txt
  ([m size]
   (merge m {:font-size (get-in enums [:font-size size])}))
  ([m size w]
   (-> (merge m {:font-weight (cget :font-weight w)})
       (txt size)))
  ([m size weight ls]
   (-> (merge m {:letter-spacing (cget :letter-spacing ls)})
       (txt size weight))))

(defn mk-const
  ([mm] (fn [m] (merge m mm)))
  ([k a] (fn [m] (assoc m k a))))

(defmacro rules [& body] `(-> {} ~@body))

(def floats-inside (mk-const {:position "relative" :overflow "hidden"}))
(def right  (mk-const :float "right"))
(def undecorate  (mk-const :text-decoration "none"))
(def left   (mk-const :float "left"))
(def circ   (mk-const :border-radius "100%"))
(def margin (mk-const :margin (px 10)))
(def rounded (mk-const :border-radius (px 5)))

(def box (mk-const :display "inline-block"))
(def no-display (mk-const :display "none"))
(def inline-block (mk-const :display "inline-block"))
(def debug (mk-const :outline "1px solid red"))

(def align-middle  (mk-const :vertical-align "middle"))
(def align-bottom  (mk-const :vertical-align "bottom"))

(def text-to-right (mk-const :text-align "right"))
(def text-justify  (mk-const :text-align "justify"))

(def absolute (mk-const :position "absolute"))
(def relative (mk-const :position "relative"))

(defn no-side-paddings [m] (merge m {:padding-left 0 :padding-right 0}))
(defn side-paddings [m x] (merge m {:padding-left x :padding-right x}))
(defn no-paddings [m] (merge m {:padding 0}))
(defn width [m x] (assoc m :width (px x)))

;; TODO: fix mixin to recognize px automatically
;; (defn line-height [m x] 
;;   (if (boolean (re-find #"px" x))
;;     (merge m {:line-height (px x)})
;;     (merge m {:line-height (em x)})))
;; (defn line-height-px [m x] (merge m {:line-height (px x)}))
(defn line-height [m x] (merge m {:line-height (em x)}))
(defn line-height-px [m x] (merge m {:line-height (px x)}))
(defn height [m x] (merge m {:height (px x)}))

(defn custom [m cm] (merge m cm))
(defn to-down
  ([m] (merge m {:margin-top (px 20)}))
  ([m x] (merge m {:margin-top (px x)})))

(defn to-right
  ([m] (merge m {:margin-left 20}))
  ([m x] (merge m {:margin-left (px x)})))

(defn to-left
  ([m] (merge m {:margin-right 20}))
  ([m x] (merge m {:margin-right (px x)})))

(defn shadow-depth-0 [m]
  (merge m 
   {:box-shadow "0 1px 1px 0 rgba(0, 0, 0, 0.25), 0 2px 10px 0 rgba(0, 0, 0, 0.06)"}))

(defn shadow-depth-1 [m]
  (merge m 
   {:box-shadow "0 1px 1px 0 rgba(0, 0, 0, 0.16), 0 2px 10px 0 rgba(0, 0, 0, 0.12)"}))

(defn shadow-depth-2 [m]
  (merge m 
   #_{:box-shadow "0 8px 17px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19)"}
   {:box-shadow "0 4px 5px 0 rgba(0, 0, 0, 0.09), 0 3px 6px 0 rgba(0, 0, 0, 0.15)"}))

(defn shadow-depth-3 [m]
  (merge m 
   {:box-shadow "0 8px 17px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19)"}))

(defn text-shadow-1 [m]
  (merge m 
   {:text-shadow "1px 1px 2px rgba(0, 0, 0, 0.20)"}))

(defn shadow-alert [m]
  (merge m 
   {:box-shadow "0 7px 19px 0 rgba(0, 0, 0, 0.19), 0 2px 5px 0 rgba(232, 0, 77, 0.24)"}))

(defn stroke [m color] (assoc m :stroke (cget :color color)))
(defn stroke-width [m x] (assoc m :stroke-width (px x)))
(def stroke-dash (mk-const :stroke-dasharray "1,3"))

(defn left-border [m opts] (merge m {:border {:left opts}}))

(def layout
  [:body [[bg :gray] padded]
   [:.promo
    [[height 200] center-text [padded :far]]]])

(defn process-rule [opts]
  (if (map? opts)
    opts
    (reduce (fn [acc mixin]
              (cond
                (vector? mixin) (apply (first mixin) acc (rest mixin))
                (map? mixin) (merge acc mixin)
                :else (mixin acc)))
            {} opts)))

(defn dsl->garden [[rule opts & nested]]
  (if (keyword? rule) 
    (into [rule (process-rule opts)] (map dsl->garden nested))
    (rule opts (map dsl->garden nested))))

(def style
  (dsl->garden [:html [] layout]))

(defn get-style [] (css style))
;; (defn get-style [] (css (merge style testoo)))

(defn $style [req]
  {:body (get-style) 
   :headers {"Content-Type" "text/css"}})

(defn push-changes []
  (mch/notify [:css (get-style)]))

(comment
  (push-changes)
  )
