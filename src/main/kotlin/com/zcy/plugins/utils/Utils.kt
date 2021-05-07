package com.zcy.plugins.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.zcy.plugins.checker.ProjectChecker
import com.zcy.plugins.model.Command
import com.zcy.plugins.model.KillCommand
import com.zcy.plugins.ui.CommandForm
import com.zcy.plugins.ui.GitForm
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.swing.JScrollBar
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

object Utils {

    private val logger = Logger.getInstance(Utils::class.java)

    var gitPath: String? = null
    var commandForm: CommandForm? = null
    var gitForm: GitForm? = null
    var isBuildRunnerSuccess: Boolean = false
    const val FLUTTER_HOST_ENV = "FLUTTER_HOST"

    /**
     * find git directory(.git) in current project bash path
     */
    fun findGitPath(project: Project?) {
        if (!isEmptyString(gitPath)) {
            return
        }
        SwingUtilities.invokeLater {
            try {
                val fillCmd: String = if (isWindowsOS) {
                    "where git"
                } else {
                    "which git"
                }
                val process = Runtime.getRuntime().exec(fillCmd, null, File(project?.basePath))
                val bufferedErrorStream = BufferedInputStream(process.errorStream)
                val bufferedInputStream = BufferedInputStream(process.inputStream)
                val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                gitPath = bufferedInputReader.readLine()
                bufferedErrorStream.close()
                bufferedInputStream.close()
                bufferedErrorReader.close()
                bufferedInputReader.close()
            } catch (e: Throwable) {
                println(e.message)
                e.printStackTrace()
            }
        }
    }

    /**
     * Is windows OS
     *
     * @return true-windowsOS
     */
    val isWindowsOS: Boolean
        get() = System.getProperty("os.name").toLowerCase().startsWith("windows")

    /**
     * show info message
     *
     * @param message msg
     */
    fun showInfo(message: String?) {
        Messages.showMessageDialog(message, "Common Tools", Messages.getInformationIcon())
    }

    /**
     * show error message
     *
     * @param message msg
     */
    fun showErrorMessage(message: String?) {
        Messages.showMessageDialog(message, "Common Tools", Messages.getErrorIcon())
    }

    /**
     * Display simple notification - information
     *
     * @param project
     * @param text
     */
    fun showInfoNotification(project: Project?, text: String?) {
        showNotification(project, MessageType.INFO, text)
    }

    /**
     * Display simple notification - error
     *
     * @param project
     * @param text
     */
    fun showErrorNotification(project: Project?, text: String?) {
        showNotification(project, MessageType.ERROR, text)
    }

    /**
     * 向上递归查找flutter工程的根目录，如果没有找到就返回当前项目的根目录
     *
     * @param project 当前项目
     * @param path    当前选中的路径
     * @param checker 路径判断器
     * @return flutter工程的根目录
     */
    fun findFlutterPath(project: Project, path: String?, checker: ProjectChecker): String? {
        var currentPath = path
        if (checker.checkFlutter(path)) {
            return path
        }
        val file = File(currentPath)
        return if (!file.exists()) {
            project.basePath
        } else {
            if (path == project.basePath) {
                return path
            }
            currentPath = file.parent
            if (path == project.basePath) {
                project.basePath
            } else {
                findFlutterPath(project, currentPath, checker)
            }
        }
    }

