(ns app.renderer.core
  (:require [cljs.nodejs :as nodejs]
            [reagent.core]
            [reagent.dom :refer [render]]
            [re-frame.core :as rf]
            [app.renderer.subs]
            [app.renderer.events]
            [app.renderer.db]
            [app.renderer.views :refer [ui]]))


(enable-console-print!)


;; (defn read-file  [f]
;;   (if (.existsSync ^js fs f)
;;     (let [data  (.readFileSync ^js fs f "utf8")
;;           song-map (reader/read-stri,sNng data)]
;;       song-map)))

(defonce electron ^js (nodejs/require "electron"))
                             
(defonce ^js ipc-renderer (-> electron (.-ipcRenderer)))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export init!
  []
  (rf/dispatch-sync [:initialize])
  (render [ui]
          (js/document.getElementById "app-container")))


;; (init!)

(defn ^:export select-file []
  (.. ^js ipc-renderer
      (invoke "open-karaoke-file")
      (then #(.log js/console "invoked main fn"))
      (catch #(.log js/console "failed to invoke main fn"))))


(def ^:export start init!)
