package space.kscience.snark.html

import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.title
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.getItem
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.names.removeHeadOrNull
import kotlin.reflect.typeOf

/**
 * Render (or don't) given data piece
 */
public interface DataRenderer {

    context(SiteBuilder)
    public operator fun invoke(name: Name, data: Data<Any>)

    public companion object {

//        context (SiteBuilder)
//        public fun buildPageMeta(name: Name, data: Data<Any>): Laminate {
//            val languages = languages.mapKeys { it.value["key"]?.string ?: it.key }
//
//            // detect current language by prefix if it is not defined explicitly
//            val currentLanguage: String = data.meta["language"]?.string
//                ?: languages.keys.firstOrNull() { key -> name.startsWith(key.parseAsName()) } ?: defaultLanguage
//
//            //
//            val languageMap = Meta {
//                languages.forEach { (key, meta) ->
//                    val languagePrefix: String = meta.string ?: meta["name"]?.string ?: return@forEach
//                    val targetName = name.removeHeadOrNull("")
//                    val targetData = this@SiteBuilder.data[targetName.parseAsName()]
//                    if (targetData != null) key put targetName
//                }
//            }
//            val languageMeta = Meta {
//                "language" put currentLanguage
//                if (!languageMap.isEmpty()) {
//                    "languageMap" put languageMap
//                }
//            }
//            return Laminate(data.meta, languageMeta, siteMeta)
//        }

        /**
         * Automatically build a language map for a data piece with given [name] based on existence of appropriate data nodes.
         */
        context(SiteBuilder)
        public fun buildLanguageMeta(name: Name): Meta = Meta {
            val currentLanguagePrefix = languages[language]?.get("prefix")?.string ?: language
            val fullName = (route.removeHeadOrNull(currentLanguagePrefix.asName()) ?: route) + name
            languages.forEach { (key, meta) ->
                val languagePrefix: String = meta["prefix"].string ?: key
                val nameWithLanguage: Name = if (languagePrefix.isBlank()) {
                    fullName
                } else {
                    languagePrefix.asName() + fullName
                }
                if (data.getItem(name) != null) {
                    key put meta.asMutableMeta().apply {
                        "target" put nameWithLanguage.toString()
                    }
                }
            }
        }

        public val DEFAULT: DataRenderer = object : DataRenderer {

            context(SiteBuilder)
            override fun invoke(name: Name, data: Data<Any>) {
                if (data.type == typeOf<HtmlData>()) {
                    page(name, data.meta) {
                        head {
                            title = data.meta["title"].string ?: "Untitled page"
                        }
                        body {
                            @Suppress("UNCHECKED_CAST")
                            htmlData(data as HtmlData)
                        }
                    }
                }
            }
        }
    }
}