(ns docstring.friends)

(defn friends
  "Creates one-line ASCII moji. Featuring Shruggie and \"friends\".
   
   This function is a contrived example to use as a test function for
   the docstring lib. This is the :`desc` entry, which can be a multi-line
   string. The `docstring` engine will sort out the indentation for you.
   You can use things like quoted literal newline chars: \"\n\".
   
   Basic example
   ```clojure
   (friends)
   ```
   
   Basic example with result
   ```clojure
   (friends)
   ;; =>
   \"¯\\_(ツ)_/¯\"
   ```
   
   Multiple basic examples
   ```clojure
   (friends)
   
   (friends :happy)
   ```
   
   Multiple basic examples with results
   ```clojure
   (friends)
   ;; =>
   \"¯\\_(ツ)_/¯\"
   
   (friends :happy)
   ;; =>
   \"ヽ(・∀・)ﾉ\"
   ```
   
   Example with prefix and `:flipping` moji
   ```clojure
   (friends {:prefix \"Flip a table: \", :moji :flipping})
   ;; =>
   \"Flip a table: (╯°□°）╯︵ ┻━┻\"
   ```
   
   Illustrated example with inline comments.
   ```clojure
   (friends {:prefix \"Flip out\"
             :moji   :flipping    ; (╯°□°）╯︵ ┻━┻     
             ;; :moji   :crying      ; ಥ_ಥ    
             ;; :moji   :flipping    ; (╯°□°）╯︵ ┻━┻     
             ;; :moji   :happy       ; ヽ(・∀・)ﾉ
             ;; :moji   :excited     ; ヾ(≧▽≦*)o
             ;; :moji   :lenny       ; ( ͡° ͜ʖ ͡°)
             ;; :moji   :shruggie    ; ¯\\_(ツ)_/¯
             })```
   
   All the options:
   
   * **`:prefix`**
       - `string?`
       - Optional.
       - Text to prefix the moji
   
   * **`:moji`**
       - `#{:flipping :happy :shruggie :lenny :excited :crying}`
       - Optional.
       - Defaults to `:shruggie`.
       - The name of the friend. 
         [See more](https://pets.com)"
  {:tldr     "Creates one-line ASCII moji. Featuring Shruggie and \"friends\"."

   :desc     "This function is a contrived example to use as a test function for
              the docstring lib. This is the :`desc` entry, which can be a multi-line
              string. The `docstring` engine will sort out the indentation for you.
              You can use things like quoted literal newline chars: \"\\n\"."

   :examples [{:desc  "Basic example"
               :forms '[[(friends)]]}

              {:desc  "Basic example with result"
               :forms '[[(friends) "¯\\_(ツ)_/¯"]]}

              {:desc  "Multiple basic examples"
               :forms '[[(friends)]
                        [(friends :happy)]]}

              {:desc  "Multiple basic examples with results"
               :forms '[[(friends)
                         "¯\\_(ツ)_/¯"]
                        [(friends :happy)
                         "ヽ(・∀・)ﾉ"]]}

              {:desc  "Example with prefix and `:flipping` moji"
               :forms '[[(friends {:prefix "Flip a table: "
                                   :moji   :flipping})
                         "Flip a table: (╯°□°）╯︵ ┻━┻"]]}

              ^:no-doc
              {:desc  "Example to be left out of docstring, via `^:no-doc` metadata"
               :forms '[[(friends {:moji :lenny})
                         "( ͡° ͜ʖ ͡°)"]]}

              {:desc  "Illustrated example with inline comments."
               :forms "(friends {:prefix \"Flip out\"
                      |          :moji   :flipping    ; (╯°□°）╯︵ ┻━┻     
                      |          ;; :moji   :crying      ; ಥ_ಥ    
                      |          ;; :moji   :flipping    ; (╯°□°）╯︵ ┻━┻     
                      |          ;; :moji   :happy       ; ヽ(・∀・)ﾉ
                      |          ;; :moji   :excited     ; ヾ(≧▽≦*)o
                      |          ;; :moji   :lenny       ; ( ͡° ͜ʖ ͡°)
                      |          ;; :moji   :shruggie    ; ¯\\_(ツ)_/¯
                      |          })"}]

   :options  [:map
              {:desc "All the options"}
              [:prefix
               {:desc     "Text to prefix the moji"
                :optional true}
               :string]
              [:moji
               {:desc     "The name of the friend. \n[See more](https://pets.com)"
                :optional true
                :default  :shruggie}
               [:enum
                :crying
                :flipping
                :happy
                :excited
                :lenny
                :shruggie]]]}
  ([]
   (friends nil))
  ([{:keys [prefix moji]}]
   (let [mojis {:crying   "ಥ_ಥ"
                :flipping "(╯°□°）╯︵ ┻━┻"
                :happy    "ヽ(・∀・)ﾉ"
                :excited  "ヾ(≧▽≦*)o"
                :lenny    "( ͡° ͜ʖ ͡°)"
                :shruggie "¯\\_(ツ)_/¯"}
         moji  (if (contains? mojis moji) moji :shruggie)]
     (str prefix (get mojis moji)))))
