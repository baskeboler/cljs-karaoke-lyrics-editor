(ns app.renderer.core
  (:require [reagent.core :refer [atom]]
            [reagent.dom :refer [render]]
            [re-frame.core :as rf]
            ;; [cljs.tools.reader :as reader]
            ;; [clojure.string :as cstr]
            ;; ["devtron" :as devtron]
            [app.renderer.subs]
            [app.renderer.events]
            [app.renderer.db]
            [app.renderer.views :refer [ui]]))
;; [cljs.core.async :as a]))
            ;; ["electron" :as electron]))
;; (defonce electron (js/require "electron"))
;; (defonce nedb (js/require "nedb"))

;; (.install ^js devtron)       ;; we love https://github.com/binaryage/c  js-devtools)

(enable-console-print!)


;; (defn read-file  [f]
;;   (if (.existsSync ^js fs f)
;;     (let [data  (.readFileSync ^js fs f "utf8")
;;           song-map (reader/read-stri,sNng data)]
;;       song-map)))


;; (def ^js ipc-renderer (-> electron (.-ipcRenderer)))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export init!
  []
  (rf/dispatch-sync [:initialize])
  (render [app.renderer.views/ui]
          (js/document.getElementById "app-container")))

(init!)
