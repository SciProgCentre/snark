package space.kscience.snark.html

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.await
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import space.kscience.snark.SnarkContext


//TODO replace by VisionForge type
//typealias HtmlFragment = context(PageBuilder, TagConsumer<*>) () -> Unit

public fun interface HtmlFragment {
    public fun TagConsumer<*>.renderFragment(page: WebPage)
    //TODO move pageBuilder to a context receiver after KT-52967 is fixed
}

public typealias HtmlData = Data<HtmlFragment>

//fun HtmlData(meta: Meta, content: context(PageBuilder) TagConsumer<*>.() -> Unit): HtmlData =
//    Data(HtmlFragment(content), meta)


context(WebPage) public fun FlowContent.htmlData(data: HtmlData): Unit = runBlocking(Dispatchers.IO) {
    with(data.await()) { consumer.renderFragment(page) }
}

context(SnarkContext) public val Data<*>.id: String get() = meta["id"]?.string ?: "block[${hashCode()}]"
context(SnarkContext) public val Data<*>.language: String? get() = meta["language"].string?.lowercase()

context(SnarkContext) public val Data<*>.order: Int? get() = meta["order"]?.int

context(SnarkContext) public val Data<*>.published: Boolean get() = meta["published"].string != "false"
