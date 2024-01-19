package space.kscience.snark.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.tryGetString
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.*


private const val DEPLOY_DATE_FILE = "deployDate"
private const val BUILD_DATE_FILE = "/buildDate"

/**
 * Prepare the data cache directory for snark. Clear data if it is outdated.
 * TODO make internal
 *
 * @return true if cache is valid and false if it is reset
 */
fun Application.prepareSnarkDataCacheDirectory(dataPath: Path): Boolean {

    // Clear data directory if it is outdated
    val deployDate = dataPath.resolve(DEPLOY_DATE_FILE).takeIf { it.exists() }
        ?.readText()?.let { LocalDateTime.parse(it) }
    val buildDate = javaClass.getResource(BUILD_DATE_FILE)?.readText()?.let { LocalDateTime.parse(it) }

    val inProduction: Boolean = environment.config.tryGetString("ktor.environment.production") == "true"

    if (inProduction) {
        log.info("Production mode activated")
        log.info("Build date: $buildDate")
        log.info("Deploy date: $deployDate")
    }

    if (!dataPath.exists()) {
        dataPath.createDirectories()
        dataPath.resolve(DEPLOY_DATE_FILE).writeText(LocalDateTime.now().toString())
        return false
    } else if (deployDate != null && buildDate != null && buildDate.isAfter(deployDate)) {
        log.info("Outdated data. Resetting data directory.")

        Files.walk(dataPath)
            .sorted(Comparator.reverseOrder())
            .forEach { it.deleteIfExists() }

        //Writing deploy date file
        dataPath.createDirectories()
        dataPath.resolve(DEPLOY_DATE_FILE).writeText(LocalDateTime.now().toString())
        return false

    } else if (inProduction && deployDate == null && buildDate != null) {
        val date = LocalDateTime.now().toString()
        log.info("Deploy date: $date")
        //Writing deploy date in production mode if it does not exist
        dataPath.createDirectories()
        dataPath.resolve(DEPLOY_DATE_FILE).writeText(date)
        return false
    } else {
        return true
    }
}