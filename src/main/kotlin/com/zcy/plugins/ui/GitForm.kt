package com.zcy.plugins.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.zcy.plugins.model.Command
import com.zcy.plugins.utils.Utils
import java.awt.Dimension
import java.awt.Toolkit
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import javax.swing.*
import kotlin.Comparator
import kotlin.math.min

class GitForm(project: Project) : JFrame() {
    private lateinit var content: JPanel
    private lateinit var nextVersion: JTextArea
    private lateinit var mStr: JTextArea
    private lateinit var commit: JButton
    private lateinit var rightContainer: JPanel
    private lateinit var lastTag: JLabel
    private lateinit var tagText: JTextArea
    private var pull: JButton? = null
    private val DEFAULT_WIDTH = 900
    private val DEFAULT_HEIGHT = 600
    private val defaultSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
    private var lastVersion: String? = null
    private fun showTags(project: Project) {
        if (Utils.isEmptyString(Utils.gitPath)) {
            Utils.showErrorMessage("can not find git environment in your home path")
            return
        }
        try {
            val fillCmd = "git tag"
            val process = Runtime.getRuntime().exec(fillCmd, null, File(project.basePath))
            val bufferedErrorStream = BufferedInputStream(process.errorStream)
            val bufferedInputStream = BufferedInputStream(process.inputStream)
            val bufferedErrorReader = BufferedReader(InputStreamReader(bufferedErrorStream, "utf-8"))
            val bufferedInputReader = BufferedReader(InputStreamReader(bufferedInputStream, "utf-8"))
            var lineStr: String?
            val tags = ArrayList<String>()
            while (bufferedInputReader.readLine().also { lineStr = it } != null) {
                lineStr?.let { tags.add(it) }
            }
            tags.sortWith(Comparator { s1: String, s2: String ->
                try {
                    val s1Split = s1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val s2Split = s2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val len1 = s1Split.size
                    val len2 = s2Split.size
                    val lim = min(len1, len2)
                    var i = 0
                    while (i < lim) {
                        val c1 = if ("" == s1Split[i]) 0 else s1Split[i].toInt()
                        val c2 = if ("" == s2Split[i]) 0 else s2Split[i].toInt()
                        if (c1 != c2) {
                            return@Comparator c1 - c2
                        }
                        i++
                    }
                    return@Comparator len1 - len2
                } catch (throwable: Throwable) {
                    return@Comparator 0
                }
            })
            val stringBuilder = StringBuilder()
            for (str in tags) {
                stringBuilder.append(str)
                    .append("\n")
            }
            val code = process.waitFor()
            if (code == 0) {
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"))
                tagText.text = stringBuilder.toString()
                val index = stringBuilder.lastIndexOf("\n")
                lastVersion = stringBuilder.substring(index)
                lastTag.text = lastVersion
                nextVersion.text = getNextVersion()
            }
            bufferedErrorStream.close()
            bufferedInputStream.close()
            bufferedErrorReader.close()
            bufferedInputReader.close()
        } catch (e: Throwable) {
            println(e.message)
            e.printStackTrace()
        }
    }

    private fun getNextVersion(): String {
        val result = lastVersion!!.split("\\.".toRegex()).toTypedArray()
        if (result.size > 1) {
            val last = result[result.size - 1]
            if (Utils.isInteger(last)) {
                val lastVersion = last.toInt()
                val nextVersion = lastVersion + 1
                val stringBuilder = StringBuilder()
                for (i in 0 until result.size - 1) {
                    stringBuilder.append(result[i])
                        .append(".")
                }
                stringBuilder.append(nextVersion)
                return stringBuilder.toString().replace("\n", "")
            }
        }
        return lastVersion + "_next"
    }

    private fun initView(project: Project) {
        add(content)
        title = "Git Tag"
        size = defaultSize
        minimumSize = defaultSize
        rightContainer.layout = VerticalFlowLayout(4)
        val dimension = Dimension(-1, 36)
        lastTag.preferredSize = dimension
        nextVersion.preferredSize = dimension
        mStr.preferredSize = dimension
        commit.preferredSize = Dimension(-1, 40)
        pull!!.addActionListener {
            val command = Command(
                "git pull",
                arrayOf("git", "pull", "--rebase"),
                "git pull --rebase"
            )
            Utils.execCommand(project, project.basePath, command,
                successAction = {
                    showTags(project)
                }, failAction = {

                })
        }
        commit.addActionListener {
            val cmd = ArrayList<String>()
            cmd.add("git")
            cmd.add("tag")
            if (Utils.isEmptyString(mStr.text)) {
                cmd.add(nextVersion.text!!)
            } else {
                cmd.add("-a")
                cmd.add(nextVersion.text)
                cmd.add("-m")
                cmd.add(mStr.text)
            }
            Utils.gitTag(project, cmd.toTypedArray(),
                successAction = {
                    val com = "git push origin " + nextVersion.text
                    Utils.pushTag(project, com, successAction = {
                        showTags(project)
                    }, failAction = {

                    })
                },
                failAction = {

                })
        }
    }

    private fun setFullScreenCenter() {
        val width = Toolkit.getDefaultToolkit().screenSize.getWidth()
        val height = Toolkit.getDefaultToolkit().screenSize.getHeight()
        setLocation((width - this.width).toInt() / 2, (height - this.height).toInt() / 2)
    }

    init {
        initView(project)
        setFullScreenCenter()
        showTags(project)
    }
}