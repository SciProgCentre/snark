package space.kscience.snark.main

import space.kscience.snark.ktor.DataHolder
import space.kscience.snark.storage.Directory
import documentBuilder.*
import kotlinx.html.HTML

internal class ServerDataHolder(private val directory: Directory): DataHolder {
    override fun init(): Directory =
        directory

    override suspend fun represent(): String {
        return buildDocument(directory).toString()
    }
}