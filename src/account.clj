(ns account
  "A namespace that contains account logic.")

(def accounts (atom {}))

(defn new-account [name]
  (let [id (count @accounts)
        account {:account-number id
                 :name name
                 :balance 0}]
    (swap! accounts assoc id account)))

(defn account-operation
  "Returns a pair of [`success` `info`].

  If operation is valid `success` will be true and `info` will contain the
  account information. If the operation failed  `success` will be false
  and `info` will contain the reason for the failure."
  ([id operation] (account-operation id operation {}))
  ([id operation {:keys [amount] :as _opts}]
   (if-let [account (get @accounts id)]
     (case operation
       :retrieval [true account]
       (:deposit :withdraw)
       (if (or (nil? amount) (not (number? amount)) (<= amount 0))
         [false {:reason "No or badly specified amount!!!"}]
         (let [{:keys [balance] :as updated-account}
               (update account :balance (if (= operation :deposit) + -) amount)]
           (if (< balance 0)
             [false {:reason "Insufficiant balance!!!"}]
             (do
               (swap! accounts assoc id updated-account)
               [true updated-account]))))
       (throw (Exception. "No such operation!")))
     [false {:reason "No such account!!!"}])))

(comment
  (new-account "foo")
  (account-operation 1 :retrieval)
  (account-operation 1 :deposit {:amount 100})
  (account-operation 1 :withdraw {:amount 100})
  (account-operation 100 :withdraw {:amount 100})
  (account-operation 1 :adsfsadsf {:amount 100}))
