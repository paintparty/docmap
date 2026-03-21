(ns docstring.sandbox)

(defn foo
  "hey
   
   Options:
   
   * **`:label-text`**
       - `string?`
       - Required.
       - The text of the floating annotation.
   
   * **`:label-style`**
       - `map?`
       - Optional.
       - Controls the style of the floating annotation
   
   * **`:label-offset`**
       - `pos-int?`
       - Optional.
       - Defaults to `3`.
       - Controls offset of the floating annotation"
  {:desc    "hey"
   :options [:map
             [:line-index
              {:required true
               :desc     "Index of the line to be labeled"}
              :pos-int]
             [:label-text
              {:required true
               :desc     "The text of the floating annotation."}
              :string]
             [:label-style
              {:optional true
               :desc     "Controls the style of the floating annotation"}
              :map]
             [:label-offset
              {:optional true
               :default  3
               :desc     "Controls offset of the floating annotation"}
              :pos-int]]}
  []
  nil)
