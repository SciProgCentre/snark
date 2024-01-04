package space.kscience.snark.html

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataSetBuilder
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.getIndexed
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.parseAsName

public fun interface HtmlSite {
    context(SiteContextWithData)
    public fun renderSite()
}

public fun DataSetBuilder<Any>.site(
    name: Name,
    siteMeta: Meta,
    block: (siteContext: SiteContext, data: DataSet<Any>) -> Unit,
) {
    static(name, HtmlSite { block(site, siteData) }, siteMeta)
}

//public fun DataSetBuilder<Any>.site(name: Name, block: DataSetBuilder<Any>.() -> Unit) {
//    node(name, block)
//}

internal fun DataSetBuilder<Any>.assetsFrom(rootMeta: Meta) {
    rootMeta.getIndexed("file".asName()).forEach { (_, meta) ->
        val webName: String? by meta.string()
        val name by meta.string { error("File path is not provided") }
        val fileName = name.parseAsName()
        static(fileName, webName?.parseAsName() ?: fileName)
    }
}