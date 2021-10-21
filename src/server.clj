(ns server
  (:require [account]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found]]
            [muuntaja.middleware :as middleware]
            [org.httpkit.server :as server]))

(defn try-parse-long [s]
  (try
    (Long/parseLong s)
    (catch Exception _
      nil)))

(defn account-creation [{:keys [body-params] :as _req}]
  (if-not (contains? body-params :name)
    {:status 400
     :headers {}
     :body {:reason "No name specified!!!"}}
    {:status 200
     :headers {}
     :body (account/new-account (:name body-params))}))

(defn account-retrieval [{:keys [params] :as _req}]
  (let [id (try-parse-long (:id params))
        [success account-or-info] (account/account-operation id :retrieval)]
    {:status (if success 200 400)
     :headers {}
     :body account-or-info}))

(defn account-deposit [{:keys [params body-params] :as _req}]
  (let [id (try-parse-long (:id params))
        [success account-or-info] (account/account-operation id :deposit body-params)]
    {:status (if success 200 400)
     :headers {}
     :body account-or-info}))

(defn account-withdraw [{:keys [params body-params] :as _req}]
  (let [id (try-parse-long (:id params))
        [success account-or-info] (account/account-operation id :withdraw body-params)]
    {:status (if success 200 400)
     :headers {}
     :body account-or-info}))

(defn account-send [{:keys [params body-params] :as _req}]
  (let [id (try-parse-long (:id params))
        [success account-or-info] (account/account-operation id :send body-params)]
    {:status (if success 200 400)
     :headers {}
     :body account-or-info}))

(defroutes routes
  (POST "/account" [] account-creation)
  (GET "/account/:id" [] account-retrieval)
  (POST "/account/:id/deposit" [] account-deposit)
  (POST "/account/:id/withdraw" [] account-withdraw)
  (POST "/account/:id/send" [] account-send)
  (not-found "<h1>Page not found, I am very sorry.</h1>"))

(def app (middleware/wrap-format routes))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& {:keys [port] :as _args}]
  (reset! server (server/run-server #'app {:port (or port 8080)})))

(comment
  (-main)
  (stop-server))
