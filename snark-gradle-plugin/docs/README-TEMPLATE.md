<#-- LLM generated code: README template for snark-gradle-plugin -->
# Module ${name}

${description}

## Usage

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://repo.kotlin.link")
        gradlePluginPortal()
    }
}

// build.gradle.kts
plugins {
    id("space.kscience.snark") version "${version}"
}

snark {
    dataDirectory = file("data") // default: `data` in the root project directory
    mcpPort = 3000               // default
}
```

## Tasks and build changes

| Applied with             | Effect                                                                                    |
|--------------------------|-------------------------------------------------------------------------------------------|
| Kotlin JVM plugin        | `snarkMcpServer` task serves the data directory as an [MCP server](../snark-ktor/docs/mcp.md) over HTTP |
| Kotlin JVM plugin        | `snarkMcpStd` task runs the same MCP server over standard input and output                |
| Kotlin JVM plugin        | `writeBuildDate` runs before `processResources` and writes the build time to the `buildDate` resource |
| `application` plugin     | The data directory is added to the `data` folder of the main distribution                 |

The distribution layout matches the default data path of [snark-ktor](../snark-ktor), so an installed application
finds its data when started from the distribution directory.

## Deploying over SFTP

The plugin provides helpers based on [JSch](https://github.com/mwiede/jsch) for deploy tasks in build scripts:

```kotlin
tasks.register("deploy") {
    dependsOn("installDist")
    doLast {
        JSch {
            addIdentity(System.getProperty("user.home") + "/.ssh/id_rsa")
        }.useSession(host = "example.com", user = "deploy") {
            uploadDirectory(layout.buildDirectory.dir("install/my-site").get().asFile, "/opt")
            println(execute("systemctl restart my-site"))
        }
    }
}
```

`uploadDirectory` skips files and directories starting with `.`. Host key checking is disabled for these sessions.
