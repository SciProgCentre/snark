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
import kotlinx.css.html
import java.nio.file.Path
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.local.localStorage
import space.kscience.snark.storage.unzip.unzip
import java.io.File
import kotlin.io.createTempFile
import kotlin.io.path.*
import kotlin.io.writeBytes

public interface DataHolder {

    fun init(relativePath: String = "/") : Directory
    fun represent(relativePath: String = "/"): String
    //will be HTML later
}

class LocalDataHolder: DataHolder {
    private var source: Path? = null
    private var response: String = ""

    private fun getPath(relativePath: String) : Path {
        return source!! / Path(relativePath.drop(1))
    }
    override fun init(relativePath: String): Directory {
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
                response += from.relativize(entry).toString() + "\n"
            }
        }
    }
    override fun represent(relativePath: String) : String =
        if (source == null) {
            "No data was loaded!"
        } else {
            response = "List of files:\n"
            val path = getPath(relativePath)
            buildResponse(path, path)
            response
        }
}

public class SNARKServer(val dataHolder: DataHolder, val port: Int): Runnable {
    private var relativePath = "/"

    private suspend fun receivePath(call: ApplicationCall) {
        relativePath = call.receiveText().drop(5).replace("%2F", "/")
        call.respondText("Path is successfully changed to: " + relativePath)
    }
    private suspend fun renderGet(call: ApplicationCall) {
        call.respondText(dataHolder.represent(relativePath))
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
        unzip(tmp.toPath().toString(), dataHolder.init(relativePath))
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
                p {
                    +("Path: " + relativePath)
                }
            }
            body {
                postForm(action = "/changePath") {
                    label {
                        + "Enter new path:"
                    }
                    input(name = "path", type = InputType.text) {}
                    button {
                        +"Change path"
                    }
                }
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
                post("/changePath") {
                    receivePath(call)
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