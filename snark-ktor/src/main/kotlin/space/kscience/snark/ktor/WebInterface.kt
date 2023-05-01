package space.kscience.snark.ktor

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.http.content.*
import kotlinx.html.*
import io.ktor.server.routing.*
import java.nio.file.Path
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.local.localStorage
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import space.kscience.snark.storage.unzip.unzip

public interface DataHolder {

    fun init() : Directory
    fun represent(): String
}
class LocalDataHolder: DataHolder {
    private var source: Path? = null
    private var response: String = ""
    override fun init(): Directory {
        source = createTempDirectory()
        return localStorage(source!!)
    }
    private fun buildResponse(path: Path) {
        for (entry in path.listDirectoryEntries()) {
            if (entry.isDirectory()) {
                buildResponse(entry)
            } else {
                response += source!!.relativize(entry).toString() + "\n"
            }
        }
    }
    override fun represent() : String =
        if (source == null) {
            "No data was loaded!"
        } else {
            response = "List of files:\n"
            buildResponse(source!!)
            response
        }
}
fun main() {
    val dataHolder: DataHolder = LocalDataHolder()
    embeddedServer(Netty, 9090) {
        routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title {
                            +"SNARK"
                        }
                    }
                    body {
                        h1 {
                            +"SNARK"
                        }
                    }
                    body {
                        postForm (action = "/upload", encType = FormEncType.multipartFormData)  {
                            label {
                                +"Choose zip archive: "
                            }
                            input (name = "file", type = InputType.file) {}
                            button {
                                +"Upload file"
                            }
                        }
                        a("/data") {
                            +"Show data\n"
                        }
                    }
                }
            }
            post("/upload") {
                val multipartData = call.receiveMultipart()
                val tmp = createTempFile(suffix=".zip")
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val fileBytes = part.streamProvider().readBytes()
                            tmp.writeBytes(fileBytes)
                        }
                        else -> {
                        }
                    }
                    part.dispose()
                }
                unzip(tmp.toPath().toString(), dataHolder.init())
                call.respondText("File is successfully uploaded")
            }
            get("/data") {
                call.respondText(dataHolder.represent())
            }
        }
    }.start(wait = true)
}