    /**
     * Display simple notification of given type
     *
     * @param project
     * @param type
     * @param text
     */
    fun showNotification(project: Project?, type: MessageType?, text: String?) {
        val statusBar = WindowManager.getInstance().getStatusBar(project!!)
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text!!, type, null)
                .setFadeoutTime(5000)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight)
    }


    fun className2DartFileName(value: String): String {
        val stringBuilder = java.lang.StringBuilder()
        for (i in value.indices) {
            val c = value[i]
            if (i != 0 && Pattern.matches("[a-z]", value[i - 1].toString())
                    && Pattern.matches("[A-Z]", c.toString())
            ) {
                stringBuilder.append("_")
            }
            stringBuilder.append(c.toString().toLowerCase())
        }
        return stringBuilder.toString()
    }


    /**
     * Easier way to check if string is empty
     *
     * @param text
     * @return
     */
    fun isEmptyString(text: String?): Boolean {
        return text == null || text.trim { it <= ' ' }.length == 0
    }

    private val currentTime: String
        get() {
            val date = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            return sdf.format(date) + " "
        }

    fun getClassName(name: String): String {
        val chars = name.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            if (!Character.isLetter(c)) {
                chars[i] = '_'
                if (i + 1 < chars.size) {
                    chars[i + 1] = Character.toUpperCase(chars[i + 1])
                }
            }
        }
        return String(chars).replace("_".toRegex(), "")
    }

    fun getFieldName(name: String): String {
        val chars = name.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            if (!Character.isLetter(c)) {
                chars[i] = '_'
                if (i + 1 < chars.size) {
                    chars[i + 1] = Character.toUpperCase(chars[i + 1])
                }
            }
        }
        Character.toLowerCase(chars[0])
        return String(chars).replace("_".toRegex(), "")
    }

    fun isInteger(str: String?): Boolean {
        val pattern = Pattern.compile("^[-\\+]?[\\d]*$")
        return pattern.matcher(str).matches()
    }

    private fun getFlutterHostEnvValue(): String {
        val clientId = ApplicationNamesInfo.getInstance().fullProductName.replace(" ".toRegex(), "-")
        val existingVar = System.getenv(FLUTTER_HOST_ENV)
        return if (existingVar == null) clientId else "$existingVar:$clientId"
    }

    private fun getPids(killCommand: KillCommand): Array<String>? {
        return if (isWindowsOS) getPidsOnWindows(
                killCommand
        ) else getPidsOnUnix(killCommand)
    }

    private fun getPidsOnUnix(killCommand: KillCommand): Array<String>? {
        val pids = ArrayList<String>()
        val runtime = Runtime.getRuntime()
        val process: Process
        try {
            process = runtime.exec("pgrep -f " + killCommand.processNameOnUnix)
            process.waitFor()
            if (process.exitValue() != 0 && process.exitValue() != 1) {
                throw UnsupportedOperationException("pgrep returned error value!")
            }
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            bufferedReader.readLines().let {
                it.forEach { str ->
                    pids.add(str)
                }
            }
            bufferedReader.close()
        } catch (exception: IOException) {
            exception.printStackTrace()
            throw UnsupportedOperationException("pgrep parsing failed!")
        } catch (exception: InterruptedException) {
            exception.printStackTrace()
            throw UnsupportedOperationException("pgrep parsing failed!")
        }
        return pids.toTypedArray()
    }

    fun killProcess(killCommand: KillCommand) {
        val pids: Array<String>? = try {
            getPids(killCommand)
        } catch (var8: UnsupportedOperationException) {
            showErrorMessage("Could not get process data!")
            return
        }
        if (null == pids || pids.isEmpty()) {
            showErrorMessage(
                    "No " + killCommand.killWhat + " process is running!"
            )
        } else {
            var result = true
            for (pid in pids) {
                result = result and killProcess(pid)
            }
            if (result) {
                showInfo(
                        killCommand.killWhat + " was killed! Your IDE may show you some other dialogs, it's safe to ignore them."
                )
            } else {
                showErrorMessage(
                        "Could not kill " + killCommand.killWhat + "! Check that your system supports killing processes!"
                )
            }
        }
    }

    private fun getPidsOnWindows(killCommand: KillCommand): Array<String>? {
        val pids = ArrayList<String>()
        val runtime = Runtime.getRuntime()
        val process: Process
        try {
            process = runtime.exec(
                    "wmic process where " + killCommand.processWhereOnUnixOnWindows + " get processid"
            )
            process.waitFor()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            bufferedReader.readLines().let {
                it.forEach { str ->
                    pids.add(str)
                }
            }
            bufferedReader.close()
        } catch (e: IOException) {
            e.printStackTrace()
            throw UnsupportedOperationException("wmic parsing failed!")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw UnsupportedOperationException("wmic parsing failed!")
        }
        return pids.toTypedArray()
    }


    private fun killProcess(pid: String): Boolean {
        return if (isWindowsOS) killProcessOnWindows(
                pid
        ) else killProcessOnUnix(pid)
    }

    private fun killProcessOnWindows(pid: String): Boolean {
        val runtime = Runtime.getRuntime()
        val process: Process
        var result: Boolean
        try {
            process = runtime.exec("taskkill /F /PID $pid")
            process.waitFor()
            result = true
        } catch (var6: IOException) {
            var6.printStackTrace()
            result = false
        } catch (var6: InterruptedException) {
            var6.printStackTrace()
            result = false
        }
        return result
    }

    private fun killProcessOnUnix(pid: String): Boolean {
        val runtime = Runtime.getRuntime()
        val process: Process
        var result: Boolean
        try {
            process = runtime.exec("kill -9 $pid")
            process.waitFor()
            result = process.exitValue() == 0
        } catch (e: IOException) {
            e.printStackTrace()
            result = false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            result = false
        }
        return result
    }

    private fun processDeleteCommand(command: Command, dirPath: String?): Boolean {
        if (command.name.startsWith("delete") || command.name.startsWith("one")) {
            isBuildRunnerSuccess =
                    deleteLockFile(dirPath)
            return true
        }
        return false
    }

    private fun deleteLockFile(path: String?): Boolean {
        path?.let {
            val parentFile = File(path)
            if (parentFile.exists()) {
                for (file in parentFile.listFiles()) {
                    if (file.name == "pubspec.lock") {
                        return file.delete()
                    }
                }
            }
        }
        return false
    }

    private fun log(jTextArea: JTextArea?, verticalBar: JScrollBar?, message: String?) {
        if (!isEmptyString(message) && jTextArea != null && verticalBar != null) {
            val curMessage = currentTime + message
            jTextArea.append(
                    "\n" + curMessage
            )
            verticalBar.value = verticalBar.maximum
        } else {
            logger.error("log error")
        }
    }

    fun oneKeyExec(
            project: Project, dirPath: String?,
            command: Command, isSYN: Boolean
    ) {
        if (processDeleteCommand(command, dirPath)) {
            val toolWindow: ToolWindow? = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands")
            toolWindow?.let {
                toolWindow.show()
                val jScrollPane = toolWindow.contentManager.getContent(0)?.component?.getComponent(0) as JScrollPane
                val verticalBar = jScrollPane.verticalScrollBar
                val jTextArea = jScrollPane.viewport.getComponent(0) as JTextArea
                val commandName: String = command.name
                project.asyncTask(commandName,
                        runAction = {
                            val cmd = arrayOf(command.command[0], "clean")
                            val fillCmd = getCommand(cmd)
                            log(jTextArea, verticalBar, "$dirPath: $fillCmd")
                            try {
                                val process = Runtime.getRuntime().exec(cmd, null, File(dirPath))
                                val bufferedErrorStream = BufferedInputStream(process.errorStream)
                                val bufferedInputStream = BufferedInputStream(process.inputStream)
                                val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                                val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                                var lineStr: String?
                                while (bufferedInputReader.readLine().also {
                                            lineStr = it
                                        } != null) {
                                    log(jTextArea, verticalBar, lineStr)
                                }
                                while (
                                        bufferedErrorReader.readLine().also {
                                            lineStr = it
                                        } != null
                                ) {
                                    log(jTextArea, verticalBar, lineStr)
                                }
                                val code = process.waitFor()
                                if (code == 0) {
                                    isBuildRunnerSuccess = true
                                    log(
                                            jTextArea, verticalBar,
                                            "flutter clean Success! Exit with code: $code"
                                    )
                                } else {
                                    isBuildRunnerSuccess = false
                                    log(
                                            jTextArea, verticalBar,
                                            "flutter clean Error! Exit with code: $code"
                                    )
                                }
                                bufferedErrorStream.close()
                                bufferedInputStream.close()
                                bufferedErrorReader.close()
                                bufferedInputReader.close()
                            } catch (e: Throwable) {
                                isBuildRunnerSuccess = false
                                e.printStackTrace()
                            }
                        }, successAction = {
                    if (isBuildRunnerSuccess) {
                        flutterPubGet(
                                project,
                                dirPath,
                                command,
                                isSYN
                        )
                    } else {
                        showErrorMessage(
                                "An exception error occurred during command execution. " +
                                        "Please manually execute and resolve the error before using this plugin."
                        )
                    }
                }, failAction = {
                    showErrorMessage(
                            command.errorMessage + ", message:" + it.localizedMessage
                    )
                }, finishAction = {

                })
            }
        } else {
            showErrorMessage("delete lock file failed")
        }
    }


    private fun getCommand(command: Array<String>): String {
        val stringBuilder = StringBuilder()
        for (str in command) {
            stringBuilder.append(str)
            stringBuilder.append(" ")
        }
        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(" "))
        return stringBuilder.toString()
    }

     fun flutterPubGet(
            project: Project, dirPath: String?, command: Command, isSYN: Boolean
    ) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands")
        if (toolWindow != null) {
            toolWindow.show()
            val jScrollPane = toolWindow.contentManager.getContent(0)!!.component.getComponent(0) as JScrollPane
            val verticalBar = jScrollPane.verticalScrollBar
            val jTextArea = jScrollPane.viewport.getComponent(0) as JTextArea
            val commandName: String = command.name
            project.asyncTask(commandName, runAction = {
                val cmd = arrayOf(command.command[0], "pub", "get")
                val fillCmd = getCommand(cmd)
                log(jTextArea, verticalBar, "$dirPath: $fillCmd")
                val line = GeneralCommandLine()
                line.charset = CharsetToolkit.UTF8_CHARSET
                line.withEnvironment(FLUTTER_HOST_ENV, getFlutterHostEnvValue())
                line.exePath = cmd[0]
                dirPath?.let {
                    line.setWorkDirectory(dirPath)
                }
                line.addParameters(listOf("pub", "get"))
                val process = line.createProcess()
                val bufferedErrorStream = BufferedInputStream(process.errorStream)
                val bufferedInputStream = BufferedInputStream(process.inputStream)
                val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                var lineStr: String?
                while (bufferedInputReader.readLine().also {
                            lineStr = it
                        } != null) {
                    log(jTextArea, verticalBar, lineStr)
                }
                while (bufferedErrorReader.readLine().also {
                            lineStr = it
                        } != null) {
                    log(jTextArea, verticalBar, lineStr)
                }
                val code = process.waitFor()
                if (code == 0) {
                    log(
                            jTextArea, verticalBar,
                            "$commandName Success! Exit with code: $code"
                    )
                    isBuildRunnerSuccess = true
                } else {
                    isBuildRunnerSuccess = false
                    log(
                            jTextArea, verticalBar,
                            "$commandName Error! Exit with code: $code"
                    )
                }
                bufferedErrorStream.close()
                bufferedInputStream.close()
                bufferedErrorReader.close()
                bufferedInputReader.close()
            }, successAction = {
                if (isBuildRunnerSuccess) {
                    if (isSYN) {
                        if (!isWindowsOS) {
                            postGet(project, command)
                        } else {
                            showInfo(command.successMessage)
                        }
                    } else {
                        showInfo(command.successMessage)
                    }
                } else {
                    showErrorMessage(
                            "An exception error occurred during command execution. " +
                                    "Please manually execute and resolve the error before using this plugin."
                    )
                }
            }, failAction = {
                showErrorMessage(
                        command.errorMessage + ", message:" + it.localizedMessage
                )
            }, finishAction = {

            })
        }
    }

    fun postGet(project: Project, command: Command) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands")
        if (toolWindow != null) {
            toolWindow.show()
            val jScrollPane = toolWindow.contentManager.getContent(0)!!.component.getComponent(0) as JScrollPane
            val verticalBar = jScrollPane.verticalScrollBar
            val jTextArea = jScrollPane.viewport.getComponent(0) as JTextArea
            val commandName: String = command.name
            project.asyncTask(commandName, runAction = {
                val fillCmd = " ./postget.sh"
                log(jTextArea, verticalBar, project.basePath + ": " + fillCmd)
                val process = Runtime.getRuntime().exec(fillCmd, null, File(project.basePath))
                val bufferedErrorStream = BufferedInputStream(process.errorStream)
                val bufferedInputStream = BufferedInputStream(process.inputStream)
                val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                var lineStr: String?
                while (bufferedInputReader.readLine().also {
                            lineStr = it
                        } != null) {
                    log(jTextArea, verticalBar, lineStr)
                }
                while (bufferedErrorReader.readLine().also {
                            lineStr = it
                        } != null) {
                    log(jTextArea, verticalBar, lineStr)
                }
                val code = process.waitFor()
                if (code == 0) {
                    log(
                            jTextArea, verticalBar,
                            "postget Success! Exit with code: $code"
                    )
                    isBuildRunnerSuccess = true
                } else {
                    isBuildRunnerSuccess = false
                    log(
                            jTextArea, verticalBar,
                            "postget Error! Exit with code: $code"
                    )
                }
                bufferedErrorStream.close()
                bufferedInputStream.close()
                bufferedErrorReader.close()
                bufferedInputReader.close()
            }, successAction = {
                if (isBuildRunnerSuccess) {
                    showInfo(command.successMessage)
                } else {
                    showErrorMessage(
                            "An exception error occurred during command execution. " +
                                    "Please manually execute and resolve the error before using this plugin."
                    )
                }
            }, failAction = {
                showErrorMessage(
                        command.errorMessage + ", message:" + it.localizedMessage
                )
            }, finishAction = {
                isBuildRunnerSuccess = false
            })
        }
    }

    fun execCommand(
            project: Project, dirPath: String?, command: Command,
            successAction: (() -> Unit)? = null,
            failAction: (() -> Unit)? = null
    ) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands")
        if (toolWindow != null) {
            toolWindow.show()
            val jScrollPane = toolWindow.contentManager.getContent(0)?.component?.getComponent(0) as JScrollPane
            val verticalBar = jScrollPane.verticalScrollBar
            val jTextArea = jScrollPane.viewport.getComponent(0) as JTextArea
            val commandName: String = command.name
            project.asyncTask(commandName, runAction = {
                if (processDeleteCommand(command, dirPath)) {
                    return@asyncTask
                }
                val fillCmd: String = getCommand(command.command)
                log(jTextArea, verticalBar, "$dirPath: $fillCmd")
                try {
                    val process = Runtime.getRuntime().exec(fillCmd, null, File(dirPath))
                    val bufferedErrorStream = BufferedInputStream(process.errorStream)
                    val bufferedInputStream = BufferedInputStream(process.inputStream)
                    val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                    val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                    var lineStr: String?
                    while (bufferedInputReader.readLine().also {
                                lineStr = it
                            } != null) {
                        log(jTextArea, verticalBar, lineStr)
                    }
                    while (bufferedErrorReader.readLine().also {
                                lineStr = it
                            } != null) {
                        log(jTextArea, verticalBar, lineStr)
                    }
                    val code = process.waitFor()
                    if (code == 0) {
                        isBuildRunnerSuccess = true
                        log(
                                jTextArea, verticalBar,
                                "$commandName Success! Exit with code: $code"
                        )
                    } else {
                        isBuildRunnerSuccess = false
                        log(
                                jTextArea, verticalBar,
                                "$commandName Error! Exit with code: $code"
                        )
                    }
                } catch (e: Throwable) {
                    println(e.message)
                    isBuildRunnerSuccess = false
                    failAction?.invoke()
                }
            }, successAction = {
                if (isBuildRunnerSuccess) {
                    successAction?.invoke()
                    showInfo(command.successMessage)
                } else {
                    failAction?.invoke()
                    showErrorMessage("An exception error occurred during command execution. Please manually execute and resolve the error before using this plugin.")
                }
            }, failAction = {
                failAction?.invoke()
                showErrorMessage(
                        command.errorMessage + ", message:" + it.localizedMessage
                )
            }, finishAction = {
                isBuildRunnerSuccess = false
            })
        }
    }


    fun gitTag(
            project: Project, command: Array<String>,
            successAction: (() -> Unit)? = null,
            failAction: (() -> Unit)? = null
    ) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands")
        if (toolWindow != null) {
            toolWindow.show()
            val jScrollPane = toolWindow.contentManager.getContent(0)!!.component.getComponent(0) as JScrollPane
            val verticalBar = jScrollPane.verticalScrollBar
            val jTextArea = jScrollPane.viewport.getComponent(0) as JTextArea
            val commandName = "Git Tag"
            project.asyncTask(commandName,
                    runAction = {
                        log(
                                jTextArea,
                                verticalBar,
                                project.basePath + ": " + getCommand(command)
                        )
                        val process = Runtime.getRuntime().exec(command, null, File(project.basePath))
                        val bufferedErrorStream = BufferedInputStream(process.errorStream)
                        val bufferedInputStream = BufferedInputStream(process.inputStream)
                        val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                        val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                        var lineStr: String?
                        while (bufferedInputReader.readLine().also {
                                    lineStr = it
                                } != null) {
                            log(jTextArea, verticalBar, lineStr)
                        }
                        while (bufferedErrorReader.readLine().also {
                                    lineStr = it
                                } != null) {
                            log(jTextArea, verticalBar, lineStr)
                        }
                        val code = process.waitFor()
                        if (code == 0) {
                            isBuildRunnerSuccess = true
                            log(
                                    jTextArea, verticalBar,
                                    "$commandName Success! Exit with code: $code"
                            )
                        } else {
                            isBuildRunnerSuccess = false
                            log(
                                    jTextArea, verticalBar,
                                    "$commandName Error! Exit with code: $code"
                            )
                        }
                        bufferedErrorStream.close()
                        bufferedInputStream.close()
                        bufferedErrorReader.close()
                        bufferedInputReader.close()
                    },
                    successAction = {
                        successAction?.invoke()
                        if (!isBuildRunnerSuccess) {
                            showErrorMessage(command.toString() + "failed, see logs at CommonCommands window.")
                        }
                    }, failAction = {
                failAction?.invoke()
                showErrorMessage(command.toString() + ", message:" + it.localizedMessage)
            }, finishAction = {

            })
        }
    }

    fun pushTag(
            project: Project, command: String,
            successAction: (() -> Unit)? = null,
            failAction: (() -> Unit)? = null
    ) {
        val toolWindow: ToolWindow? = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands")
        toolWindow?.let {
            toolWindow.show()
            val jScrollPane = toolWindow.contentManager.getContent(0)!!.component.getComponent(0) as JScrollPane
            val verticalBar = jScrollPane.verticalScrollBar
            val jTextArea = jScrollPane.viewport.getComponent(0) as JTextArea
            val commandName = "Git Push Tag"
            project.asyncTask(commandName,
                    runAction = {
                        log(jTextArea, verticalBar, project.basePath + ": " + command)
                        val process = Runtime.getRuntime().exec(command, null, File(project.basePath))
                        val bufferedErrorStream = BufferedInputStream(process.errorStream)
                        val bufferedInputStream = BufferedInputStream(process.inputStream)
                        val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
                        val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
                        var lineStr: String?
                        while (bufferedInputReader.readLine().also {
                                    lineStr = it
                                } != null) {
                            log(jTextArea, verticalBar, lineStr)
                        }
                        while (bufferedErrorReader.readLine().also {
                                    lineStr = it
                                } != null) {
                            log(jTextArea, verticalBar, lineStr)
                        }
                        val code = process.waitFor()
                        if (code == 0) {
                            isBuildRunnerSuccess = true
                            log(
                                    jTextArea, verticalBar,
                                    "$commandName Success! Exit with code: $code"
                            )
                        } else {
                            isBuildRunnerSuccess = false
                            log(
                                    jTextArea, verticalBar,
                                    "$commandName Error! Exit with code: $code"
                            )
                        }
                        bufferedErrorStream.close()
                        bufferedInputStream.close()
                        bufferedErrorReader.close()
                        bufferedInputReader.close()
                    }, successAction = {
                if (isBuildRunnerSuccess) {
                    successAction?.invoke()
                    showInfo("Complete!\nRunning $command successfully.")
                } else {
                    failAction?.invoke()
                    showErrorMessage("An exception error occurred during command execution. Please manually execute and resolve the error before using this plugin.")
                }
            }, failAction = {
                failAction?.invoke()
                showErrorMessage(command + ", message:" + it.localizedMessage)
            }, finishAction = {
                isBuildRunnerSuccess = false
            })
        }
    }

    private fun Project.asyncTask(
            title: String,
            runAction: (ProgressIndicator) -> Unit,
            successAction: (() -> Unit)? = null,
            failAction: ((Throwable) -> Unit)? = null,
            finishAction: (() -> Unit)? = null
    ) {
        object : Task.Backgroundable(
                this, title, true,
                PerformInBackgroundOption.ALWAYS_BACKGROUND
        ) {
            override fun run(p0: ProgressIndicator) {
                return runAction.invoke(p0)
            }

            override fun onSuccess() {
                successAction?.invoke()
            }

            override fun onThrowable(error: Throwable) {
                failAction?.invoke(error)
            }

            override fun onFinished() {
                finishAction?.invoke()
            }
        }.queue()
    }

}