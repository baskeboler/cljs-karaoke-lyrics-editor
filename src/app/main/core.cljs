(ns app.main.core
  (:require [clj-karaoke.protocols :as p]
            [cljs.spec.alpha :as specs]
            [clj-karaoke.lyrics-frame :as fe]
            [cljs.core.async :as async :refer [<! >! chan promise-chan]]
            ["electron" :as electron
             :refer [app
                     ipc
                     dialog
                     BrowserWindow
                     crashReporter]]
            ["fs" :as fs]))

(goog-define dev? false)
(specs/check-asserts false)

(def main-window (atom nil))

(defn init-browser []
  (reset! main-window (BrowserWindow.
                       (clj->js {:width 700
                                 :height 600})))
                                        ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js  @main-window "closed" #(reset! main-window nil)))

(defn main []
                                        ; CrashReporter can just be omitted
  (.start crashReporter
          (clj->js
           {:companyName "MyAwesomeCompany"
            :productName "MyAwesomeApp"
            :submitURL "https://example.com/submit-url"
            :autoSubmit false}))

  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser))

(defn select-song-lyrics []
  (let [select-promwise (-> dialog
                            (.showOpenDialog))
        res             (async/promise-chan)]
    (.. select-promwise
        (then (fn [file-selection]
                (js/console.log "result: " file-selection)
                (js->clj file-selection)))
        (then (fn [selection-map]
                (async/put! res selection-map)
                {:result :ok})))

    res))
