(ns app.renderer.components
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-com.core :as recom]))

(defn card-title [title]
  [recom/title :level :level2 :label title])
(defn card [& {:keys [title body footer]}]
  [recom/border
   :radius "0.5em"
   :padding "0.5em"
   :style {:box-shadow "5px 2px 100px 5px black"}

   :child
   [recom/v-box
    :children
    [[card-title title]
     [recom/line :style {:margin-bottom "0.5em"}]
     body
     (when footer
       [recom/v-box
        :children
        [[recom/line]
         footer]])]]])


