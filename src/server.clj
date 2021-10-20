(ns server
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found]]
            [muuntaja.middleware :as middleware]
            [org.httpkit.server :as server]))

(def accounts (atom {}))

(defn new-account [name]
  {:account-number (count @accounts)
   :name name
   :balance 0})

(defn account-creation [{:keys [body-params] :as _req}]
  (if-not (contains? body-params :name)
    {:status 400
     :headers {}
     :body {:reason "No name specified!!!"}}
    (let [{:keys [account-number] :as account} (new-account (:name body-params))]
      (swap! accounts assoc account-number account)
      {:status 200
       :headers {}
       :body account})))

(defroutes routes
  (POST "/account" [] account-creation)
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
