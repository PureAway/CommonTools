package com.zcy.plugins.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.zcy.plugins.checker.ProjectChecker
import com.zcy.plugins.model.Command
import com.zcy.plugins.model.KillCommand
import com.zcy.plugins.sdk.FlutterSdk
import com.zcy.plugins.utils.Utils
import org.jdesktop.swingx.VerticalLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.util.*
import java.util.Timer
import javax.swing.*

class CommandForm(private val project: Project, private var path: String?) : JFrame("Common Commands") {
    private var content: JPanel? = null
    private val DEFAULT_WIDTH = 900
    private val DEFAULT_HEIGHT = 500
    private val defaultSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
    private var isGit: Boolean = false
    private var isFlutter: Boolean = false
    private var isSYN: Boolean = false
    private var postGetPath: String = ""
    private var flutterSdkPath: String = ""
    private var flutterKillCommand: KillCommand? = null
    private var flutterDataList: ArrayList<Command> = ArrayList()
    private var gitDataList: ArrayList<Command> = ArrayList()
    private var postGetDataList: ArrayList<Command> = ArrayList()
    private var adbDataList: ArrayList<Command> = ArrayList()
    private fun initView() {
        add(content)
        size = defaultSize
        minimumSize = defaultSize
    }

    private fun setFullScreenCenter() {
        val width = Toolkit.getDefaultToolkit().screenSize.getWidth()
        val height = Toolkit.getDefaultToolkit().screenSize.getHeight()
        setLocation((width - this.width).toInt() / 2, (height - this.height).toInt() / 2)
    }

    companion object {
        private val log = Logger.getInstance(CommandForm::class.java)
    }

