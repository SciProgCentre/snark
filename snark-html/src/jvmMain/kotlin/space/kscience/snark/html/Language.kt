package space.kscience.snark.html

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.actions.transform
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.*
import space.kscience.snark.DataTreeWithDefault
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.html.Language.Companion.SITE_LANGUAGE_KEY
import space.kscience.snark.html.Language.Companion.SITE_LANGUAGE_MAP_KEY
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

        public val LANGUAGE_KEY: Name = "language".asName()

        public val LANGUAGE_MAP_KEY: Name = "languageMap".asName()

        public val SITE_LANGUAGE_KEY: Name = SiteContext.SITE_META_KEY + LANGUAGE_KEY

        public val SITE_LANGUAGE_MAP_KEY: Name = SiteContext.SITE_META_KEY + LANGUAGE_MAP_KEY

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

public val SiteContext.languageMap: Map<String, Language>
    get() = siteMeta[SITE_LANGUAGE_MAP_KEY]?.items?.map {
        it.key.toStringUnescaped() to Language.read(it.value)
    }?.toMap() ?: emptyMap()

public val SiteContext.language: String
    get() = siteMeta[SITE_LANGUAGE_KEY].string ?: Language.DEFAULT_LANGUAGE

/**
 * Walk the data tree depth-first
 */
private fun <T, TR : GenericDataTree<T, TR>> TR.walk(
    namePrefix: Name = Name.EMPTY,
): Sequence<Pair<Name, TR>> = sequence {
    yield(namePrefix to this@walk)
    items.forEach { (token, tree) ->
        yieldAll(tree.walk(namePrefix + token))
    }
}

private class LanguageMapAction(val languages: Set<Language>) : AbstractAction<Any, Any>(typeOf<Any>()) {
    override fun DataSink<Any>.generate(data: DataTree<Any>, meta: Meta) {
        val languageMapCache = mutableMapOf<Name, MutableMap<Language, DataTree<Any>>>()

        data.walk().forEach { (name, node) ->
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
                    set(language.key, (language.route + nodeName).toWebPath())
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
                    wrap(languagePrefix + nodeName, Unit, Meta { set(Language.LANGUAGE_MAP_KEY, languageMapMeta) })
                }
            }
        }

    }

    override fun DataSink<Any>.update(source: DataTree<Any>, meta: Meta, namedData: NamedData<Any>) {
        TODO("Not yet implemented")
    }
}


/**
 * Create a multiple sites for different languages. All sites use the same [content], but rely on different data
 */
@SnarkBuilder
public fun SiteContextWithData.multiLanguageSite(
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

        site(
            language.route,
            overlayData,
            siteMeta = Laminate(languageSiteMeta, siteMeta),
            content
        )
    }
}

/**
 * The language key of this page
 */
public val PageContext.language: String
    get() = pageMeta[Language.LANGUAGE_KEY]?.string ?: pageMeta[SITE_LANGUAGE_KEY]?.string ?: Language.DEFAULT_LANGUAGE

/**
 * Mapping of language keys to other language versions of this page
 */
public val PageContext.languageMap: Map<String, Meta>
    get() = pageMeta[Language.LANGUAGE_MAP_KEY]?.items?.mapKeys { it.key.toStringUnescaped() } ?: emptyMap()

public fun PageContext.localisedPageRef(pageName: Name): String {
    val prefix = languageMap[language]?.get(Language::dataPath.name)?.string?.parseAsName() ?: Name.EMPTY
    return resolvePageRef(prefix + pageName)
}