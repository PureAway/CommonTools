package com.zcy.plugins.ui

import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.openapi.project.Project
import com.zcy.plugins.utils.Utils
import java.awt.Dimension
import java.awt.Toolkit
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTextArea
import kotlin.experimental.and

class UhomeSignForm(val project: Project) : JFrame("Uhome Sign") {
    private lateinit var content: JPanel
    private lateinit var pathText: JTextArea
    private lateinit var idText: JTextArea
    private lateinit var keyText: JTextArea
    private lateinit var bodyText: JTextArea
    private lateinit var timeText: JTextArea
    private lateinit var signBtn: JButton
    private lateinit var signText: JTextArea
    private val DEFAULT_WIDTH = 900
    private val DEFAULT_HEIGHT = 500
    private val defaultSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)

    init {
        initView()
        setFullScreenCenter()
        addListener()
    }

    private fun initView() {
        add(content)
        size = defaultSize
        minimumSize = defaultSize
        content.layout = VerticalLayout(4)
        val dimension = Dimension(450, 30)
        pathText.preferredSize = dimension
        idText.preferredSize = dimension
        keyText.preferredSize = dimension
        timeText.preferredSize = dimension
        bodyText.preferredSize = Dimension(450, 100)
        signBtn.preferredSize = Dimension(120, 30)
        signText.preferredSize = Dimension(700, 100)
    }

    private fun setFullScreenCenter() {
        val width = Toolkit.getDefaultToolkit().screenSize.getWidth()
        val height = Toolkit.getDefaultToolkit().screenSize.getHeight()
        setLocation((width - this.width).toInt() / 2, (height - this.height).toInt() / 2)
    }

    private fun addListener() {
        signBtn.addActionListener {
            if (Utils.isEmptyString(pathText.text)) {
                Utils.showErrorNotification(project, "path can not be null")
                return@addActionListener
            }
            if (Utils.isEmptyString(idText.text)) {
                Utils.showErrorNotification(project, "appId can not be null")
                return@addActionListener
            }
            if (Utils.isEmptyString(keyText.text)) {
                Utils.showErrorNotification(project, "appKey can not be null")
                return@addActionListener
            }
            if (Utils.isEmptyString(bodyText.text)) {
                Utils.showErrorNotification(project, "bodyJson can not be null")
                return@addActionListener
            }
            if (Utils.isEmptyString(timeText.text)) {
                Utils.showErrorNotification(project, "timeStamp can not be null")
                return@addActionListener
            }
            getUhomeSign()
        }
    }

    private fun getUhomeSign() {
        val sign = createSign(pathText.text, bodyText.text, idText.text, keyText.text, timeText.text)
        signText.text = sign
    }


    private fun createSign(
        path: String, requestBody: String, appId: String, appKey: String,
        timestamp: String
    ): String {
        var body = requestBody
        val builder = StringBuilder()
        body = body.replace(" ".toRegex(), "")
        body = body.replace("\t".toRegex(), "")
        body = body.replace("\r".toRegex(), "")
        body = body.replace("\n".toRegex(), "")
        builder.append(path).append(body).append(appId).append(appKey).append(timestamp)
        val md5: String
        md5 = try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(builder.toString().toByteArray(charset("UTF-8")))
            binaryToHexString(bytes)
        } catch (e: NoSuchAlgorithmException) {
            ""
        } catch (e: UnsupportedEncodingException) {
            ""
        }
        return md5
    }

    private fun binaryToHexString(bytes: ByteArray): String {
        val hex = StringBuilder()
        val hexStr = "0123456789abcdef"
        for (aByte in bytes) {
            hex.append(hexStr[aByte.toInt() and 0xF0 shr 4])
            hex.append(hexStr[(aByte and 0x0F).toInt()])
        }
        return hex.toString()
    }
}