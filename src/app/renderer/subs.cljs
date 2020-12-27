(ns app.renderer.subs
  (:require [re-frame.core :as rf :refer [reg-sub subscribe]]))


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :time
  (fn [db _]     ;; db is current app state. 2nd unused param is query vector
    (:time db))) ;; return a query computation over the application state

(rf/reg-sub
  :time-color
  (fn [db _]
    (:time-color db)))

(rf/reg-sub
 ::current-song
 (fn [db _]
   (:current-song db)))
(rf/reg-sub
 ::slider-val
 (fn [db _]
   (:slider-val db)))

(rf/reg-sub
 ::frame-by-id
 :<- [::current-song]
 (fn [song [_ frame-id]]
   (->> song
        :frames
        (filter #(= frame-id (:id %)))
        first)))


(rf/reg-sub
 ::lyrics-event-by-id
 (fn [[_ frame-id _]]
   (rf/subscribe [::frame-by-id frame-id]))
 (fn [frame [_ _ evt-id]]
   (->> frame
        :events
        (filter #(= evt-id (:id %)))
        (first))))

(rf/reg-sub
 ::select-value
 (fn [db [_  & others]]
   (get-in db others)))
       
