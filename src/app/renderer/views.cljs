(ns app.renderer.views
  (:require [reagent.core  :as reagent :refer [atom]]
            [re-com.core :as recom]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [clojure.string :as str]
            [cljs.reader :as cljsreader]
            [cljs.core.async :as async]
            [cljs.spec.alpha :as spec]
            [cljs.nodejs :as nodejs]
            [app.renderer.events :as evts]
            [app.renderer.subs :as subs]
            [clj-karaoke.protocols :as protocols]
            [clj-karaoke.song-data :as sd]
            [clj-karaoke.lyrics-frame :as lf]
            [clj-karaoke.lyrics-event :as le]
            [goog.string :as gstr :refer [format]]
            [app.renderer.components :as comps]))
(spec/check-asserts false)
;; -- Domino 5 - View Functions ----------------------------------------------
;; (def current-song (atom nil))
(defn clock
  []
  [recom/title
   :level :level2
   :style {:color @(rf/subscribe [:time-color])}
   :label
   (-> @(rf/subscribe [:time])
       .toTimeString
       (str/split " ")
       first)])

(defn load-lyrics-file [evt]
  (let [ret (async/chan)
        reader (doto (js/FileReader.)
                 (.addEventListener
                  "load"
                  (fn [evt2]
                    (async/put! ret (-> evt2 .-target .-result)))))]
    (.. reader (readAsText (-> evt .-target .-files (aget 0))))
    (async/go
      (let [t (async/<! ret)]
        ;; (println t)
        (rf/dispatch [:app.renderer.events/slider-val 0])
        (rf/dispatch [:app.renderer.events/current-song (protocols/map-> (cljsreader/read-string t))])))))
        ;; (reset! current-song
                ;; (protocols/map->
                 ;; (cljsreader/read-string t))))))

(defn file-select []
  [:div.file-selector
   [:input {:type :file
            :accept ".edn"
            :on-change load-lyrics-file}]])

(defn file-display []
  [:div.file-display
   [:h4 (:title @(rf/subscribe [:app.renderer.subs/current-song]))]
   [:h5  (:date @(rf/subscribe [:app.renderer.subs/current-song]))]
   [:p "frames: " (count (:frames @(rf/subscribe [:app.renderer.subs/current-song])))]])

(declare slider-val selected-frame)

(defn frame-length-ms [f]
  (-> f :events last :offset))

(defn ms->s [ms]
  (/ ms 1000.0))

(defn frame-list []
  [:table.table.table-responsive.table-striped
   [:thead
    [:tr
     [:th "id"] [:th "offset"] [:th "length"] [:th "text"]]]
   [:tbody
    (doall
     (for [f (:frames @(rf/subscribe [:app.renderer.subs/current-song]))
           :when (>= (protocols/get-offset f)
                     @(rf/subscribe [:app.renderer.subs/slider-val]))]
       ^{:key (hash f)}
       [:tr {:class (when (= f @selected-frame) "active")
             :on-click #(reset! selected-frame f)}
        [:td (:id f)]
        [:td (format "%1.2f s"
                     (/ (protocols/get-offset f) 1000.0))]
        [:td (format "%1.2f" (-> (frame-length-ms f) ms->s))]
        [:td (protocols/get-text f)]
        [:td 
         [recom/button
          :label "delete"
          :on-click #(rf/dispatch [::evts/delete-frame (:id f)])
          :class "btn-danger"]]]))]])
(defn expand-frame [f]
  [:ul
   (doall
    (for [e (:events f)]
      [:li (protocols/get-text e)]))])

(defn color-input
  []
  [:div.color-input
   "Time color: "
   [:input {:type "color"

            :value @(rf/subscribe [:time-color])
            :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]])  ;; <---
(def slider-val (atom 0))
(def selected-frame (atom nil))

(defn offset [f]
  (format "%1.2f" (-> (protocols/get-offset f) ms->s)))
(defn frame-length-s [f]
  (format "%1.2f" (-> (frame-length-ms) ms->s)))

;; (defn split-frame-at-event [evt])
  

(defn evt-row [f evt]
  [recom/h-box
   :children
   [[recom/label :label (offset evt)]
    [recom/gap :size "1"]
    [recom/label :label (protocols/get-text evt)]
    [recom/gap :size "1"]
    [recom/button :label "split frame here" :on-click #(rf/dispatch [::evts/split-frame-at (:id f) (:offset evt)])]]])
    
(defn frame-editor [f]
  [recom/border
   :margin "15px"
   :child
   [recom/v-box
    :size "auto"
    :children
    [[recom/h-box
      :children [[recom/label :label "offset"]
                 [recom/gap :size "1"]
                 [recom/label :label (offset f)]]]
     [recom/h-box
      :children [[recom/label :label "length"]
                 [recom/gap :size "1"]
                 [recom/label :label (frame-length-s f)]]]
     [recom/h-box
      :children
      [[recom/label :label "text"]
       [recom/gap :size "1"]
       [recom/label :label (protocols/get-text f)]]]
     [recom/title :label "events" :level :level2]
     [recom/v-box
      :children
      ;; (into []
      (doall
       (for [evt (:events @(rf/subscribe [::subs/frame-by-id (:id f)]))]
          [evt-row f evt]))]]]])

(defn ui
  []
  [:div
   "阿斗:"]
  [recom/v-box
   :padding "1em"
   :children
   
   [[recom/title :level :level1 :label "Hello world, it is now"]
    [comps/card
     :title "my card"
     :body
     [recom/h-box
      :children [[recom/box
                  :size "auto"
                  :child [file-select]]
                 [recom/box
                  :size "auto"
                  :child [clock]]
                 [recom/box
                  :size "auto"
                  :child [color-input]]]]]
    (when-not (nil? @(rf/subscribe [:app.renderer.subs/current-song]))
      [recom/v-box
       :children
       [[recom/h-box
         :margin "4"
         :children [[recom/gap :size "1"]
                    [recom/button
                     :class "btn-lg btn-primary"
                     :on-click #(rf/dispatch [:app.renderer.events/current-song nil])
                     :label "clear song"]
                    [recom/gap :size "1"]
                    (when @selected-frame
                      [recom/button
                       :class "btn-lg btn-warning"
                       :on-click #(reset! selected-frame nil)
                       :label "clear selected frame"])
                    [recom/gap :size "1"]]]
        
        [recom/label
         :label (format "%1.2fs"
                        (/ @(rf/subscribe [:app.renderer.subs/slider-val])
                           1000.0))]
        [recom/slider
         :min       0
         :max       (protocols/get-song-length @(rf/subscribe [:app.renderer.subs/current-song]))
         :model     (rf/subscribe [:app.renderer.subs/slider-val])
         :on-change #(rf/dispatch-sync [:app.renderer.events/slider-val (double %)])]
        [file-display]
        (when @selected-frame
          [frame-editor @selected-frame])
        [recom/scroller
         :child [frame-list]]]])]])
