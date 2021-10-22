(ns account
  "A namespace that contains account logic."
  (:require [db]
            [util]
            [xtdb.api :as xt]))

(def audit-log (atom {}))

(defn get-account [id]
  (xt/entity (xt/db (db/get-node)) id))

(defn get-max-account-nb []
  (->> (xt/q (xt/db (db/get-node)) '{:find [(max id)]
                                     :where [[id :user/name]]})
       first first))

(defn transform-account [account]
  (util/update-keys account {:xt/id :account-number :user/balance :balance :user/name :name}))

(defn new-account [name]
  (let [id (if-let [id (get-max-account-nb)] (inc id) 0)
        account {:xt/id id
                 :user/name name
                 :user/balance 0}
        node (db/get-node)]
    (xt/submit-tx node [[::xt/put account]])
    (xt/sync node)
    (swap! audit-log assoc id '())
    (transform-account account)))

(defn amount-check [amount]
  (not (or (nil? amount) (not (number? amount)) (<= amount 0))))

(defn account-operation
  "Given a account `id` and an `operation`, returns a pair of [`success` `info`].
  An optional parameters map can be passed as third argument.

  If operation is valid `success` will be true and `info` will contain the
  account information. If the operation failed  `success` will be false
  and `info` will contain the reason for the failure.

  Operations include
  - :retrieval - return account information
  - :deposit - deposit an amount
  - :withdraw - withdraw an amount
  - :send - send an amount

  :deposit, :withdraw and :send need an additional amount as optional parameter.
  :send also needs a valid account number to deposit to."
  ([id operation] (account-operation id operation {}))
  ([id operation {:keys [amount account-number] :as _opts}]
   (if-let [account (get-account id)]
     (case operation
       :retrieval [true (transform-account account)]
       (:deposit :withdraw :send)
       (if-not (amount-check amount)
         [false {:reason "No or badly specified amount!!!"}]
         (let [{:user/keys [balance] :as updated-account}
               (update account :user/balance (if (= operation :deposit) + -) amount)
               receiving-account (get-account account-number)
               node (db/get-node)]
           (cond (< balance 0)
                 [false {:reason "Insufficiant balance!!!"}]
                 (and (= operation :send)
                      (or (nil? receiving-account)
                          (= id account-number)))
                 [false {:reason "Invalid receiving account!!!"}]
                 :else
                 (do
                   (if (= operation :send)
                     (xt/submit-tx node [[::xt/put updated-account]
                                         [::xt/put (update receiving-account :user/balance + amount)]])
                     (xt/submit-tx node [[::xt/put updated-account]]))
                   (xt/sync node)
                   [true (transform-account updated-account)]))))
       (throw (Exception. "No such operation!")))
     [false {:reason "No such account!!!"}])))

(defn- keyword->string [kw]
  (apply str (rest (str kw))))

(defn- audit-log-sequence-id [id]
  (-> (get @audit-log id) count))

(defn wrap-account-operation [& [id operation opts :as args]]
  (let [[success :as res] (apply account-operation args)]
    (when (and success (#{:deposit :withdraw :send} operation))
      (let [{:keys [amount account-number]} opts]
        (case operation
          (:deposit :withdraw)
          (swap! audit-log update id conj
                 {:sequence (audit-log-sequence-id id)
                  (if (= operation :deposit) :credit :debit) amount
                  :description (keyword->string operation)})
          :send
          (do
            (swap! audit-log update id conj
                   {:sequence (audit-log-sequence-id id)
                    :debit amount
                    :description (str "send to #" account-number)})
            (swap! audit-log update account-number conj
                   {:sequence (audit-log-sequence-id account-number)
                    :credit amount
                    :description (str "receive from #" id)})))))
    res))

(defn account-audit [id]
  (if-let [log (get @audit-log id)]
    [true log]
    [false {:reason "No such account!!!"}]))

(comment
  (new-account "foo")
  (account-operation 0 :retrieval)
  (account-operation 1 :retrieval)
  (account-operation 1 :deposit {:amount 100})
  (account-operation 1 :withdraw {:amount 100})
  (account-operation 100 :withdraw {:amount 100})
  (account-operation 1 :adsfsadsf {:amount 100})
  (account-operation 1 :send {:amount 100 :account-number 0})
  (account-operation 1 :send {:amount 100 :account-number 100})

  (account-audit 1)

  )
