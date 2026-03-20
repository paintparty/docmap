# docstring

A tool to generate Clojure docstrings from function metadata maps.

<br>

## Why

- Function metadata maps are just maps, they are easy to read and structurally edit.<br>
- Manually formatting structured data and/or markdown within Clojure docstrings can feel awkward.<br>
- Works great in tandem with [`quickdoc`](https://github.com/borkdude/quickdoc) for lo-fuss `API.md` generation.

<br>

## How

Just give your function a metadata map. **`docstring`** looks for the following entries:

<br>

**`:tldr`** <br>
A brief summary of the function's purpose. This should be a one-liner, useful
for readers if the `:desc` will be a detailed description over many lines. If
your `:desc` entry is brief, there is no need to provide a `:tldr` 

<br>

**`:desc`** <br>
A description of your function as a (potentially) multi-line string. Do not
include examples or options, use the `:examples` or `:options` entries for those.


<br>

**`:examples`** <br>
A vector of maps with the following entries:
- `:desc`   - Description of example. Required.
- `:forms`   - A quoted vector of vectors, or a multi-line string. Required. See example below for details.

<br>

**`:options`** <br>
A schema for a map of options, in [Malli](https://github.com/metosin/malli) syntax. Suitable when your function takes an options map for one of its args.

<br>

<!-- **`:schema`**<br>
A Malli schema of function args. This will document the args and return type. This will override the `:options` entry, so choose one or the other.

<br> -->

> [!NOTE]
> The use of Malli syntax here is a just a convention i.e. your project does not
> need to use Malli in order for `docstring` to work.

<br>

## Example

Below is a contrived example function with a metadata map.

A few notes on this example:
- The `:forms` entry in an `:examples` map must be a quoted vector of vectors.
This allows you to provide one or more example calls (and optional results) for
each example "group". Each child vector should contain an example function call
as the first element, with an optional second element being the resulting value
of the call.

- You can also provide this `:forms` entry as a multi-line string, for example if
you want to add inline comments as shown in the example below. If you provide a
multiline string, `|` characters should be used to control the
formatting / indentation of subsequent lines. These `|` chars will not be
included in the output.

- You can use `^:no-doc` on selected example groups to leave them out of the
generated docstring.

````Clojure 
(defn friends
  {:tldr               "Creates one-line ASCII moji. Featuring Shruggie and \"friends\"."
   :desc               "This function is a contrived example to use as a test function for
                        the docstring lib. This is the :`desc` entry, which can be a multi-line
                        string. The `docstring` engine will sort out the indentation for you.
                        You can use things like quoted literal newline chars: \"\\n\"." 

   :examples           [{:desc  "Basic example"
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

                        {:desc "Illustrated example with inline comments."
                         :forms "(friends {:prefix \"Flip out\"
                                |          :moji   :flipping    ; (╯°□°）╯︵ ┻━┻     
                                |          ;; :moji   :crying      ; ಥ_ಥ    
                                |          ;; :moji   :flipping    ; (╯°□°）╯︵ ┻━┻     
                                |          ;; :moji   :happy       ; ヽ(・∀・)ﾉ
                                |          ;; :moji   :excited     ; ヾ(≧▽≦*)o
                                |          ;; :moji   :lenny       ; ( ͡° ͜ʖ ͡°)
                                |          ;; :moji   :shruggie    ; ¯\\_(ツ)_/¯
                                |          })"}]

   :options            [:map
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
````
<br>

[Run `docstring` as a `bb` task](#usage-with-babashka), and it will format a docstring from your
function's metadata map and use [`rewrite-clj`](https://github.com/clj-commons/rewrite-clj) to update the function in your file with the new docstring:
             

````Clojure 
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
````

## Templating
For templating, you can provide a `:docstring/template` entry in your function's metadata map. This is useful for omitting certain things or specifying a custom order for the sections. For instance, you might want your `:examples` section to be above the `:desc` section.

In the example below, we are composing the docstring with the examples first, and using only the examples listed in the `:examples` vector

```Clojure
{...
 :docstring/template [[:examples
                       "Multiple basic examples"
                       "Example with prefix and `:flipping` moji"]
                      :desc
                      :options]
 ...
}              
```

## Usage with Babashka
Use as a [bb task](https://book.babashka.org/#tasks) by adding to your `:tasks` entry in a `bb.edn` file at your project's root:
```Clojure
{:tasks {docstring {:doc        "Invoke docstring "
                    :extra-deps {io.github.paintparty/docstring {:local/root "./../docstring/"}}
                    :task       (exec 'docstring.api/docstring)
                    :exec-args  {:source-paths ["src/my_project/core.cljc"
                                                "src/my_project/api.cljc"]
                                 :exclude [myproject.api/myfn]}}}}
```
Then run `bb docstring` from your project root.




## Status / Roadmap
Alpha, subject to change. Intended to be a very simple tool, so additional features will be kept to a minimum.
<br>

## Contributing
Issues for bugs, improvements, or features are very welcome. Please file an issue for discussion before starting or issuing a PR.

Copyright © 2026 Jeremiah Coyle

## License
See [LICENSE](LICENSE) 
