package space.kscience.snark.storage.unzip

import space.kscience.snark.storage.Directory
import java.io.FileInputStream
import java.util.zip.ZipInputStream

public suspend fun unzip(source_path: String, target: Directory) {
    val zis = ZipInputStream(FileInputStream(source_path))
    var zipEntry = zis.nextEntry
    while (zipEntry != null) {
        if (!zipEntry.isDirectory) {
            val filename = zipEntry.name
            target.create(filename, true)
            val fos = target.put(filename)
            val buffer = ByteArray(zipEntry.size.toInt())
            zis.read(buffer)
            fos.write(buffer)
            fos.close()
        }
        zipEntry = zis.nextEntry
    }
    zis.closeEntry()
    zis.close()
}