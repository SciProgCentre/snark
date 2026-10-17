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
        api(libs.markdown)
        api(libs.freemarker)

    }

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