(ns client
  (:require [muuntaja.core :as m]
            [org.httpkit.client :as http]))

(def address "http://localhost:8080")

(defn assoc-string-content-type [res]
  (update res :headers #(assoc % "Content-Type" (:content-type %))))

(defn http-get
  ([decoding] (http-get "" decoding))
  ([endpoint decoding]
   (-> (http/get (str address endpoint) {:headers {"accept" decoding}})
       deref
       assoc-string-content-type
       m/decode-response-body)))

(comment
  (http-get "/account/0" "application/edn")
  (http-get "/account/100" "application/edn")
  )

(defn http-post
  ([endpoint body] (http-post endpoint "application/json" "application/json" body))
  ([encoding decoding body] (http-post "" encoding decoding body))
  ([endpoint encoding decoding body]
   (-> (http/post (str address endpoint) {:headers {"content-type" encoding
                                                    "accept" decoding}
                                          :body (m/encode encoding body)})
       deref
       assoc-string-content-type
       m/decode-response-body)))

(comment
  (http-post "/account" {:name "Mr. Black"})
  )
