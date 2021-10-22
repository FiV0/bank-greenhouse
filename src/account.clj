(ns account
  "A namespace that contains account logic."
  (:require [db]
            [util]
            [xtdb.api :as xt]))

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

(defn contains-user-id? [transactions id]
  (some (fn [[_op m]] (= id (:xt/id m))) transactions))

(defn get-transactions [id]
  (with-open [tx-log-iterator (xt/open-tx-log (db/get-node) nil true)]
    (->> (iterator-seq tx-log-iterator)
         (filter #(contains-user-id? (:xtdb.api/tx-ops %) id))
         (map :xtdb.api/tx-ops))))

(defn get-snapshots [id]
  (->> (get-transactions id)
       (map (fn [ops] (map (fn [[_op data]] data) ops)))))

(defn get-audit-log [id]
  (let [[previous-snapshot & remaining-snapshots] (get-snapshots id)]
    (loop [prev (first previous-snapshot) [cur & rem] remaining-snapshots log []]
      (if (nil? cur)
        (reverse (map-indexed #(assoc %2 :sequence %1) log))
        (cond (= 1 (count cur))
              (let [diff (- (:user/balance (first cur)) (:user/balance prev) )
                    res (if (pos? diff)
                          {:credit diff
                           :description "deposit"}
                          {:debit (- diff)
                           :description "withdraw"})]
                (recur (first cur) rem (conj log res)))
              (= 2 (count cur))
              (let [[[account] [other]] ((juxt filter remove) #(= id (:xt/id %)) cur)
                    diff (- (:user/balance account) (:user/balance prev))
                    res (if (pos? diff)
                          {:credit diff
                           :description (str "receive from #" (:xt/id other))}
                          {:debit (- diff)
                           :description (str "send to #" (:xt/id other))})]
                (recur account rem (conj log res)))
              :else
              (throw (Exception. "Should not happen!!!")))))))

(defn account-audit [id]
  (if-let [_account (get-account id)]
    [true (get-audit-log id)]
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
