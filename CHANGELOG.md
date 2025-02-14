# Unreleased

## Added

## Fixed

## Changed

# 0.14.67 (2025-02-03 / 26d417f)

## Added

- Added support for using multimethods as tags in hiccup expressions

# 0.13.64 (2025-01-29 / 82eaa64)

## Changed

- Normalize spacing in style attr with multiple properties.
  Where before the generated style attr might have "\n" or "   ",
  these are all now a simple " ", e.g. in-between multiple CSS properties.

# 0.12.59 (2025-01-29 / d1e76bd)

## Added

- Add multimethod `convert-attr-value` to support custom rendering per attribute key
- Add support for setting `lambdaisland.hiccup.kebab-prefixes` via a system
  property (comma or space separated)
- Add HTMX extension prefixes to the default set: `hx-`, `ws-`, `sse-`

# 0.10.51 (2024-09-05 / 3992111)

## Added

- Add `wrap-render`, a middleware for rendering HTML with content negotiation

# 0.9.48 (2024-06-18 / e43362c)

## Added

- Support custom attribute prefixes that use dashes

# 0.7.42 (2024-03-17 / 52532f5)

## Changed

- bump garden

# 0.6.39 (2024-03-17 / b3b2b6c)

## Changed

- switch to `com.lambdaisland/garden`

# 0.0.33 (2023-07-03 / 7aa06c6)

## Fixed

- Fix rendering of classes specified in tags

# 0.0.25 (2023-06-16 / 7c4925e)

## Changed

- Convert multi-word attributes to match Reagent + React's behaviour.

  data-, aria-, and hx- attributes remain kebab-case.
  html and svg attributes that are kebab-case in those specs, are converted to kebab-case

  BREAKING in some cases:

    Previously:
    :tab-index -> "tab-index"
    "fontStyle" -> "fontStyle"
    :fontStyle -> "fontStyle"

    Now:
    :tab-index -> "tabIndex"
    "fontStyle" -> "font-style"
    :fontStyle -> "font-style"

# 0.0.15 (2023-03-20 / c0a2d53)

## Fixed

- Correctly handle boolean attributes (false values are removed rather than being set to "false")

## Changed

# 0.0.4 (2021-10-28 / 8c0198a)

## Added

- Initial implementation
- fragment support
- component support (fn? in first vector position)
- unsafe-html support
