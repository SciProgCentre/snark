package space.kscience.snark.html

import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName

public class SnarkHtmlEnvironmentBuilder {
    public val layouts: HashMap<Name, SiteLayout> = HashMap()

    public fun layout(name: String, body: context(SiteBuilder) (DataTreeItem<*>) -> Unit) {
        layouts[name.parseAsName()] = object : SiteLayout {
            context(SiteBuilder) override fun render(item: DataTreeItem<*>) = body(site, item)
        }
    }
}


//public fun SnarkEnvironment.html(block: SnarkHtmlEnvironmentBuilder.() -> Unit) {
//    contract {
//        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//    }
//
//    val envBuilder = SnarkHtmlEnvironmentBuilder().apply(block)
//
//    val plugin = object : AbstractPlugin() {
//        val snark by require(SnarkHtmlPlugin)
//
//        override val tag: PluginTag = PluginTag("@extension[${hashCode()}]")
//
//
//        override fun content(target: String): Map<Name, Any> = when (target) {
//            SiteLayout.TYPE -> envBuilder.layouts
//            else -> super.content(target)
//        }
//    }
//    registerPlugin(plugin)
//}