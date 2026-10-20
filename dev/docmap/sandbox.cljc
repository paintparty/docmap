(ns foo.core)

(defn bar
  "prints baz"
  [{:keys [baz]
    :or   {baz :my-kw}}]
  (println baz))
