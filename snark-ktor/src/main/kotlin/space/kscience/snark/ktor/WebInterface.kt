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
import kotlinx.css.h1
import kotlinx.css.html
import kotlinx.html.dom.create
import kotlinx.html.dom.document
import java.nio.file.Path
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.local.localStorage
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import space.kscience.snark.storage.unzip.unzip

public interface DataHolder {
    public fun init() : Directory
    public suspend fun represent(): String
    //will be HTML later
}
class LocalDataHolder: DataHolder {
    private var source: Path? = null
    private var response: String = ""
    override fun init(): Directory {
        source?.toFile()?.deleteRecursively()
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
    override suspend fun represent(): String =
        if (source == null) {
            "No data was loaded!"
        } else {
            response = "List of files:\n"
            buildResponse(source!!)
            response
        }
}

public class SNARKServer(val dataHolder: DataHolder, val port: Int): Runnable {
    private suspend fun renderGet(call: ApplicationCall) {
        call.respondText(dataHolder.represent())
    }
    private suspend fun renderUpload(call: ApplicationCall) {
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
    private suspend fun renderMainPage(call: ApplicationCall)  {
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
    override fun run() {
        embeddedServer(Netty, port) {
            routing {
                get("/") {
                    renderMainPage(call)
                }
                post("/upload") {
                    renderUpload(call)
                }
                get("/data") {
                    renderGet(call)
                }
            }
        }.start(wait = true)
    }
}