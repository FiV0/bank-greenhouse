(ns server-test
  (:require [account]
            [client]
            [clojure.test :refer [deftest is testing]]
            [db]
            [matcher-combinators.test]
            [server]
            [xtdb.api :as xt]))

(defn server-and-db-fixture [f]
  (server/stop-server)
  (server/-main)
  (f)
  (server/stop-server))

(clojure.test/use-fixtures :once server-and-db-fixture)

(defn db-fixture [f]
  (let [old-audit-log @account/audit-log
        old-node @db/node]
    (reset! account/audit-log {})
    (with-open [node (xt/start-node {})]
      (reset! db/node node)
      (f))
    (reset! db/node old-node)
    (reset! account/audit-log old-audit-log)))

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

(deftest audit-account-test
  (testing "creating account"
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 0, :account-number 0}}
                (client/http-post "/account" {:name "Mr. Black"})))
    (is (match? {:status 200
                 :body {:name "Mr. Turing", :balance 0, :account-number 1}}
                (client/http-post "/account" {:name "Mr. Turing"})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 200, :account-number 0}}
                (client/http-post "/account/0/deposit" {:amount 200})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 100, :account-number 0}}
                (client/http-post "/account/0/withdraw" {:amount 100})))
    (is (match? {:status 200
                 :body {:name "Mr. Black", :balance 50, :account-number 0}}
                (client/http-post "/account/0/send" {:amount 50 :account-number 1})))
    (is (match? {:status 200
                 :body [{:description "send to #1", :debit 50, :sequence 2}
                        {:description "withdraw", :debit 100, :sequence 1}
                        {:description "deposit", :sequence 0, :credit 200}]}
                (client/http-get "/account/0/audit")))
    (is (match? {:status 200
                 :body [{:description "receive from #0", :sequence 0, :credit 50}]}
                (client/http-get "/account/1/audit")))
    (is (match? {:status 200
                 :body {:name "Mr. Shannon", :balance 0, :account-number 2}}
                (client/http-post "/account" {:name "Mr. Shannon"})))
    (is (match? {:status 200
                 :body []}
                (client/http-get "/account/2/audit")))
    (is (match? {:status 400
                 :body {:reason "No such account!!!"}}
                (client/http-get "/account/asdf/audit")))))

(comment
  (clojure.test/run-tests)

  (server/-main)
  (client/http-get "/account/adsaf/deposit" {:amount 100})

  (client/http-post "/account" {:name "Mr. Black"})
  (client/http-post "/account" {:name "Mr. Turing"})
  (client/http-post "/account/0/deposit" {:amount 200})
  (client/http-post "/account/0/withdraw" {:amount 100})
  (client/http-post "/account/0/send" {:amount 50 :account-number 1})
  (client/http-get "/account/0/audit")
  (client/http-get "/account/1/audit")

  )
