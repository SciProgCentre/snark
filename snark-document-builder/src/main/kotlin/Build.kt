package documentBuilder

public class GraphManager(public val graph: DependencyGraph) {
    fun buildDocument(file: FileName) {
        val list = graph.nodes[file]
        if (list != null) {
            for (element in list.dependencies) {
                element.visit(this)
            }
        }
    }

    fun getAstRootDocument(file: FileName): MdAstRoot {
        buildDocument(file)
        return graph.nodes[file]!!.mdAst
    }
}