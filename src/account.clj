(ns account
  "A namespace that contains account logic.")

(def accounts (atom {}))
(def audit-log (atom {}))

(defn new-account [name]
  (let [id (count @accounts)
        account {:account-number id
                 :name name
                 :balance 0}]
    (swap! accounts assoc id account)
    (swap! audit-log assoc id '())
    account))

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
   (if-let [account (get @accounts id)]
     (case operation
       :retrieval [true account]
       (:deposit :withdraw :send)
       (if-not (amount-check amount)
         [false {:reason "No or badly specified amount!!!"}]
         (let [{:keys [balance] :as updated-account}
               (update account :balance (if (= operation :deposit) + -) amount)]
           (cond (< balance 0)
                 [false {:reason "Insufficiant balance!!!"}]
                 (and (= operation :send)
                      (or (nil? (get @accounts account-number))
                          (= id account-number)))
                 [false {:reason "Invalid receiving account!!!"}]
                 :else
                 (do
                   (if (= operation :send)
                     (swap! accounts assoc
                            id updated-account
                            account-number (update (get @accounts account-number) :balance + amount))
                     (swap! accounts assoc id updated-account))
                   [true updated-account]))))
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
                    :description (str "receive from #" account-number)})
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
