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

(defn account-retrieval [{:keys [params] :as _req}]
  (if-let [account (get @accounts (Long/parseLong (:id params)))]
    {:status 200
     :headers {}
     :body account}
    {:status 400
     :headers {}
     :body {:reason "No such account!!!"}}))

(defn account-deposit [{:keys [params body-params] :as _req}]
  (let [id (Long/parseLong (:id params))
        amount (Long/parseLong (:amount body-params))
        account (get @accounts id)]
    (cond
      ;; no such account
      (nil? account)
      {:status 400
       :headers {}
       :body {:reason "No such account!!!"}}

      ;; bad amount
      (or (nil? amount) (not (number? amount)) (<= amount 0))
      {:status 400
       :headers {}
       :body {:reason "No or badly specified amount!!!"}}

      :else
      (let [updated-account (update account :balance + amount)]
        (swap! accounts assoc id updated-account)
        {:status 200
         :headers {}
         :body updated-account}))))

(defroutes routes
  (POST "/account" [] account-creation)
  (GET "/account/:id" [] account-retrieval)
  (POST "/account/:id/deposit" [] account-deposit)
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
