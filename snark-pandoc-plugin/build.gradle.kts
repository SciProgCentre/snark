plugins {
    id("java")
}

repositories {
    mavenCentral()
}

java.sourceCompatibility = JavaVersion.VERSION_11

dependencies {
    implementation("commons-io:commons-io:2.7")
    implementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.apache.commons:commons-exec:1.3")
    implementation("org.apache.commons:commons-compress:1.2")
    implementation("org.apache.ant:ant:1.10.13")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}