    init {
        val flutterSdk: FlutterSdk? = FlutterSdk.getFlutterSdk(project)
        val checker = ProjectChecker()
        isFlutter = checker.checkFlutter(path)
        isGit = checker.checkoutGit(project)
        var flutterPath: String? = path!!
        if (!isFlutter) {
            flutterPath = Utils.findFlutterPath(project, path, checker)
            isFlutter = checker.checkFlutter(flutterPath)
        }
        if (isFlutter) {
            path = flutterPath
            flutterKillCommand = KillCommand(
                    "Flutter",
                    "flutter", "commandline name='flutter'"
            )
        }
        isSYN = checker.checkPostGet(project.basePath)
        if (isSYN) {
            postGetPath = project.basePath!!
        }
        flutterSdk?.let {
            flutterSdkPath =
                    flutterSdk.homePath + if (Utils.isWindowsOS) "/bin/flutter.bat" else "/bin/flutter"
            log.info("find flutter home : $flutterSdkPath")
        }
        initData()
        initView()
        setFullScreenCenter()
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                showData()
            }
        }, 200)
    }

    private fun showData() {
        val jbList = JPanel()
        jbList.layout = VerticalLayout(4)
        jbList.size = content!!.size
        if (isFlutter) {
            jbList.add(getTitleWithKillFlutterCommand())
            addSpace(jbList)
            for (command in flutterDataList) {
                val item: JPanel = buildItemPanel(command)
                jbList.add(item)
                addSpace(jbList)
            }
        }
        if (isGit) {
            jbList.add(getCommandTitle("Git"))
            addSpace(jbList)
            for (command in gitDataList) {
                val item: JPanel = buildItemPanel(command)
                jbList.add(item)
                addSpace(jbList)
            }
        }
        if (isSYN && !Utils.isWindowsOS) {
            jbList.add(getCommandTitle("SYN"))
            addSpace(jbList)
            for (command in postGetDataList) {
                val item: JPanel = buildItemPanel(command)
                jbList.add(item)
                addSpace(jbList)
            }
        }
        jbList.add(getCommandTitle("ADB"))
        addSpace(jbList)
        for (command in adbDataList) {
            val item: JPanel = buildItemPanel(command)
            jbList.add(item)
            addSpace(jbList)
        }
        jbList.background = JBColor.WHITE
        val scrollPane = JBScrollPane(jbList)
        val preferredSize = Dimension(content!!.width, content!!.height)
        scrollPane.preferredSize = preferredSize
        content!!.add(scrollPane)
        content!!.revalidate()
    }

    private fun getTitleWithKillFlutterCommand(): JPanel? {
        val item = JPanel()
        item.layout = HorizontalLayout(-60)
        val jbLabel = JBLabel("Flutter")
        jbLabel.font = Font(null, Font.BOLD, 40)
        jbLabel.horizontalAlignment = SwingConstants.CENTER
        jbLabel.preferredSize = Dimension(content!!.width, 60)
        item.add(jbLabel)
        val killIcon = ImageIcon(javaClass.getResource("/icons/flutter.png"))
        val killButton = JButton()
        killButton.setSize(48, 30)
        killButton.preferredSize = Dimension(48, 30)
        killButton.icon = killIcon
        killButton.toolTipText = "点击杀死flutter进程"
        killButton.addActionListener {
            Utils.killProcess(flutterKillCommand!!)
            this@CommandForm.isVisible = false
        }
        item.add(killButton)
        return item
    }

    private fun addSpace(jbList: JPanel) {
        val space = JPanel()
        space.preferredSize = Dimension(content!!.width - 16, 1)
        space.background = JBColor.border()
        jbList.add(space)
    }

    private fun getCommandTitle(title: String): JBLabel {
        val jbLabel = JBLabel(title)
        jbLabel.font = Font(null, Font.BOLD, 40)
        jbLabel.horizontalAlignment = SwingConstants.CENTER
        jbLabel.preferredSize = Dimension(content!!.width - 40, 60)
        return jbLabel
    }

    private fun buildItemPanel(command: Command): JPanel {
        val item = JPanel()
        item.layout = HorizontalLayout(-120)
        val name = JBLabel(command.name)
        name.font = Font(null, Font.BOLD, 18)
        name.horizontalAlignment = SwingConstants.CENTER
        name.preferredSize = Dimension(content!!.width, 32)
        item.add(name)
        val function = JPanel()
        function.layout = HorizontalLayout(10)
        function.preferredSize = Dimension(120, 32)
        item.add(function)
        val copyIcon = ImageIcon(javaClass.getResource("/icons/copy.png"))
        val copyButton = JButton()
        copyButton.preferredSize = Dimension(48, 30)
        copyButton.icon = copyIcon
        copyButton.toolTipText = "点击将会copy命令：" + command.cmd
        copyButton.addActionListener {
            val systemClipboard = toolkit.systemClipboard
            val content: Transferable = StringSelection(command.cmd)
            systemClipboard.setContents(content, null)
            Utils.showInfoNotification(project, "命令" + command.cmd.toString() + "已经拷贝至剪贴板")
            this@CommandForm.isVisible = false
        }
        function.add(copyButton)
        if (command.cmd.startsWith("delete")
                || command.name.startsWith("git")
                || command.cmd.startsWith("generate")) {
            copyButton.isVisible = false
        }
        val execIcon = ImageIcon(javaClass.getResource("/icons/run.png"))
        val execButton = JButton()
        execButton.preferredSize = Dimension(48, 30)
        execButton.icon = execIcon
        execButton.toolTipText = "点击将会执行命令：" + command.cmd
        execButton.addActionListener {
            when {
                command.name == "images_path_generator" -> {
                    Utils.generateImagesPath(path)
                }
                command.name == "export_path_generator" ->{
                    Utils.generateExportPath(path)
                }
                command.name == "one key exec" -> {
                    Utils.oneKeyExec(project, path, command, isSYN)
                }
                command.name.startsWith("git") -> {
                    this@CommandForm.isVisible = false
                    Utils.gitForm?.let {
                        Utils.gitForm?.isVisible = false
                        Utils.gitForm = null
                    }
                    Utils.gitForm = GitForm(project)
                    Utils.gitForm?.isVisible = true
                }
                command.name.startsWith("build_runner rebuild") -> {
                    Utils.oneKeyExec(project, path, command, false, finishAction = {
                        Utils.buildRunnerRebuild(project, path, command)
                    })
                }
                command.name.startsWith("flutter pub get") -> {
                    Utils.flutterPubGet(project, path, command = command, isSYN = isSYN)
                }
                else -> {
                    Utils.execCommand(project, path, command)
                }
            }
            this@CommandForm.isVisible = false
        }
        function.add(execButton)
        if (command.cmd.startsWith("adb")) {
            execButton.isVisible = false
        }
        item.preferredSize = Dimension(content!!.width - 40, 40)
        return item
    }

    private fun initData() {
        if (isFlutter) {
            flutterDataList.add(
                    Command(
                            "flutter clean",
                            arrayOf(flutterSdkPath, "clean"), "flutter clean"
                    )
            )
            flutterDataList.add(
                    Command(
                            "flutter pub get",
                            arrayOf(flutterSdkPath, "pub", "get"), "flutter pub get"
                    )
            )
            flutterDataList.add(
                    Command(
                            "delete lock file",
                            arrayOf("delete lock file"), "delete lock file"
                    )
            )
            flutterDataList.add(
                    Command(
                            "one key exec",
                            arrayOf(flutterSdkPath),
                            "delete lock file, then exec flutter clean、flutter pub get"
                    )
            )

            flutterDataList.add(
                    Command(
                            "build_runner rebuild",
                            arrayOf(flutterSdkPath, "pub", "run", "build_runner", "build", "--delete-conflicting-output"),
                            "flutter pub run build_runner build --delete-conflicting-outputs"
                    )
            )

            flutterDataList.add(
                    Command(
                            "images_path_generator",
                            arrayOf("generate images path at pubspec.yaml file"),
                            "generate images path at pubspec.yaml file"
                    )
            )

            flutterDataList.add(
                    Command(
                            "export_path_generator",
                            arrayOf("generate export path at library file"),
                            "generate export path at library file"
                    )
            )
        }
        if (isGit) {
            gitDataList.add(
                    Command(
                            "git tag",
                            arrayOf("git", "tag"),
                            "git tag"
                    )
            )
        }
        if (!Utils.isWindowsOS && isSYN) {
            postGetDataList.add(
                    Command(
                            "postget.sh",
                            arrayOf("./postget.sh"),
                            "./postget.sh"
                    )
            )
        }
        adbDataList.add(
                Command(
                        "adb logcat",
                        arrayOf("adb", "logcat", "-v", "time", ">", "log.txt"),
                        "adb logcat -v time > log.txt"
                )
        )
        adbDataList.add(
                Command(
                        "adb devices",
                        arrayOf("adb", "devices"),
                        "adb devices"
                )
        )
        adbDataList.add(
                Command(
                        "adb kill-server",
                        arrayOf("adb", "kill-server"),
                        "adb kill-server"
                )
        )
        adbDataList.add(
                Command(
                        "adb start-server",
                        arrayOf("adb", "start-server"),
                        "adb start-server"
                )
        )
    }
}