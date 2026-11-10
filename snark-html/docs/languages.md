## Multi-language sites

`multiLanguageSite` renders the same site layout once per language, each with its own data:

```kotlin
val mySite = HtmlSite {
    static("assets")      // shared files, linked with resolveAssetRef

    multiLanguageSite(
        Language("en"),   // default language
        Language("ru"),
    ) {
        page("index") {
            body { fragment(data.resolveHtml("about")) }
        }
    }
}
```

### Data layout

The language of a data item is taken from its `language` meta field or from the first segment of its path:

```
data/
  about.md        # default language
  ru/
    about.md      # Russian version of `about`
  assets/
    logo.png      # shared by all languages
```

Items without a language are available in all languages. Items missing in a language are taken from the default
language.

### Language options

| Property   | Default          | Meaning                                    |
|------------|------------------|--------------------------------------------|
| `key`      | —                | Language key, for example `en`             |
| `route`    | the key          | Route of the language site                 |
| `dataPath` | the key          | Data branch with the language-specific data |

```kotlin
Language("en", route = Name.EMPTY) // serve the default language at the site root
```

### Pages

* `page.language` is the language key of the current page.
* `languageMap` lists language versions of the current page, if known.
* `resolveAssetRef(path)` links to a file shared between languages instead of the language site.

Short translated strings can be defined in code or in page metadata:

```kotlin
val greeting = LocalizedString.byLanguage("en" to "Hello", "ru" to "Привет")
val footer = LocalizedString.byResource("footer", defaultValue = "")  // pageMeta: localization.<language>.footer

body { +greeting.value() }
```
