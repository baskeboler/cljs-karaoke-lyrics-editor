(ns app.renderer.views
  (:require [reagent.core  :as reagent :refer [atom]]
            [re-com.core :as recom]
            [re-frame.core :as rf]
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
            [app.renderer.components :as comps]
            [app.renderer.player :as player]))
            ;; ["electron" :as electron :refer [ipcRenderer]]))
(spec/check-asserts false)
;; -- Domino 5 - View Functions ----------------------------------------------

(extend-protocol protocols/PSong
  nil
  (get-song-length [this] 0))

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
  [:div.table-responsive>table.table.table-striped
   [:thead
    [:tr
     [:th "id"] [:th "offset"] [:th "length"] [:th "text"]]]
   [:tbody
    (doall
     (for [f (:frames @(rf/subscribe [:app.renderer.subs/current-song]))
           :when (>= (protocols/get-offset f)
                     @(rf/subscribe [:app.renderer.subs/slider-val]))]
       ^{:key (hash f)}
       [:tr {:class (when (= f @selected-frame) "table-active")
             :on-click #(reset! selected-frame f)}
        [:td (:id f)]
        [:td (format "%1.2f s"
                     (/ (protocols/get-offset f) 1000.0))]
        [:td (format "%1.2f" (-> (frame-length-ms f) ms->s))]
        [:td (protocols/get-text f)]
        [:td
         [recom/md-icon-button
          :md-icon-name "zmdi-delete"
          :size :regular
          :on-click #(rf/dispatch [::evts/delete-frame (:id f)])]]]))]])
          ;; :class "btn-danger"]]]))]])

(defn frame-list-card []
  [comps/card
   :title "Frame List"
   :body [frame-list]])

(defn expand-frame [f]
  [:ul
   (doall
    (for [e (:events f)]
      [:li (protocols/get-text e)]))])

(def slider-val (atom 0))
(def selected-frame (atom nil))

(defn offset [f]
  (format "%1.2f" (-> (protocols/get-offset f) ms->s)))
(defn frame-length-s [f]
  (format "%1.2f" (-> f (frame-length-ms) ms->s)))

;; (defn split-frame-at-event [evt])

(defn evt-row-editor [f evt-id on-submit on-cancel]
  (let [evt-sub (rf/subscribe [::subs/lyrics-event-by-id (:id f) evt-id])
        temp-event (atom @evt-sub)]
    (fn [f evt-id on-submit on-cancel]
      [:tr
       [:td
        [recom/h-box
         :children
         [[recom/label :label (/  (:offset @temp-event) 1000.0)]
          [recom/gap :size "1"]
          [recom/slider
           :min 0
           :max (lf/frame-ms-duration f)
           :model (:offset @temp-event)
           :on-change #(swap! temp-event assoc :offset %)]]]]
       [:td
        [recom/input-text
         :model (:text @temp-event)
         :on-change #(swap! temp-event assoc :text %)]]
       [:td
        [recom/h-box
         :children
         [[recom/md-icon-button
           :style {:color "green"}
           :md-icon-name "zmdi-check"
           :on-click #(on-submit @temp-event)]
          [recom/md-icon-button
           :style {:color "red"}
           :md-icon-name "zmdi-close"
           :on-click on-cancel]]]]])))

(defn evt-row [f evt-id]
  (let [evt-sub (rf/subscribe [::subs/lyrics-event-by-id (:id f) evt-id])
        editing? (atom false)
        on-cancel-edit (fn [] (reset! editing? false))
        on-submit-edit (fn [new-val]
                         (rf/dispatch-sync
                          [::evts/update-lyrics-event
                           (:id f)
                           evt-id
                           new-val])
                         (reset! editing? false))]
    
                         
    (fn []
      (if @editing?
        [evt-row-editor f evt-id on-submit-edit on-cancel-edit]
        [:tr
         [:td (offset @evt-sub)]
         [:td (protocols/get-text @evt-sub)]
         [:td
          [recom/h-box
           :children
           [[recom/md-icon-button 
             :md-icon-name "zmdi-format-valign-top"
             :on-click #(rf/dispatch [::evts/split-frame-at (:id f) (:offset @evt-sub)])]
            [recom/md-icon-button
             :md-icon-name "zmdi-edit"
             :on-click #(reset! editing? true)]]]]]))))

(defn frame-editor [f]
  [comps/card
   :title (str "Frame " (:id f))
   :body
   [recom/v-box
    :children
    [[:dl
      [:dt "offset"]
      [:dd (offset f)]
      [:dt "length"]
      [:dd (frame-length-s f)]
      [:dt "text"]
      [:dd (protocols/get-text f)]]
     [recom/title :label "events" :level :level2]
     [:div.table-responsive>table.table
      [:thead>tr
       [:th "offset"] [:th "text"] [:th "actions"]]
      [:tbody
       (doall
        (for [evt (:events @(rf/subscribe [::subs/frame-by-id (:id f)]))
              :let [evt-id (:id evt)]]
          ^{:key (str "event-" evt-id)}
          [evt-row f evt-id]))]]]]]) 

(defn ^:export ui
  []
  [recom/v-box
   :padding "1em"
   :children
   [;[recom/title :level :level1 :label "Hello world, it is now"]
    [comps/card
     :title "Load File"
     :body
     [recom/h-box
      :align :center
      :children [[recom/box
                  :size "auto"
                  :child [file-select]]
                 [recom/gap :size "a"]
                 [recom/md-icon-button
                  :md-icon-name (if @(rf/subscribe [::subs/select-value :playing?])
                                  "zmdi-pause"
                                  "zmdi-play")
                  :size :larger
                  :on-click #(rf/dispatch [(if @(rf/subscribe [::subs/select-value :playing?])
                                             ::evts/stop-playback
                                             ::evts/start-playback)])]
                 [recom/gap :size "1"]
                 [recom/title
                  :level :level1
                  :label @(rf/subscribe [::subs/select-value :playback-position])]]]]
    (when-not (nil? @(rf/subscribe [::subs/current-song]))
      [comps/card
       :title "Player"
       :body [player/player-panel]])
    (when-not (nil? @(rf/subscribe [::subs/current-song]))

      [comps/card
       :title "Controls"
       :body
       [recom/v-box
        :children
        [[recom/h-box
          :margin "1em 0"
          :children [[recom/button
                      :class "btn-primary"
                      :style {:border-radius 0}
                      :on-click #(rf/dispatch [:app.renderer.events/current-song nil])
                      :label "clear song"]
                     (when @selected-frame
                       [recom/button
                        :class "btn-warning"
                        :style {:border-radius 0}
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
         [file-display]]]])
    (when @selected-frame
      [frame-editor @selected-frame])
    ;; (when @selected-frame
      ;; [recom/scroller
       ;; :child
    [frame-list-card]]])
