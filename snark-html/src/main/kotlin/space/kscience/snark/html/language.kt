package space.kscience.snark.html

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus

public val SiteBuilder.languages: Map<String, Meta>
    get() = siteMeta["site.languages"]?.items?.mapKeys { it.key.toStringUnescaped() } ?: emptyMap()

public val SiteBuilder.language: String
    get() = siteMeta["site.language"].string ?: "en"

public fun SiteBuilder.withLanguages(languageMap: Map<String, Meta>, block: SiteBuilder.(language: String) -> Unit) {
    languageMap.forEach { (languageKey, languageMeta) ->
        val prefix = languageMeta["prefix"].string ?: languageKey
        val routeMeta = Meta {
            "site.language" put languageKey
            "site.languages" put Meta {
                languageMap.forEach {
                    it.key put it.value
                }
            }
        }
        route(prefix, routeMeta = routeMeta) {
            block(languageKey)
        }
    }
}

public fun SiteBuilder.withLanguages(
    vararg language: Pair<String, String>,
    block: SiteBuilder.(language: String) -> Unit,
) {
    val languageMap = language.associate {
        it.first to Meta {
            "prefix" put it.second
        }
    }
    withLanguages(languageMap, block)
}

/**
 * The language key of this page
 */
public val WebPage.language: String get() = pageMeta["language"]?.string ?: pageMeta["site.language"]?.string ?: "en"

/**
 * Mapping of language keys to other language versions of this page
 */
public val WebPage.languages: Map<String, Meta>
    get() = pageMeta["languages"]?.items?.mapKeys { it.key.toStringUnescaped() } ?: emptyMap()

public fun WebPage.localisedPageRef(pageName: Name, relative: Boolean = false): String {
    val prefix = languages[language]?.get("prefix")?.string?.parseAsName() ?: Name.EMPTY
    return resolvePageRef(prefix + pageName, relative)
}