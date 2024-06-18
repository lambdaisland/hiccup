# Unreleased

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
