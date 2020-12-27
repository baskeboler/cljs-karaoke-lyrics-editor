(ns app.renderer.events
  (:require
   [re-frame.core  :as rf :refer [reg-event-db reg-event-fx inject-cofx path after]]
   [clj-karaoke.lyrics-frame :as lf]
   [cljs.spec.alpha :as s]))



;; -- Domino 2 - Event Handlers -----------------------------------------------


(rf/reg-event-db              ;; sets up initial application state
 :initialize                 ;; usage:  (dispatch [:initialize])
 (fn [_ _]                   ;; the two parameters are not important here, so use _
   {:time         (js/Date.) ;; What it returns becomes the new application state
    :time-color   "#f88"
    :slider-val   0
    :current-song nil
    :modals       '()}))    ;; so the application state will initially be a map with two keys


(rf/reg-event-db                ;; usage:  (dispatch [:time-color-change 34562])
 :time-color-change            ;; dispatched when the user enters a new colour into the UI text field
 (fn [db [_ new-color-value]]  ;; -db event handlers given 2 parameters:  current application state and event (a vector)
   (assoc db :time-color new-color-value)))   ;; compute and return the new application state


(rf/reg-event-db                 ;; usage:  (dispatch [:timer a-js-Date])
 :timer                         ;; every second an event of this kind will be dispatched
 (fn [db [_ new-time]]          ;; note how the 2nd parameter is destructured to obtain the data value
   (assoc db :time new-time)))  ;; compute and return the new application state

(rf/reg-event-db
 ::current-song
 (fn [db [_ song]]
   (-> db
       (assoc :current-song song))))

(rf/reg-event-db
 ::slider-val
 (fn [db [_ song]]
   (-> db
       (assoc :slider-val song))))

(rf/reg-event-db
 ::split-frame-at
 (fn [db [_ frame-id ms]]
   (-> db
       (update-in [:current-song :frames]
                  (fn [frames]
                    (flatten
                     (map (fn [frame]
                            (if (= frame-id (:id frame))
                              (lf/split-frame-at frame ms)
                              frame))
                          frames)))))))

(rf/reg-event-db
 ::delete-frame
 (fn [db [_ frame-id]]
   (-> db
       (update-in [:current-song :frames]
                  (fn [frames]
                    (filter #(not= frame-id (:id %)) frames))))))

(rf/reg-event-db
 ::update-lyrics-event
 (fn [db [_ frame-id evt-id evt]]
   (-> db
       (update-in [:current-song :frames]
                  (fn  [frames]
                    (map (fn [frame]
                           (if (= frame-id (:id frame))
                             (update frame
                                     :events
                                     (fn [evts]
                                       (sort-by
                                        :offset
                                        (map (fn [event]
                                               (if (= evt-id (:id event))
                                                 evt
                                                 event))
                                             evts))))
                             frame))
                         frames))))))

(rf/reg-event-db
 ::set-val
 (fn [db [_ k v]]
   (-> db (assoc k v))))

(rf/reg-event-db
 ::push-modal
 (fn [db [_ m]]
   (update db :modals conj m)))

(rf/reg-event-db
 ::pop-modal
 (fn [db _]
   (update db :modals rest)))

(rf/reg-event-fx
 ::inc-playback-position
 (fn [{:keys [db]} _]
   {:db (if (:playing? db)
          (-> db
              (update :playback-position + 100))
          db)
    :fx [(when (:playing? db)
           [:dispatch-later {:ms 100 :dispatch [::inc-playback-position]}])]}))

(rf/reg-event-fx
 ::start-playback
 (fn [{:keys [db]} _]
   {:db
    (-> db
        (assoc :playing? true
               :playback-position 0))
    :fx [[:dispatch-later {:ms 100 :dispatch [::inc-playback-position]}]]}))

(rf/reg-event-db
 ::stop-playback
 (fn [db _]
   (assoc db :playing? false)))
