(ns server
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found]]
            [muuntaja.middleware :as middleware]
            [org.httpkit.server :as server]))

(defn handler [{:keys [request-method headers body-params] :as _req}]
  {:status 200
   :headers {}
   :body (cond-> {:encoding (get headers "accept")
                  :type request-method}
           (= request-method :post)
           (assoc :body body-params))})

(defroutes routes
  (GET "/" [] handler)
  (POST "/" [] handler)
  (not-found "<h1>Page not found, I am very sorry.</h1>"))

(def app (middleware/wrap-format routes))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  (reset! server (server/run-server #'app {:port 8080})))

(comment
  (-main)
  (stop-server))
