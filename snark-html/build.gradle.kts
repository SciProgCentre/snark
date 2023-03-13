plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra
val ktorVersion = space.kscience.gradle.KScienceVersions.ktorVersion

kscience{
    useContextReceivers()
}

dependencies {
    api(projects.snarkCore)

    api("org.jetbrains.kotlinx:kotlinx-html:0.8.0")
    api("org.jetbrains.kotlin-wrappers:kotlin-css")

    api("io.ktor:ktor-utils:$ktorVersion")

    api("space.kscience:dataforge-io-yaml:$dataforgeVersion")
    api("org.jetbrains:markdown:0.4.0")
}

readme {
    maturity = space.kscience.gradle.Maturity.EXPERIMENTAL
    feature("data") { "Data-based processing. Instead of traditional layout-based" }
    feature("layouts") { "Use custom layouts to represent a data tree" }
    feature("parsers") { "Add custom file formats and parsers using DataForge dependency injection" }
    feature("preprocessor") { "Preprocessing text files using templates" }
    feature("metadata") { "Trademark DataForge metadata layering and transformations" }
    feature("dynamic") { "Generating dynamic site using KTor server" }
    feature("static") { "Generating static site" }
}