package space.kscience.snark.ktor

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.html.*
import io.ktor.server.routing.*
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.unzip.unzip
import java.io.File
import java.nio.file.Path
import kotlin.io.createTempFile
import kotlin.io.writeBytes
import kotlin.io.path.Path

public interface DataHolder {
    public suspend fun init(relativePath: Path) : Directory

    public suspend fun represent(relativePath: Path): String
}

public class SNARKServer(private val dataHolder: DataHolder, private val port: Int): Runnable {
    private var relativePath = Path("")

    private suspend fun receivePath(call: ApplicationCall) {
        val pathString = call.receiveParameters()["path"]?:""
        relativePath = Path(pathString.dropWhile{it == '/'})
        call.respondRedirect("/")
    }
    private suspend fun renderGet(call: ApplicationCall) {
        call.respondText(dataHolder.represent(relativePath), ContentType.Text.Html)
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
        call.respondRedirect("/")
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
                    +("Path: /" + relativePath.toString())
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
