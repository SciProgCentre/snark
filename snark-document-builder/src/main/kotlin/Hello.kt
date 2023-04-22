package hello

import com.fasterxml.jackson.core.io.BigDecimalParser
import space.kscience.snark.storage.*
import space.kscience.snark.storage.local.LocalDirectory
import java.nio.file.Path


public suspend fun doSomethingEssential() {
    val dir: Directory = LocalDirectory(".")
    dir.get("notexists")
}