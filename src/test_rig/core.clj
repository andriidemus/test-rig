(ns test-rig.core
  (:use ring.adapter.jetty
        ring.middleware.file
        ring.middleware.content-type
        ring.middleware.not-modified)
  (:require [ring.util.response :refer [header]]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]])
  (:import [org.eclipse.jetty.server Server])
  (:gen-class))

(def default-config {:port 8080})
(def server (atom nil))
(def config (atom default-config))

(defn current-dir []
  (-> (new java.io.File "")
      (.getAbsolutePath)))

(defn build-config [config-filename]
  (let [config-file (new java.io.File config-filename)
        config (if (.exists config-file)
                 (-> (.getAbsolutePath config-file)
                     (slurp)
                     (edn/read-string))
                 default-config)
        config (update config :dir #(or % (current-dir)))]
    config))

(defn default-handler [request]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>404</title></head><body>Not found ¯\\_(ツ)_/¯</body></html>"})

(defn wrap-headers [handler]
  (fn [request]
    (let [response (handler request)]
      (pprint response)
      (reduce (fn [resp [k v]] (header resp k v)) response (:headers @config)))))

(defn app []
  (-> default-handler
      (wrap-file (:dir @config))
      (wrap-content-type)
      (wrap-not-modified)
      (wrap-headers)))

(defn start [join?]
  (swap! server (fn [^Server srv]
                  (when srv (.stop srv))
                  (run-jetty (app) {:port (:port @config)
                                  :join? join?}))))

(defn stop []
  (swap! server (fn [srv]
                  (when srv (.stop srv))
                  nil)))

(defn -main
  [& args]
  (let [config-filename (or (first args) "test-rig.edn")]
    (prn "Loading config from" config-filename)
    (reset! config (build-config config-filename)))
  (prn "Starting server with config:")
  (pprint @config)
  (start true))


; Repl
;
; (start false)
; (stop)
