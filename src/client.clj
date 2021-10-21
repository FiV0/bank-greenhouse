(ns client
  (:require [clojure.string :as str]
            [muuntaja.core :as m]
            [org.httpkit.client :as http]))

(def address "http://localhost:8080")

(defn assoc-string-content-type [res]
  (update res :headers #(assoc % "Content-Type" (:content-type %))))

(defn http-get
  ([endpoint] (http-get address endpoint "application/json"))
  ([address endpoint] (http-get address endpoint "application/json"))
  ([address endpoint decoding]
   (let [{:keys [headers] :as resp} (-> @(http/get (str address endpoint) {:headers {"accept" decoding}})
                                        assoc-string-content-type)]
     (cond-> resp
       (and (:content-type headers) (str/starts-with? (:content-type headers) decoding))
       (assoc :body (m/decode-response-body resp))))))

(defn http-get-body [& args]
  (:body (apply http-get args)))

(comment
  (http-get-body "/account/0")
  (http-get-body "/account/100")
  (http-get-body "/account/adaa")
  (http-get-body "/garbage")
  (http-get-body "/account/1/audit")
  )

(defn http-post
  ([endpoint body] (http-post address endpoint "application/json" "application/json" body))
  ([address endpoint body] (http-post address endpoint "application/json" "application/json" body))
  ([address endpoint encoding decoding body]
   (let [{:keys [headers] :as resp} (-> @(http/post (str address endpoint) {:headers {"content-type" encoding
                                                                                      "accept" decoding}
                                                                            :body (m/encode encoding body)})
                                        assoc-string-content-type)]
     (cond-> resp
       (and (:content-type headers) (str/starts-with? (:content-type headers) decoding))
       (assoc :body (m/decode-response-body resp))))))

(defn http-post-body [& args]
  (:body (apply http-post args)))

(comment
  (http-post-body "/account" {:name "Mr. Black"})
  (http-post-body "/account" {})
  (http-post-body "/account/1/deposit" {:amount 100})
  (http-post-body "/account/adafas/deposit" {:amount 100})
  (http-post-body "/account/1/withdraw" {:amount 100})
  (http-post-body "/account/1/send" {:amount 100 :account-number 0})
  (http-post-body "/garbage" {})
  )
