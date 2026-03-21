(ns docstring.api
  "API namespace for docstring"
  (:require [clojure.string :as string]
            [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [cljfmt.core]
            [fireworks.core :refer [? !? ?> !?>]]
            [bling.banner :refer [banner]]
            [bling.core :refer [callout bling print-bling]]
            [bling.hifi :refer [hifi]]
            [bling.explain :refer [explain-malli]]
            [bling.fonts.miniwi :refer [miniwi]]
            [malli.core :as malli]
            [me.flowthing.pp :as pp]))

(defn  when->
  "If `(= (pred x) true)`, returns x, otherwise nil.
   Useful in a `clojure.core/some->` threading form."
  [x pred]
  (when (or (true? (pred x))
            (when (set? pred) (contains? pred x)))
    x))

(defn- join-lines
  ([coll]
   (join-lines "\n" coll))
  ([sep coll]
   (string/join sep coll)))


(defn- format-pred [pred]
  (cond (keyword? pred)
        (-> pred name (str "?") symbol)
        (and (vector? pred) (= (first pred) :enum))
        (->> pred rest (into #{}) str)
        :else
        pred))


(defn- md-code [s]
  (str "`" s "`"))


(defn- md-code-bold [s]
  (str "**`" s "`**"))


(defn- subline [s]
  (str "    - " s))


(defn- adjusted-indentation [s]
  (let [re #"\n( +)"
        n  (some->> s
                    str
                    (re-seq re)
                    (group-by #(count (second %)))
                    keys
                    (apply min))
        f  (fn [[a]] (str "\n" (subs a (inc n))))
        s  (string/replace s re f)]
    s))


(defn- tldr->str [s]
  (some-> s adjusted-indentation))


(defn- desc->str [s]
  (some-> s adjusted-indentation))


(defn- sublines [{:keys [optional desc default]} pred]
  (remove
   nil?
   [(subline (md-code (format-pred pred)))
    (subline (if (= optional true) "Optional." "Required."))
    (when default
      (subline (str "Defaults to `" default "`.")))
    (when-let [s (cond (and (vector? desc) (every? string? desc))
                       (string/join " " desc)

                       (and (map? desc)
                            (and (:bullet-text desc)
                                 (:sub-bullet-level desc)
                                 (:sub-bullets desc)))
                       (str (:bullet-text desc)
                            "\n"
                            (join-lines
                             (mapv #(str "        - " %)
                                   (:sub-bullets desc))))

                       (string? desc)
                       (desc->str desc))]
      (-> s
          (string/replace #"\n" "\n      ")
          subline))]))


(defn- options->md [options]
  (let [{:keys [desc]
         :as   m}             (nth options 1 nil)
        all-the-options       (if m (drop 2 options) (rest options))
        options-section-label (str (or desc "Options") ":")]
    [options-section-label
     (reduce (fn [s [option m pred]]
               (str s
                    "* " (md-code-bold option)
                    "\n"
                    (join-lines (sublines m pred))
                    "\n\n"))
             ""
             all-the-options)]))

(defn dequote-list [x]
  (if (and (list? x)
           (= (first x) 'quote))
    (second x)
    x))

(defn dequote-vec [x]
  (if (and (list? x)
           (= (first x) 'quote))
    (second x)
    x))

;; For `example->md` -----------------------------------------------------------

;; TODO - Add option for processing (:form example), if multiline string with comments etc

;; {:examples [{:form "(callout {:type                   :error
;;                    |          ;; :colorway            :purple           ; <- any bling palette color, overrides :type
;;                    |          ;; :label               \"My label\"      ; overrides label assigned by :theme
;;                    |          :side-label             \"My side label\" ; must have a :label if you want a :side-label        
;;                    |          :theme                  :sideline         ; :sideline :sideline-bold :minimal :gutter
;;                    |          :label-theme            :minimal          ; :minimal :marquee
;;                    |          ;; :padding-top         0                 
;;                    |          ;; :padding-left        2                 
;;                    |          ;; :padding-bottom      0                 
;;                    |          ;; :padding-right       0                 
;;                    |          ;; :margin-top          1                 
;;                    |          ;; :margin-botom        0                 
;;                    |          ;; :margin-left         0                 
;;                    |          ;; :data?               true              ; <- just returns string, no printing
;;                    |          })"
;;                 }]}


;; (println (string/replace
;;           (string/join "\n"
;;                        (string/split (:form wtf) 
;;                                      #"\n *\|"))
;;           #"\""
;;           "\\\\\""))

(defn- vec-of-strings?
  "Returns true if x is a vector where every element is a string."
  [x]
  (and (vector? x) (every? string? x)))

(defn- escape-escaped-double-quote [s]
  (string/replace s #"\"" "\\\""))

(defn- escape-escaped-backslashes [s]
  (string/replace s #"\\" "\\\\\\\\"))

(defn- example->md [example]
  (let [{:keys [desc forms]} example
        desc+forms?         (boolean (and desc forms))
        no-doc?             (-> example meta :no-doc)]

    (!? {:example    example
         :no-doc?    no-doc?
         :desc+form? desc+forms?})

    (when-not no-doc?
      (when desc+forms?
        (str (desc->str desc)
             "\n"
             "```clojure\n"
             (if (string? forms)

               ;; Example form (and maybe result) supplied as a potentially multi-line string
               (-> (string/join "\n" (string/split forms #"\n *\|"))
                   escape-escaped-double-quote
                   escape-escaped-backslashes)

               ;; Example form(s) (and maybe corresponding result(s)) supplied as a vector or 1 or 2 element vectors
               ;; E.g.
               ;; [[(my-adder 1 2) 3]
               ;;  [(my-adder 1 2 3) 6]]
               (string/join 
                "\n"
                (mapv (fn [[form result :as vc]]
                        (let [result 
                              (if (= 2 (count vc)) 
                                result
                                ::result-unsupplied)]
                          (str (-> form
                                   (pp/pprint {:max-width 70})
                                   with-out-str)
                               (when-not (= result ::result-unsupplied)
                                 (str ";; =>\n"
                                      (-> result
                                          (pp/pprint {:max-width 24})
                                          with-out-str))))))
                      (dequote-vec forms))))
             "```")))))

(defn- docsgen-section
  "Reducing function to generate a section of the docstring based on the
   docstring template."
  [{:keys [tldr desc examples options]} acc x]
  (cond
    (= x :tldr)
    (or (some->> tldr desc->str (conj acc))
        acc)

    (= x :desc)
    (or (some->> desc desc->str (conj acc))
        acc)

    (= x :examples)
    (if (seq examples)
      (conj acc (string/join "\n\n" (keep #(example->md %) examples)))
      acc)

    (= x :options)
    (if (seq options)
      (into acc (options->md options))
      acc)))


(defn metadata-map->docstring
  "Experimental utility for repl usage.
   This is also called internally from `docstring.api/docstring`, when docstring
   is initiated via a bb task.

   Expects a map, usually a metadata map of a function.

   See `docstring.api/docstring` docs for more info on the shape of this map.

   Returns a string intended to serve as the function's docstring.
   
   Resulting docstrings are optimized for consumption by
   [quickdoc](https://github.com/borkdude/quickdoc)."
  [{:keys [:docstring/template] :as m}]
  (let [template (or template [:tldr :desc :examples :options])
        sections (reduce (partial docsgen-section m) 
                         []
                         template)]
    (-> (string/join "\n\n" sections)
        (string/split #"\n")
        (->> (map-indexed (fn [i x] (str (if (zero? i) "  " "   ") x)))
             join-lines)
        (string/replace #"^  " ""))))


(defn docstring->string-node
  "Removes leading and trailing double quote chars, splits string on newline
   char, and creates string node."
  [s]
  (some-> s
          (string/replace #"^  \"|\"$" "")
          (string/split #"\n")
          n/string-node))


(defn insert-docstring
  "Inserts a docstring between function name and metadata."
  [zloc existing-docstring s]
  (if existing-docstring
    (-> zloc
        (z/right)                              ; move to fn name
        (z/right)                              ; move to docstring
        (z/replace (docstring->string-node s)) ; insert docstr before metadata
        (z/up))
    (-> zloc
        (z/right)                                  ; move to fn name
        (z/right)                                  ; move to metadata or params
        (z/insert-left (docstring->string-node s)) ; insert before metadata
        (z/insert-left (n/newline-node "\n"))      ; add newline for formatting
        (z/up))))                                  ; go back to defn level


(defn process-defn
  "Processes a single defn or defmacro form, adding docstring if it has :desc metadata."
  [zloc stats ns-sym exclude]
  (if (and (z/list? zloc)
           (contains? #{'defn 'defmacro}  (z/sexpr (z/down zloc))))
    (let [fn-name  (z/sexpr (-> zloc z/down z/right))
          fqfn-sym (symbol (str ns-sym "/" fn-name))]
      (if-not (contains? exclude fqfn-sym)
        (let [a                               
              (-> zloc z/down z/right z/right z/sexpr)

              b                               
              (-> zloc z/down z/right z/right z/right z/sexpr)

              existing-docstring              
              (when (string? a) a)

              {:keys [desc]
               :as   fn-metadata} 
              (if existing-docstring b a)]

          (if desc
            (let [new-docstring  (metadata-map->docstring fn-metadata)
                  new-docstring* (-> new-docstring
                                     (string/replace #"\"" "\\\\\""))

                  ;; TODO this seems to not detect if there is an escaped backslash in docstring
                  ;; Meaning false positive and reports that docstring was changed, even if it was changed to same thing
                  
                  no-change?     (= new-docstring* existing-docstring)
                  updated-zloc   (insert-docstring (z/down zloc)
                                                   existing-docstring
                                                   new-docstring*)
                  updated-stats  (if no-change?
                                   stats
                                   (update stats
                                           :updated
                                           conj
                                           (symbol (str ns-sym "/" fn-name))))]

              [updated-zloc updated-stats])
            [zloc stats]))
        [zloc stats]))
    [zloc stats]))


(defn- traverse-zipper [zloc ns-sym exclude]
  (loop [loc   zloc
         stats {:updated []}]
    (if (z/end? loc)
      [loc stats]
      (let [[new-loc new-stats]
            (process-defn loc stats ns-sym exclude)]
        (recur (z/next new-loc) new-stats)))))


(def title-in-ascii-quotes
  (bling [:white.italic ""]))

(defn- print-report [coll opts]
  #_(println (banner {:text            "DOCSTRING"
                      :font            miniwi
                      :gradient-colors [:green :blue]
                      :letter-spacing  2}))

  (when (:print-report-banner? opts)
    (print-bling
     (let [ascii-quote-style :subtle]
       [ascii-quote-style
        (str "
      0      0            0000   0000
    00     00            000000 000000
  0000   0000          "  (bling [ascii-quote-style "   0000   0000
 000000 000000            00     00
  0000   0000            0      0   
 "]))])))


  (callout {:colorway       :subtle
            :label          (bling [:subtle.italic "Docstring generation options"])
            :label-margin-left 1
            ;; :padding-top    1
            ;; :padding-bottom 0
            :padding-left   4
            :margin-top     1
            :margin-left    0
            :theme          :sideline
            :border-style   :none
            }
           (bling.hifi/hifi (dissoc opts :print-report? :print-report-banner?)
                            {:theme     "Universal Neutral"
                             :truncate? false}))

  (callout {:colorway       :subtle
            :label          (bling [:subtle.italic "Docstring generation report"])
            :label-margin-left 0
            ;; :padding-top    1
            ;; :padding-bottom 0
            :padding-left   4
            :margin-top     1
            :margin-left    0
            :theme          :sideline
            :border-style   :none
            }
           (if (some #(-> % :stats :updated seq) coll)
             (string/join
              "\n\n"
              (for [{:keys             [source-path]
                     {:keys [updated]} :stats}
                    coll]
                (str (bling [:blue source-path] "\n")
                     (when (seq updated)
                       (let [n      (count updated)
                             suffix (when (> n 1) "s")
                             s      (bling [:subtle (str "  Updated " n " docstring" suffix ":")])]
                         (str s
                              "\n"
                              (string/join "\n"
                                           (for [fn-name updated]
                                             (str "  - " (bling [:green fn-name])))) ))))))
             "No docstrings to update.")))

(defn first-ns-form [zloc]
  (some-> zloc
          (z/find z/next
                  (fn [loc]
                    (and (z/list? loc)
                         (= 'ns (z/sexpr (z/down loc))))))
          z/sexpr))


;; TODO - Don't process a defined if it is ignored with #_
;; TODO - Warn if malformed entry in fn metadata map e.g. `:form` vs `:forms`
;; TODO - Warn if example supplied as string ends with "```, but its not on its own line" 
;; TODO - Make sure this works with non-top level defs in reader conds
;; TODO - Make sure this works with macros
;; TODO - Example ala-carte use on a per-function basis by sending map to repl.
;;        Suggested use is copy to clipboard then paste.

(defn docstring
  {:desc ["Adds or updates docstrings on functions and macros, using existing function metadata." "\n\n"

          "Expects a valid source path, and an optional map of options." "\n\n"

          "Optionally prints a report." "\n\n"

          "Returns a map of :source-path and :updated-functions."]
   :options [:map
             [:source-paths {:desc "A vector of file paths representing namespaces to process with docstring."
                             :example ["src/my_project/core.cljc"
                                       "src/my_project/api.cljc"]}
              [:vector :string]]
             [:exclude 
              {:desc     "A vector of fully-qualified symbols representing functions to exclude from docstring generation"
               :example  ['my-project/my-fn]
               :optional true}
              [:vector :symbol]]
             [:print-report? 
              {:desc     "Prints a report with source path and updated-functions."
               :optional true
               :default  true}
              :boolean]
             [:print-report-banner?
              {:desc     "Prints an ascii art banner of typographer's double quotes."
               :optional true
               :default  true}
              :boolean]
             [:cljfmt-options {:desc     "See `cljfmt` [Formatting Options](https://github.com/weavejester/cljfmt?tab=readme-ov-file#formatting-options)."
                               :optional true
                               :default  true}
              :map]]}
  [opts]
  (if-not (malli/validate (-> docstring var meta :options) opts)
    (explain-malli (-> docstring var meta :options)
                   opts
                   {:file-info-str "docstring.api"
                    :preamble-section-label "Message:"
                    :preamble-section-body (bling "An invalid value was supplied to "
                                                  [:blue "docstring.api/docstrings"])})
    (let [{:keys [source-paths print-report? exclude cljfmt-options]}
          opts

          exclude
          (if (seq exclude) (into #{} exclude) #{})

          updated-contents
          (mapv (fn [source-path]
                  (let [zloc
                        (z/of-file source-path)

                        [_ ns-sym]
                        (first-ns-form zloc)

                        [final-zloc stats] 
                        (traverse-zipper zloc ns-sym exclude)

                        updated-content    
                        (-> final-zloc
                            z/root-string
                            (cljfmt.core/reformat-string 
                             (merge {:indent-line-comments? true}
                                    (when (map? cljfmt-options)
                                      cljfmt-options))))]
                    {:source-path     source-path
                     :updated-content updated-content
                     :stats           stats}))
                source-paths)]

      (doseq [{:keys [source-path
                      updated-content]}
              updated-contents]

        ;; Sanity check for dev
        #_(println updated-content)

        ;; Write updated content to file
        (spit source-path updated-content))

      (when-not (false? print-report?)
        (print-report updated-contents opts))

      source-paths)))


;; TODO - detect duplicate options
