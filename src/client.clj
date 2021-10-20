(ns client
  (:require [muuntaja.core :as m]
            [org.httpkit.client :as http]))

(def address "http://localhost:8080")

(defn assoc-string-content-type [res]
  (update res :headers #(assoc % "Content-Type" (:content-type %))))

(defn http-get [decoding]
  (-> (http/get address {:headers {"accept" decoding}})
      deref
      assoc-string-content-type
      m/decode-response-body))

(comment
  (http-get "application/edn")
  (http-get "application/json")
  (http-get "application/transit+json")
  )

(defn http-post [encoding decoding body]
  (-> (http/post address {:headers {"content-type" encoding
                                    "accept" decoding}
                          :body (m/encode encoding body)})
      deref
      assoc-string-content-type
      m/decode-response-body))

(comment
  (http-post "application/edn" "application/edn" {:hello :edn})
  (http-post "application/json" "application/edn" {:hello :json+edn})
  (http-post "application/transit+json" "application/transit+json" {:hello :tranit+json})
  )
