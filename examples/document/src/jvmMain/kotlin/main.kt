package center.sciprog.snark.documents

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.html.ScriptCrossorigin
import kotlinx.html.link
import kotlinx.html.script
import kotlinx.html.unsafe
import space.kscience.snark.html.document.allDocuments
import space.kscience.snark.ktor.snarkApplication

@Suppress("unused")
fun Application.renderAllDocuments() = snarkApplication {
    allDocuments(
        headers = {
            //add katex headers
            link {
                rel = "stylesheet"
                href = "https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.css"
                attributes["integrity"] = "sha384-wcIxkf4k558AjM3Yz3BBFQUbk/zgIYC2R0QpeeYb+TwlBVMrlgLqwRjRtGZiK7ww"
                attributes["crossorigin"] = "anonymous"
            }
            script {
                defer = true
                src = "https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/katex.min.js"
                integrity = "sha384-hIoBPJpTUs74ddyc4bFZSM1TVlQDA60VBbJS0oA934VSz82sBx1X7kSx2ATBDIyd"
                crossorigin = ScriptCrossorigin.anonymous
            }
            script {
                defer = true
                src = "https://cdn.jsdelivr.net/npm/katex@0.16.10/dist/contrib/auto-render.min.js"
                integrity = "sha384-43gviWU0YVjaDtb/GhzOouOXtZMP/7XUzwPTstBeZFe/+rCMvRwr4yROQP43s0Xk"
                crossorigin = ScriptCrossorigin.anonymous
                attributes["onload"] = "renderMathInElement(document.body);"
            }
            // Auto-render latex expressions with katex
            script {
                unsafe {
                    +"""
                        document.addEventListener("DOMContentLoaded", function() {
                            renderMathInElement(document.body, {
                                delimiters: [
                                    {left: '$$', right: '$$', display: true},
                                    {left: '$', right: '$', display: false},
                                ],
                                throwOnError : false
                            });
                        });

                    """.trimIndent()
                }
            }
        }
    )

    routing {
        get("/"){
            call.respondRedirect("lorem/ipsum")
        }
    }
}


fun main() {
    embeddedServer(CIO, module = Application::renderAllDocuments).start(true)
}