package space.kscience.snark.plugin

import com.jcraft.jsch.*
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * https://kodehelp.com/java-program-uploading-folder-content-recursively-from-local-to-sftp-server/
 */
private fun ChannelSftp.recursiveFolderUpload(sourceFile: File, destinationPath: String) {
    if (sourceFile.isFile) {
        // copy if it is a file
        cd(destinationPath)
        if (!sourceFile.name.startsWith(".")) put(
            FileInputStream(sourceFile),
            sourceFile.getName(),
            ChannelSftp.OVERWRITE
        )
    } else {
        val files = sourceFile.listFiles()
        if (files != null && !sourceFile.getName().startsWith(".")) {
            cd(destinationPath)
            var attrs: SftpATTRS? = null
            // check if the directory is already existing
            val directoryPath = destinationPath + "/" + sourceFile.getName()
            try {
                attrs = stat(directoryPath)
            } catch (e: Exception) {
                println("$directoryPath does not exist")
            }

            // else create a directory
            if (attrs != null) {
                println("Directory $directoryPath exists IsDir=${attrs.isDir()}")
            } else {
                println("Creating directory $directoryPath")
                mkdir(sourceFile.getName())
            }
            for (f in files) {
                recursiveFolderUpload(f, destinationPath + "/" + sourceFile.getName())
            }
        }
    }
}

public fun Session.uploadDirectory(
    file: File,
    targetDirectory: String,
) {
    var channel: ChannelSftp? = null
    try {
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        channel = openChannel("sftp") as ChannelSftp // Open SFTP Channel
        channel.connect()
        channel.cd(targetDirectory) // Change Directory on SFTP Server
        channel.recursiveFolderUpload(file, targetDirectory)
    } finally {
        channel?.disconnect()
    }
}

public fun Session.execute(
    command: String,
): String {
    var channel: ChannelExec? = null
    try {
        channel = openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.inputStream = null
        channel.setErrStream(System.err)
        val input = channel.inputStream
        channel.connect()
        return input.use { it.readAllBytes().decodeToString() }
    } finally {
        channel?.disconnect()
    }
}

public inline fun JSch.useSession(
    host: String,
    user: String,
    port: Int = 22,
    block: Session.() -> Unit,
) {
    var session: Session? = null
    try {
        session = getSession(user, host, port)
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.connect()
        session.block()
    } finally {
        session?.disconnect()
    }
}

public fun JSch(configuration: JSch.() -> Unit): JSch = JSch().apply(configuration)
