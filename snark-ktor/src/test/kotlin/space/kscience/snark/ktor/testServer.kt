package space.kscience.snark.ktor

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.local.localStorage
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

private class LocalDataHolder: DataHolder {
    private var source: Path? = null
    private var response: String = ""

    private fun getPath(relativePath: Path) : Path {
        return source!! / relativePath
    }
    override suspend fun init(relativePath: Path): Directory {
        if (source == null) {
            source = createTempDirectory()
        }
        val path = getPath(relativePath)
        path.createDirectories()
        path.toFile().deleteRecursively()
        path.createDirectory()
        return localStorage(path)
    }
    private fun buildResponse(from: Path, cur: Path) {
        for (entry in cur.listDirectoryEntries()) {
            if (entry.isDirectory()) {
                buildResponse(from, entry)
            } else {
                response += from.relativize(entry).toString() + "<br>"
            }
        }
    }
    override suspend fun represent(relativePath: Path) : String =
        if (source == null) {
            "No data was loaded!"
        } else {
            response = "List of files:<br>"
            val path = getPath(relativePath)
            buildResponse(path, path)
            response
        }
}

fun main() {
    SNARKServer(LocalDataHolder(), 9090).run()
}