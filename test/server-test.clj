(ns server-test
  (:require [account]
            [client]
            [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test]
            [server]))

(def address "http://localhost:8080")

(defn server-fixture [f]
  (server/stop-server)
  (server/-main)
  (f)
  (server/stop-server))

(clojure.test/use-fixtures :once server-fixture)

(defn db-fixture [f]
  (reset! account/accounts {})
  (f))

(clojure.test/use-fixtures :each db-fixture)

(deftest garbage-test
  (testing "testing a garbage endpoint"
    (is (match? {:status 404
                 :body "<h1>Page not found, I am very sorry.</h1>"}
                (client/http-get "/garbage")))))

(deftest create-account-test
  (testing "creating account"
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account" {:name "Mr. Black"})))
    (is (match? {:status 200
                 :body {:name "Mr. Turing", :balance 0, :account-number 1}}
                (client/http-post "/account" {:name "Mr. Turing"})))
    (is (match? {:status 400
                 :body {:reason "No name specified!!!"}}
                (client/http-post "/account" {})))))

(deftest retrieve-account-test
  (testing "creating account"
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account" {:name "Mr. Black"})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-get "/account/0")))
    (is (match? {:status 400
                 :body {:reason "No such account!!!"}}
                (client/http-get "/account/asdf")))))

(deftest deposit-account-test
  (testing "creating account"
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account" {:name "Mr. Black"})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 100, :account-number 0}}
                (client/http-post "/account/0/deposit" {:amount 100})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 100, :account-number 0}}
                (client/http-get "/account/0")))
    (is (match? {:status 400
                 :body {:reason "No or badly specified amount!!!"}}
                (client/http-post "/account/0/deposit" {})))
    (is (match? {:status 400
                 :body {:reason "No or badly specified amount!!!"}}
                (client/http-post "/account/0/deposit" {:amount (- 100)})))
    (is (match? {:status 400
                 :body {:reason "No such account!!!"}}
                (client/http-post "/account/asdf/deposit" {:amount 100})))))

(deftest withdraw-account-test
  (testing "creating account"
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account" {:name "Mr. Black"})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 100, :account-number 0}}
                (client/http-post "/account/0/deposit" {:amount 100})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account/0/withdraw" {:amount 100})))
    (is (match? {:status 400
                 :body {:reason "Insufficiant balance!!!"}}
                (client/http-post "/account/0/withdraw" {:amount 100})))
    (is (match? {:status 400
                 :body {:reason "No or badly specified amount!!!"}}
                (client/http-post "/account/0/withdraw" {})))
    (is (match? {:status 400
                 :body {:reason "No or badly specified amount!!!"}}
                (client/http-post "/account/0/withdraw" {:amount (- 100)})))
    (is (match? {:status 400
                 :body {:reason "No such account!!!"}}
                (client/http-post "/account/asdf/withdraw" {:amount 100})))))

(deftest send-account-test
  (testing "creating account"
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account" {:name "Mr. Black"})))
    (is (match? {:status 200
                 :body {:name "Mr. Turing", :balance 0, :account-number 1}}
                (client/http-post "/account" {:name "Mr. Turing"})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 100, :account-number 0}}
                (client/http-post "/account/0/deposit" {:amount 100})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 50, :account-number 0}}
                (client/http-post "/account/0/send" {:amount 50 :account-number 1})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 50, :account-number 0}}
                (client/http-get "/account/0")))
    (is (match? {:status 200
                 :body {:name "Mr. Turing", :balance 50, :account-number 1}}
                (client/http-get "/account/1")))
    (is (match? {:status 400
                 :body {:reason "Insufficiant balance!!!"}}
                (client/http-post "/account/0/send" {:amount 100 :account-number 1})))
    (is (match? {:status 400
                 :body {:reason "Invalid receiving account!!!"}}
                (client/http-post "/account/0/send" {:amount 50 :account-number 1000})))
    (is (match? {:status 400
                 :body {:reason "Invalid receiving account!!!"}}
                (client/http-post "/account/0/send" {:amount 50 :account-number 0})))
    (is (match? {:status 400
                 :body {:reason "Invalid receiving account!!!"}}
                (client/http-post "/account/0/send" {:amount 50})))
    (is (match? {:status 400
                 :body {:reason "No or badly specified amount!!!"}}
                (client/http-post "/account/0/send" {})))
    (is (match? {:status 400
                 :body {:reason "No or badly specified amount!!!"}}
                (client/http-post "/account/0/send" {:amount (- 100)})))
    (is (match? {:status 400
                 :body {:reason "No such account!!!"}}
                (client/http-post "/account/asdf/send" {:amount 100 :account-number 0})))))


(comment
  (clojure.test/run-tests)

  (server/-main)
  (client/http-get "/account/adsaf/deposit" {:amount 100})

  (client/http-post "/account" {:name "Mr. Black"})
  (client/http-post "/account/0/deposit" {:amount 100})
  (client/http-post "/account/0/withdraw" {:amount 100})
  (client/http-get "/account/0" {:name "Mr. Black"})
  (client/http-post "/account/0/send" {:amount 10 :account-number 1000})

  )
