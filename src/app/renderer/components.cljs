(ns app.renderer.components
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-com.core :as recom]))

(defn card-title [title]
  [recom/title :level :level2 :label title])
(defn card [& {:keys [title body footer]}]
  (let [expanded? (reagent/atom true)]
    (fn [& {:keys [title body footer]}]
      [recom/border
       :radius "0.5em"
       :padding "0.5em"
       :style {:box-shadow "5px 2px 100px 3px lightgrey"
               :margin-bottom "1em"}
       :child
       [recom/v-box
        :children
        [[recom/h-box
          :align :center
          :children
          [[card-title title]
           [recom/gap :size "1"]
           [recom/md-icon-button
            :md-icon-name (if @expanded?
                            "zmdi-minus"
                            "zmdi-plus")
            :on-click #(swap! expanded? not)]]]
         (when  @expanded?
           [recom/line :style {:margin-bottom "0.5em"}])
         (when @expanded?
           body)
         (when (and @expanded? footer)
           [recom/v-box
            :children
            [[recom/line]
             footer]])]]])))


