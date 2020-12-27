(ns app.renderer.player
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as recom]
            [app.renderer.subs :as subs]
            [clj-karaoke.protocols :as p]
            [clj-karaoke.song-data]
            [clj-karaoke.lyrics-event]
            [clj-karaoke.lyrics-frame]))

(defn player-panel []
  (let [position (rf/subscribe [::subs/select-value :playback-position])
        song     (rf/subscribe [::subs/current-song])]
    (fn []
      [recom/v-box
       :children
       [[recom/title
         :level :level1
         :label (doall
                 (for [evt  (:events (p/get-current-frame @song @position))
                       :let [evt-offset (- @position (:offset (p/get-current-frame @song @position)))
                             played? (p/played? evt evt-offset)]]
                   [:div
                    {:style (merge
                             {:display :contents
                              :transition "all 0.4s ease-in-out"}
                             (if played?
                               {:color       "red"
                                :font-weight 300
                                :text-shadow "2px 2px 10px grey"}
                               {}))}
                    (p/get-text evt)]))]]])))
