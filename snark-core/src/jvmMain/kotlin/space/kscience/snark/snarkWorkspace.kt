@file:OptIn(DFExperimental::class)

package space.kscience.snark

import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.node
import space.kscience.dataforge.data.observeNode
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.WorkspaceBuilder
import space.kscience.dataforge.workspace.readDirectory
import space.kscience.dataforge.workspace.task
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import kotlin.io.path.Path
import kotlin.io.path.toPath

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

/**
 * @param resources The names of the resources to read.
 * @param classLoader The class loader to use for loading the resources. By default, it uses the current thread's context class loader.
 */
public fun IOPlugin.readResourceDirectory(
    resource: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): DataTree<Binary> {
    //create a file system if necessary
    val uri = Thread.currentThread().contextClassLoader.getResource("common")!!.toURI()
    try {
        uri.toPath()
    } catch (e: FileSystemNotFoundException) {
        FileSystems.newFileSystem(uri, mapOf("create" to "true"))
    }

    val path = classLoader.getResource(resource)?.toURI()?.toPath() ?: error(
        "Resource with name $resource is not resolved"
    )
    return readDirectory(path, monitor = false, includeExtensions = true)
}

public fun Snark.workspace(
    meta: Meta,
    workspaceBuilder: WorkspaceBuilder.() -> Unit = {},
): Workspace = Workspace {


    val readData by task<Binary> {

        DataTree<Binary> {
            meta.getIndexed("directory").forEach { (index, directoryMeta) ->
                val dataDirectory =
                    directoryMeta["path"].string?.let { Path(it) } ?: error("Directory path not defined")
                val nodeName = directoryMeta["name"].string ?: directoryMeta.string ?: index ?: ""
                observeNode(nodeName, context, io.readDirectory(dataDirectory, monitor = true, includeExtensions = true))
            }
            meta.getIndexed("resource").forEach { (index, resourceMeta) ->
                val resources = resourceMeta["path"]?.stringList ?: listOf("/")
                val nodeName = resourceMeta["name"].string ?: resourceMeta.string ?: index ?: ""

                resources.forEach { resource ->
                    node(nodeName + resource, io.readResourceDirectory(resource))
                }
            }
        }
    }

    workspaceBuilder()
}