# Scala Language Reference

First of all, the language specification is meant to be correct, precise and clear.

Second, editing, previewing and generating output for the markdown should be simple and easy.

Third, we'd like to support different output formats. An html page per chapter with MathJax seems like a good start, as it satisfies the second requirement, and enables the first one.

## Editing

At the time of writing we are using Jekyll 3.3.0 and [Redcarpet 3.3.2](https://github.com/vmg/redcarpet) to generate the html.

Check `Gemfile` for the current versions.

We aim to track the configuration GitHub Pages use but at times differences will arise as GitHub Pages evolves.

## Building

Travis CI builds the spec automatically after every merged pull release and publishes to http://www.scala-lang.org/files/archive/spec/2.13/.

To preview locally, run the following commands in the root of your checkout scala/scala:
`bundle install` to install Jekyll and `bundle exec jekyll serve -d build/spec/ -s spec/ -w --baseurl=""` to start it,
and open http://0.0.0.0:4000/ to view the spec. Jekyll will rebuild as you edit the markdown, but make sure to restart it when you change `_config.yml`.

## General Advice for editors

- All files must be saved as UTF-8: ensure your editors are configured appropriately.
- Use of the appropriate unicode characters instead of the latex modifiers for accents, etc. is necessary. For example, é instead of `\'e`.
- MathJAX errors  will appear within the  rendered DOM as span  elements with class `mtext` and style attribute `color: red` applied. It is  possible to search for this combination in the development  tools of the browser of your choice. In chrome, CTRL+F / CMD+F within the inspect element panel allows you to do this.

### Macro replacements:

- While  MathJAX just  support LaTeX style  command definition,  it is recommended  to not use  this as  it will likely cause issues with preparing the document for PDF or ebook distribution.
- `\SS` (which I could not find defined within the latex source) seems to be closest to `\mathscr{S}`
- `\TYPE` is equivalent to `\boldsymbol{type}'
- As MathJAX has  no support for slanted font (latex  command \sl), so in all instances  this should be replaced with \mathit{}
- The macro \U{ABCD} used for unicode character references can be replaced with \\uABCD.
- The macro \URange{ABCD}{DCBA} used for unicode character ranges can be replaced with \\uABCD-\\uDBCA.
- The macro \commadots can be replaced with ` , … , `.
- There is no adequate replacement for `\textsc{...}`  (small caps) in pandoc markdown. While unicode contains a number of  small capital  letters, it  is notably  missing Q and  X as  these glyphs  are intended  for phonetic spelling, therefore these  cannot be reliably used. For now,  the best option is to use  underscore emphasis and capitalise the text manually, `_LIKE THIS_`.

### Unicode Character replacements

- The unicode  left and right single  quotation marks (‘ and ’ (U+2018 and U+2019, respectively)) have been used in  place of ` and ', where the quotation marks  are intended to  be paired. These can  be typed on  a mac using  Option+] for a left  quote and Option+Shift+] for the right quote.
- Similarly for left and right double quotation marks (“ and ” (U+201C and U+201D, respectively)) in place of ". These can be typed on a mac using Option+[ and Option+Shift+].
