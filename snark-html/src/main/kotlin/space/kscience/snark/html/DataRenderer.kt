package space.kscience.snark.html

import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.title
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import kotlin.reflect.typeOf

/**
 * Render (or don't) given data piece
 */
public interface DataRenderer {

    context(SiteBuilder)
    public operator fun invoke(name: Name, data: Data<Any>)

    public companion object {
        public val DEFAULT: DataRenderer = object : DataRenderer {

            context(SiteBuilder)
            override fun invoke(name: Name, data: Data<Any>) {
                if (data.type == typeOf<HtmlData>()) {
                    val languageMeta: Meta = Language.forName(name)

                    val dataMeta: Meta = if (languageMeta.isEmpty()) {
                        data.meta
                    } else {
                        data.meta.toMutableMeta().apply {
                            "languages" put languageMeta
                        }
                    }

                    page(name, dataMeta) {
                        head {
                            title = dataMeta["title"].string ?: "Untitled page"
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