(ns client
  (:require [clojure.string :as str]
            [muuntaja.core :as m]
            [org.httpkit.client :as http]))

(def address "http://localhost:8080")

(defn assoc-string-content-type [res]
  (update res :headers #(assoc % "Content-Type" (:content-type %))))

(defn http-get
  ([decoding] (http-get "" decoding))
  ([endpoint decoding]
   (let [{:keys [headers] :as resp} @(http/get (str address endpoint) {:headers {"accept" decoding}})]
     (if-not (str/starts-with? (:content-type headers) decoding)
       (:body resp)
       (-> resp assoc-string-content-type m/decode-response-body)))))

(comment
  (http-get "/account/0" "application/edn")
  (http-get "/account/100" "application/edn")
  (http-get "/account/adaa" "application/edn")
  (http-get "/garbage" "application/edn")
  )

(defn http-post
  ([endpoint body] (http-post endpoint "application/json" "application/json" body))
  ([encoding decoding body] (http-post "" encoding decoding body))
  ([endpoint encoding decoding body]
   (let [{:keys [headers] :as resp} @(http/post (str address endpoint) {:headers {"content-type" encoding
                                                                                  "accept" decoding}
                                                                        :body (m/encode encoding body)}) ]
     (if-not (str/starts-with? (:content-type headers) decoding)
       (:body resp)
       (-> resp assoc-string-content-type m/decode-response-body)))))

(comment
  (http-post "/account" {:name "Mr. Black"})
  (http-post "/account/1/deposit" {:amount "100"})
  (http-post "/account/adafas/deposit" {:amount "100"})
  (http-post "/account/1/withdraw" {:amount "100"})
  (http-post "/account/1/send" {:amount "100"})
  (http-post "/garbage" {})
  )
