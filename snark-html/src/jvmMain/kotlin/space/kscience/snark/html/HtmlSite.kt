package space.kscience.snark.html

import space.kscience.dataforge.data.DataSink
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.writeValue
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.getIndexed
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName

public fun interface HtmlSite {
    context(siteContext: SiteContextWithData)
    public fun renderSite()
}

public suspend fun DataSink<Any>.site(
    name: Name,
    siteMeta: Meta,
    block: (siteContext: SiteContext, data: DataTree<*>?) -> Unit,
) {
    writeValue(name, HtmlSite { block(siteContextWithData.site, siteContextWithData.siteData) }, siteMeta)
}

//public fun DataSetBuilder<Any>.site(name: Name, block: DataSetBuilder<Any>.() -> Unit) {
//    node(name, block)
//}

internal suspend fun DataSink<Any>.assetsFrom(rootMeta: Meta) {
    rootMeta.getIndexed("file").forEach { (_, meta) ->
        val webName: String? by meta.string()
        val name by meta.string { error("File path is not provided") }
        val fileName = name.parseAsName()
        writeValue(fileName, webName?.parseAsName() ?: fileName)
    }
}