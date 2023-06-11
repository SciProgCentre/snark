package space.kscience.snark.main

import space.kscience.snark.ktor.DataHolder
import space.kscience.snark.storage.Directory
import documentBuilder.*
import java.nio.file.Path

internal class ServerDataHolder(private val directory: Directory): DataHolder {

    override suspend fun init(relativePath: Path): Directory = directory


    override suspend fun represent(relativePath: Path): String {
        return buildDocument(directory, relativePath)
    }

    override suspend fun toPdf(relativePath: Path) :  Path {
        return buildLatex(directory, relativePath)
    }
}