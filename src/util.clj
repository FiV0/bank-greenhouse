(ns util
  "Some utility functions.")

;; copied from clojure 1.11-alpha

(defn update-keys
  "m f => {(f k) v ...}
  Given a map m and a function f of 1-argument, returns a new map whose
  keys are the result of applying f to the keys of m, mapped to the
  corresponding values of m.
  f must return a unique key for each key of m, else the behavior is undefined."
  {:added "1.11"}
  [m f]
  (let [ret (persistent!
             (reduce-kv (fn [acc k v] (assoc! acc (f k) v))
                        (transient {})
                        m))]
    (with-meta ret (meta m))))
