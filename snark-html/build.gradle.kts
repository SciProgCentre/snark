plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val ktorVersion = space.kscience.gradle.KScienceVersions.ktorVersion

kscience {
    jvm()
    useSerialization()
    commonMain {
        api(projects.snarkCore)

        api(spclibs.kotlinx.html)

//        api("org.jetbrains.kotlin-wrappers:kotlin-css")

        api("io.ktor:ktor-http:$ktorVersion")
        api(libs.dataforge.io.yaml)
//        api(libs.markdown)
        api(libs.kotmark)
        api(libs.kotmark.kotlinx)
        api(libs.kotmark.math)
        api(libs.kotmark.tables)
        api(libs.freemarker)

    }

}


readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    feature("data", "docs/data.md") { "Data-based processing instead of traditional layout-based" }
    feature("layouts", "docs/site-design.md") { "Use custom layouts to represent a data tree" }
    feature("parsers", "docs/data.md#custom-formats") { "Add custom file formats and parsers using DataForge dependency injection" }
    feature("preprocessor", "docs/data.md#preprocessing") { "Preprocessing text files using templates" }
    feature("metadata", "docs/data.md#metadata") { "DataForge metadata layering and transformations" }
    feature("links", "docs/links.md") { "Link resolution at render time" }
    feature("languages", "docs/languages.md") { "Multi-language sites" }
    feature("dynamic", "../snark-ktor") { "Generating dynamic site using Ktor server" }
    feature("static", "docs/site-design.md#5-rendering-targets") { "Generating static site" }
}