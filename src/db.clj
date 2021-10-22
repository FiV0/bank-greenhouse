(ns db
  (:require [clojure.java.io :as io]
	    [xtdb.api :as xt]))

(def db-path "resources/dev")

(defn start-xtdb! []
  #_(letfn [(kv-store [dir]
	      {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
	                  :db-dir (io/file dir)
	                  :sync? true}})]
      (xt/start-node
       {:xtdb/tx-log (kv-store (str db-path "/tx-log"))
        :xtdb/document-store (kv-store (str db-path "/doc-store"))
        :xtdb/index-store (kv-store (str db-path "/index-store"))}))
  (xt/start-node {}))

(def node (atom (start-xtdb!)))

(defn get-node [] @node)

(defn stop-xtdb! []
  (when-not (nil? @node)
    (.close @node)
    (reset! node nil)))

(defn q [query & args]
  (apply xt/q (xt/db (get-node)) query args))

(comment
  (start-xtdb!)
  (stop-xtdb!)

  (xt/entity-history (xt/db @node) 0 :desc {:with-docs? true})

  (xt/entity-history (xt/db @node) 1 :desc {:with-docs? true})

  (with-open [tx-log-iterator (xt/open-tx-log @node 0 true)]
    (iterator-seq tx-log-iterator))

  )
