(ns app.main.core
  (:require [clj-karaoke.protocols :as p]
            [cljs.spec.alpha :as specs]
            [clj-karaoke.song-data :as sd]
            [clj-karaoke.lyrics-frame :as lf]
            [clj-karaoke.lyrics-event :as le]
            [cljs.core.async :as async :refer [<! >! chan promise-chan]]
            ["electron" :as electron
             :refer [app
                     ipcMain
                     dialog
                     Menu
                     BrowserView
                     BrowserWindow
                     crashReporter]]
            ["fs" :as fs]))

(goog-define dev? false)
(specs/check-asserts false)

(def menu-template
  [{:label "File"
    :submenu [{:role :quit}]}
   {:label "Help"
    :submenu [{:role :help}
              {:role :about}]}])

(def main-window (atom nil))

(defn echo-fn [evt arg]
  (js/console.log "lalala "
                  (js/JSON.stringify arg))
  (set! (.-returnValue evt) "ok"))

(declare select-song-lyrics load-lyrics-file)

(defn setup-menu [] 
  (let [menu (.buildFromTemplate Menu (clj->js menu-template))]
    (.setApplicationMenu Menu menu)))
(defn ^:export init-browser []
  (reset! main-window (BrowserWindow.
                       #js {:width           700
                            :height          600
                            :backgroundColor "#2e2c29"
                            :webPreferences  #js {:nodeIntegration true}}))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js  @main-window "closed" #(reset! main-window nil))
  (.handle ^js ipcMain "open-karaoke-file" select-song-lyrics)
  (.handle ^js ipcMain "lalala" echo-fn))
  

(defn ^:export main []
                                        ; CrashReporter can just be omitted
  (.start crashReporter
          (clj->js
           {:companyName "GilDev"
            :productName "KaraokeLyricsEditor"
            :submitURL   "https://example.com/submit-url"
            :autoSubmit  false
            :compress    true}))

  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (..  app (whenReady) (then init-browser)))

(defn select-song-lyrics [^js evt]
  (let [select-promise (.showOpenDialog ^js/electron.Dialog dialog @main-window #js ["openFile"])]
    (.. select-promise
        (then (fn [file-selection]
                (js/console.log "result: " file-selection)
                (load-lyrics-file
                 (aget (.-filePaths file-selection) 0))))
        (then (fn [file-contents]
                (set! (.-resultValue evt)
                      file-contents)
                file-contents))
        (catch (fn [err]
                 (println "Failed to open file " err))))))

(defn promise->chan [promise]
  (let [c (promise-chan)]
    (.. promise
        (then (fn [result]
                (async/put! c result)))
        (catch (fn [err]
                 (async/close! c))))
    c))

(defn load-lyrics-file [file-path]
  (println "reading " file-path
           (.. fs
               (readFile file-path
                         (fn [result]
                           (println "read " result)
                           result)))))
