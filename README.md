# Scout

Scout is a library for scanning through strings in a functional
way. There are many great parser libraries in Clojure, but I wrote
Scout to process text without having to fully parse it, either because
the text format made that difficult, or it would have been overkill
for that particular task. For example, it is used for parsing the
mustache templates in the
[Stencil](http://github.com/davidsantiago/stencil) library.

The key object in Scout is a Scanner object, which is an immutable
object associated with the string it is scanning and a position within
that string. There are two ways to make a Scanner: either create a new
one using the `scanner` function, or use a Scout function to create a
new Scanner as the result of a search from a previous Scanner. If a
Scanner is created as the result of a successful search, it will
contain a third piece of data, an object with information about the
parts of the string it matched and any regular expression groups that
were assigned as part of the match. Since Scanners are immutable, you
can easily start any number of searches from a given Scanner and refer
back to Scanner objects from earlier in the parsing process.

## Usage

[API Reference](http://davidsantiago.github.com/scout)

Suppose we wish to search a string for an emoticon. One way to do this would be

```clojure
user=> (require '[scout.core :as scout])
nil
user=> (-> (scout/scanner "Hi there. :)")
            (scout/scan-until #":-?([()PD])"))
#scout.core.Scanner{:src "Hi there. :)", :curr-loc 12, :match #scout.core.MatchInfo{:start 10, :end 12, :groups [":)" ")"]}}
```

Here we used the `scan-until` function to search the string for the
next occurrence of the regular expression we gave it. It found a match
at the end of the string, and returns a new scanner at position 12,
which is just past the end of the string (So `scout.core/end?` will
return true on the returned Scanner). Since there was a successful
match, there is an associated MatchInfo object telling us that the
match started at character 10, and ended at character 12, with the
groups the regular expression matched. The first group is always the
entire matching string, so that is ":)", and any following match
groups will be the ordered matches of the groups in the regular
expression (from left to right). In this case, the group we specified
to catch the mouth of the emoticon matched ")". Now we can grab that
information and figure out if the emoticon is happy:

```clojure
user=> (-> (scout/scanner "Hi there. :-)")
           (scout/scan-until #":-?([()PD])")
           (scout/groups)
           (nth 1)
           {")" :happy, "(" :sad, "P" :sad, "D" :happy})
:happy
```

There are many more functions in Scout to help you parse, including
functions to scan for a match only at a given position, look ahead,
find pieces of the string before and after a match, skip to the
beginning of a match, and more. Check the
[API Reference](http://davidsantiago.github.com/scout) for the
details.

## Obtaining

Add 

    [scout "0.1.1"]
    
to the `:dependencies` key of your Leiningen project map.

## License

Copyright © 2012 David Santiago

Distributed under the Eclipse Public License, the same as Clojure.
