package space.kscience.snark.ktor

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.html.*
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import kotlin.reflect.KClass

private fun FlowContent.renderTreeItems(
    dataTree: DataTree<Any>,
    prefix: Name = Name.EMPTY,
    linkBuilder: (Name) -> String? = { null }
): Unit = ul {
    dataTree.items.forEach { (token, tree) ->
        li {
            val text = token.toStringUnescaped() +
                    (tree.data?.type?.let { ": ${(it.classifier as KClass<*>).simpleName}" } ?: "")

            val link = linkBuilder(prefix + token)
            if (link != null) {
                a(link) { +text }
            } else {
                +text
            }
            if (tree.items.isNotEmpty()) {
                renderTreeItems(tree, prefix + token, linkBuilder)
            }
        }
    }
}


public fun Route.debugDataTree(route: String, data: DataTree<Any>, linkBuilder: (Name) -> String? = { null }) {
    get(route) {
        call.respondHtml {
            head {

            }
            body {
                renderTreeItems(data, linkBuilder = linkBuilder)
            }
        }
    }
}