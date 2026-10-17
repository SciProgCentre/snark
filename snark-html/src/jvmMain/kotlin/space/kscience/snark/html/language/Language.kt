package space.kscience.snark.html.language

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.actions.transform
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataBuilderScope
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.branch
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.*
import space.kscience.dataforge.workspace.filterByType
import space.kscience.snark.DataTreeWithDefault
import space.kscience.snark.html.*
import space.kscience.snark.html.language.Language.Companion.SITE_LANGUAGE_KEY
import space.kscience.snark.html.language.Language.Companion.SITE_LANGUAGE_MAP_KEY
import kotlin.reflect.typeOf


public class Language : Scheme() {

    /**
     * Language key override
     */
    public var key: String by string { error("Language key is not defined") }

    /**
     * Data location
     */
    public var dataPath: Name by value<Name>(
        reader = { (it?.string ?: key).parseAsName(true) },
        writer = { it.toString().asValue() }
    )

    /**
     * Page name prefix override
     */
    public var route: Name by value<Name>(
        reader = { (it?.string ?: key).parseAsName(true) },
        writer = { it.toString().asValue() }
    )

//    /**
//     * An override for data path. By default uses [prefix]
//     */
//    public var dataPath: String? by string()
//
//    /**
//     * Target page name with a given language key
//     */
//    public var target: Name?
//        get() = meta["target"].string?.parseAsName(false)
//        set(value) {
//            meta["target"] = value?.toString()?.asValue()
//        }

    public companion object : SchemeSpec<Language>(::Language) {

        public val LANGUAGE_KEY: Name = Name.of("language")

        public val LANGUAGE_MAP_KEY: Name = Name.of("languageMap")

        public val SITE_LANGUAGE_KEY: Name = SiteContext.Companion.SITE_META_KEY + LANGUAGE_KEY

        public val SITE_LANGUAGE_MAP_KEY: Name = SiteContext.Companion.SITE_META_KEY + LANGUAGE_MAP_KEY

        public const val DEFAULT_LANGUAGE: String = "en"
    }
}

public fun Language(
    key: String,
    route: Name = key.parseAsName(true),
    modifier: Language.() -> Unit = {},
): Language = Language {
    this.key = key
    this.route = route
    modifier()
}

internal val Data<*>.language: String?
    get() = meta[Language.LANGUAGE_KEY].string?.lowercase()

context(site: SiteContext)
public val languageMap: Map<String, Language>
    get() = site.siteMeta[SITE_LANGUAGE_MAP_KEY]?.items?.map {
        it.key.toStringUnescaped() to Language.read(it.value)
    }?.toMap() ?: emptyMap()

public val SiteContext.language: String
    get() = siteMeta[SITE_LANGUAGE_KEY].string ?: Language.DEFAULT_LANGUAGE

/**
 * Walk the data tree depth-first
 */
private fun <T> DataTree<T>.walk(
    namePrefix: Name = Name.EMPTY,
): Sequence<Pair<Name, DataTree<T>>> = sequence {
    yield(namePrefix to this@walk)
    items.forEach { (token, tree) ->
        yieldAll(tree.walk(namePrefix + token))
    }
}

private class LanguageMapAction(val languages: Set<Language>) : AbstractAction<Any, Any>(typeOf<Any>()) {

    override fun DataBuilderScope<Any>.generate(
        source: DataTree<Any>,
        meta: Meta
    ): Map<Name, Data<Any>> = buildMap {
        val languageMapCache = mutableMapOf<Name, MutableMap<Language, DataTree<Any>>>()

        source.walk().forEach { (name, node) ->
            val language = node.data?.language?.let { itemLanguage -> languages.find { it.key == itemLanguage } }
            if (language == null) {
                // put data without a language into all buckets
                languageMapCache[name] = languages.associateWithTo(HashMap()) { node }
            } else {
                // collect data with language markers
                val nameWithoutPrefix = if (name.startsWith(language.dataPath)) name.cutFirst() else name
                languageMapCache.getOrPut(nameWithoutPrefix) { mutableMapOf() }[language] = node
            }
        }

        languageMapCache.forEach { (nodeName, languageMap) ->
            val languageMapMeta = Meta {
                languageMap.keys.forEach { language ->
                    set(language.key, (language.route + nodeName).toString())
                }
            }
            languageMap.forEach { (language, node) ->
                val languagePrefix = language.dataPath
                val nodeData = node.data
                if (nodeData != null) {
                    put(
                        languagePrefix + nodeName,
                        nodeData.withMeta { set(Language.LANGUAGE_MAP_KEY, languageMapMeta) }
                    )
                } else {
                    put(
                        languagePrefix + nodeName,
                        Data.wrapValue(Unit, Meta { set(Language.LANGUAGE_MAP_KEY, languageMapMeta) })
                    )
                }
            }
        }

    }


}


/**
 * Create a multiple sites for different languages. All sites use the same [content], but rely on different data
 */
context(site: SiteContextWithData)
public fun multiLanguageSite(
    defaultLanguage: Language,
    vararg languages: Language,
    content: HtmlSite,
) {
    val languageSet = setOf(defaultLanguage, *languages)

    val languageMappedData = siteData.filterByType<Any>().transform(
        LanguageMapAction(languageSet)
    )

    languageSet.forEach { language ->
        val languageSiteMeta = Meta {
            SITE_LANGUAGE_KEY put language.key
            SITE_LANGUAGE_MAP_KEY put Meta {
                languageSet.forEach {
                    it.key put it
                }
            }
        }

        val overlayData = DataTreeWithDefault<Any>(
            languageMappedData.branch(language.dataPath)!!,
            languageMappedData.branch(defaultLanguage.dataPath)!!
        )

        site.site(
            language.route,
            overlayData,
            siteMeta = Laminate(languageSiteMeta, site.siteMeta),
            content
        )
    }
}

/**
 * The language key of this page
 */
public val PageContext.language: String
    get() = pageMeta[Language.LANGUAGE_KEY]?.string ?: pageMeta[SITE_LANGUAGE_KEY]?.string ?: site.language

/**
 * Mapping of language keys to other language versions of this page
 */
context(page: PageContext)
public val languageMap: Map<String, Meta>
    get() = page.pageMeta[Language.LANGUAGE_MAP_KEY]?.items?.mapKeys { it.key.toStringUnescaped() } ?: emptyMap()

public fun PageContext.localisedPageRef(pageName: Name): String {
    val prefix = languageMap[language]?.get(Language::dataPath.name)?.string?.parseAsName() ?: Name.EMPTY
    return resolvePageRef(prefix + pageName)
}