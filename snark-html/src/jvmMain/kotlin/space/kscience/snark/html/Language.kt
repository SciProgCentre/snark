package space.kscience.snark.html

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.branch
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.*
import space.kscience.snark.SnarkBuilder
import space.kscience.snark.html.Language.Companion.SITE_LANGUAGES_KEY
import space.kscience.snark.html.Language.Companion.SITE_LANGUAGE_KEY


public class Language : Scheme() {
    /**
     * Language key override
     */
    public var key: String? by string()

    /**
     * Page name prefix
     */
    public var prefix: String? by string()

    /**
     * Target page name with a given language key
     */
    public var target: Name?
        get() = meta["target"].string?.parseAsName(false)
        set(value) {
            meta["target"] = value?.toString()?.asValue()
        }

    public companion object : SchemeSpec<Language>(::Language) {

        public val LANGUAGE_KEY: Name = "language".asName()

        public val LANGUAGES_KEY: Name = "languages".asName()

        public val SITE_LANGUAGE_KEY: Name = SiteContext.SITE_META_KEY + LANGUAGE_KEY

        public val SITE_LANGUAGES_KEY: Name = SiteContext.SITE_META_KEY + LANGUAGES_KEY

        public const val DEFAULT_LANGUAGE: String = "en"
//
//        /**
//         * Automatically build a language map for a data piece with given [name] based on existence of appropriate data nodes.
//         */
//        context(SiteContext)
//        public fun forName(name: Name): Meta = Meta {
//            val currentLanguagePrefix = languages[language]?.get(Language::prefix.name)?.string ?: language
//            val fullName = (route.removeFirstOrNull(currentLanguagePrefix.asName()) ?: route) + name
//            languages.forEach { (key, meta) ->
//                val languagePrefix: String = meta[Language::prefix.name].string ?: key
//                val nameWithLanguage: Name = if (languagePrefix.isBlank()) {
//                    fullName
//                } else {
//                    languagePrefix.asName() + fullName
//                }
//                if (resolveData.getItem(name) != null) {
//                    key put meta.asMutableMeta().apply {
//                        Language::target.name put nameWithLanguage.toString()
//                    }
//                }
//            }
//        }
    }
}

public val SiteContext.languages: Map<String, Meta>
    get() = siteMeta[SITE_LANGUAGES_KEY]?.items?.mapKeys { it.key.toStringUnescaped() } ?: emptyMap()

public val SiteContext.language: String
    get() = siteMeta[SITE_LANGUAGE_KEY].string ?: Language.DEFAULT_LANGUAGE

public val SiteContext.languagePrefix: Name
    get() = languages[language]?.let { it[Language::prefix.name].string ?: language }?.parseAsName() ?: Name.EMPTY

@SnarkBuilder
public suspend fun SiteContext.multiLanguageSite(data: DataSet<Any>, languageMap: Map<String, Language>, site: HtmlSite) {
    languageMap.forEach { (languageKey, language) ->
        val prefix = language.prefix ?: languageKey
        val languageSiteMeta = Meta {
            SITE_LANGUAGE_KEY put languageKey
            SITE_LANGUAGES_KEY put Meta {
                languageMap.forEach {
                    it.key put it.value
                }
            }
        }
        site(prefix.parseAsName(), data.branch(prefix), siteMeta = Laminate(languageSiteMeta, siteMeta), site)
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
public val PageContext.languages: Map<String, Meta>
    get() = pageMeta[Language.LANGUAGES_KEY]?.items?.mapKeys { it.key.toStringUnescaped() } ?: emptyMap()

public fun PageContext.localisedPageRef(pageName: Name, relative: Boolean = false): String {
    val prefix = languages[language]?.get(Language::prefix.name)?.string?.parseAsName() ?: Name.EMPTY
    return resolvePageRef(prefix + pageName, relative)
}

//
///**
// * Render all pages in a node with given name. Use localization prefix if appropriate data is available.
// */
//public fun SiteContext.localizedPages(
//    dataPath: Name,
//    remotePath: Name = dataPath,
//    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
//) {
//    val item = resolveData.getItem(languagePrefix + dataPath)
//        ?: resolveData.getItem(dataPath)
//        ?: error("No data found by name $dataPath")
//    route(remotePath) {
//        pages(item, dataRenderer)
//    }
//}
//
//public fun SiteContext.localizedPages(
//    dataPath: String,
//    remotePath: Name = dataPath.parseAsName(),
//    dataRenderer: DataRenderer = DataRenderer.DEFAULT,
//) {
//    localizedPages(dataPath.parseAsName(), remotePath, dataRenderer = dataRenderer)
//}