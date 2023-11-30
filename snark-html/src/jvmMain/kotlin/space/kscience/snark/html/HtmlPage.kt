package space.kscience.snark.html

import kotlinx.html.HTML
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataSetBuilder
import space.kscience.dataforge.data.node
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name

public fun interface HtmlPage {
    public fun HTML.renderPage(pageContext: PageContext, pageData: DataSet<*>)
}


// data builders

public fun DataSetBuilder<Any>.page(name: Name, pageMeta: Meta = Meta.EMPTY, block: HTML.(pageContext: PageContext, pageData: DataSet<Any>) -> Unit) {
    val page = HtmlPage(block)
    static<HtmlPage>(name, page, pageMeta)
}



//                if (data.type == typeOf<HtmlData>()) {
//                    val languageMeta: Meta = Language.forName(name)
//
//                    val dataMeta: Meta = if (languageMeta.isEmpty()) {
//                        data.meta
//                    } else {
//                        data.meta.toMutableMeta().apply {
//                            "languages" put languageMeta
//                        }
//                    }
//
//                    page(name, dataMeta) { pageContext->
//                        head {
//                            title = dataMeta["title"].string ?: "Untitled page"
//                        }
//                        body {
//                            @Suppress("UNCHECKED_CAST")
//                            htmlData(pageContext, data as HtmlData)
//                        }
//                    }
//                }