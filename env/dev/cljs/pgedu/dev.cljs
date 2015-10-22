(ns ^:figwheel-no-load pgedu.dev
  (:require [pgedu.core :as core]
            [pgedu.utils :as pu]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url (pu/socket-url 3001 "/figwheel-ws")
  :jsload-callback core/mount-root)

(core/init!)
