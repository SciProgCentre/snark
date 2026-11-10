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
import kotlinx.html.style
import kotlinx.html.unsafe
import space.kscience.snark.html.HtmlSite
import space.kscience.snark.html.document.allDocuments
import space.kscience.snark.ktor.snarkApplication

@Suppress("unused")
fun Application.renderAllDocuments() {
    snarkApplication(
        site = HtmlSite {
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
                    // LLM generated code: render Markdown math and Mermaid diagrams on the client
                    script {
                        unsafe {
                            +"""
                                document.addEventListener("DOMContentLoaded", function() {
                                    // Markdown math is rendered as .math elements without delimiters
                                    document.querySelectorAll(".math").forEach(function(el) {
                                        // keep the anchor of an equation label: ${'$'}{label("equation", "key")} inside \tag{}
                                        var label = el.querySelector("a.snark-label");
                                        if (label && !el.id) el.id = label.id;
                                        katex.render(el.textContent, el, {
                                            displayMode: el.classList.contains("math-block"),
                                            throwOnError: false
                                        });
                                    });
                                    // Raw HTML fragments may still use $ delimiters
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
                    script {
                        type = "module"
                        unsafe {
                            +"""
                                import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.esm.min.mjs";
                                // ```mermaid code blocks become diagrams
                                document.querySelectorAll("pre > code.language-mermaid").forEach(function(code) {
                                    const diagram = document.createElement("pre");
                                    diagram.className = "mermaid";
                                    diagram.textContent = code.textContent;
                                    code.parentElement.replaceWith(diagram);
                                });
                                mermaid.initialize({
                                    startOnLoad: false,
                                    securityLevel: "loose",
                                    theme: "base",
                                    themeVariables: {
                                        fontFamily: "Instrument Sans, Onest, Helvetica, Arial, sans-serif",
                                        fontSize: "15px",
                                        primaryColor: "#f3f7f7",
                                        primaryBorderColor: "#589885",
                                        primaryTextColor: "#222a34",
                                        lineColor: "#4d5561",
                                        clusterBkg: "#f9fafb",
                                        clusterBorder: "#eaecf0",
                                        edgeLabelBackground: "#ffffff"
                                    }
                                });
                                await mermaid.run();
                                document.dispatchEvent(new Event("snark:mermaid-ready"));
                            """.trimIndent()
                        }
                    }
                    style {
                        unsafe {
                            +"""
                                body { max-width: 900px; margin: 0 auto; padding: 0 16px; font-family: serif; line-height: 1.5; }
                                .snark-figure { margin: 1.5em 0; text-align: center; }
                                .snark-image { max-width: 100%; }
                                .snark-figure-caption, .snark-table-caption, .snark-listing-caption { font-size: 0.9em; }
                                blockquote { margin: 1em 0; padding: 0.5em 1em; border-left: 3px solid #999; background: #f6f6f6; }
                                table { border-collapse: collapse; margin: 0.5em auto 1.5em; }
                                th, td { border-top: 1px solid #999; border-bottom: 1px solid #999; padding: 4px 8px; }
                                pre { overflow-x: auto; }
                            """.trimIndent()
                        }
                    }
                }
            )
        }
    )

    routing {
        get("/") {
            call.respondRedirect("lorem/ipsum")
        }
    }
}

fun main() {
    embeddedServer(CIO, port = 8080, module = Application::renderAllDocuments).start(wait = true)
}