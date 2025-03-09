@file:OptIn(DFExperimental::class)

package space.kscience.snark

import space.kscience.dataforge.data.branch
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.WorkspaceBuilder
import space.kscience.dataforge.workspace.directory
import space.kscience.dataforge.workspace.resources
import kotlin.io.path.Path

//
///**
// * Reads the specified resources and returns a [DataTree] containing the data.
// *
// * @param resources The names of the resources to read.
// * @param classLoader The class loader to use for loading the resources. By default, it uses the current thread's context class loader.
// * @return A DataTree containing the data read from the resources.
// */
//private fun IOPlugin.readResources(
//    vararg resources: String,
//    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
//): DataTree<Binary> = DataTree {
//    //    require(resource.isNotBlank()) {"Can't mount root resource tree as data root"}
//    resources.forEach { resource ->
//        val path = classLoader.getResource(resource)?.toURI()?.toPath() ?: error(
//            "Resource with name $resource is not resolved"
//        )
//        node(resource, readRawDirectory(path))
//    }
//}

public fun Snark.workspace(
    meta: Meta,
    workspaceBuilder: WorkspaceBuilder.() -> Unit = {},
): Workspace = Workspace {


    data {
        meta.getIndexed("directory").forEach { (index, directoryMeta) ->
            val dataDirectory = directoryMeta["path"].string ?: error("Directory path not defined")
            val nodeName = directoryMeta["name"].string ?: directoryMeta.string ?: index ?: ""
            directory(io, nodeName.parseAsName(), Path((dataDirectory)))
        }
        meta.getIndexed("resource").forEach { (index, resourceMeta) ->
            val resource = resourceMeta["path"]?.stringList ?: listOf("/")
            val nodeName = resourceMeta["name"].string ?: resourceMeta.string ?: index ?: ""
            branch(nodeName) {
                resources(io, *resource.toTypedArray())
            }
        }
    }

    workspaceBuilder()
}