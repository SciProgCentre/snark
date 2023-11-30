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

public fun interface HtmlFragment {
    public fun TagConsumer<*>.renderFragment()
}

public typealias HtmlData = Data<HtmlFragment>


public fun FlowContent.htmlData(page: PageContext, data: HtmlData): Unit = runBlocking(Dispatchers.IO) {
    withSnarkPage(page) {
        with(data.await()) { consumer.renderFragment() }
    }
}

context(SnarkContext)
public val Data<*>.id: String
    get() = meta["id"]?.string ?: "block[${hashCode()}]"

context(SnarkContext)
public val Data<*>.language: String?
    get() = meta["language"].string?.lowercase()

context(SnarkContext)
public val Data<*>.order: Int?
    get() = meta["order"]?.int

context(SnarkContext)
public val Data<*>.published: Boolean
    get() = meta["published"].string != "false"
