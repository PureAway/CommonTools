package com.zcy.plugins.ui

import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.openapi.project.Project
import com.zcy.plugins.utils.Utils
import java.awt.Dimension
import java.awt.Toolkit
import java.security.MessageDigest
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTextArea
import kotlin.experimental.and

class PsiSignForm(val project: Project) : JFrame("PSI Sign") {
    private lateinit var content: JPanel
    private lateinit var idText: JTextArea
    private lateinit var keyText: JTextArea
    private lateinit var clientText: JTextArea
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
        idText.preferredSize = dimension
        keyText.preferredSize = dimension
        clientText.preferredSize = dimension
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
            if (Utils.isEmptyString(idText.text)) {
                Utils.showErrorNotification(project, "appId can not be null")
                return@addActionListener
            }
            if (Utils.isEmptyString(keyText.text)) {
                Utils.showErrorNotification(project, "appKey can not be null")
                return@addActionListener
            }
            if (Utils.isEmptyString(clientText.text)) {
                Utils.showErrorNotification(project, "uHomeClient can not be null")
                return@addActionListener
            }
            getPsiSign()
        }
    }

    private fun getPsiSign() {
        val sign = getSign(idText.text, keyText.text, clientText.text)
        signText.text = sign
    }

    private fun getSign(appId: String, appKey: String, clientId: String): String {
        var aId = appId
        var aKey = appKey
        var cId = clientId
        aId = aId.trim { it <= ' ' }
        aId = aId.replace("\"".toRegex(), "")
        aKey = aKey.trim { it <= ' ' }
        aKey = aKey.replace("\"".toRegex(), "")
        cId = cId.trim { it <= ' ' }
        cId = cId.replace("\"".toRegex(), "")
        val sb = StringBuffer()
        sb.append(aId).append(aKey).append(cId)
        val md = MessageDigest.getInstance("SHA-256")
        val bytes: ByteArray?
        try {
            bytes = md.digest(sb.toString().toByteArray(charset("utf-8")))
        } catch (e: Exception) {
            e.printStackTrace()
            return "";
        }
        return binaryToHexString(bytes)
    }

    private fun binaryToHexString(bytes: ByteArray): String {
        val hex = StringBuilder()
        val hexStr = "0123456789abcdef"
        for (aByte in bytes) {
            hex.append(hexStr[(aByte.toInt() and 0xF0) shr 4])
            hex.append(hexStr[(aByte and 0x0F).toInt()])
        }
        return hex.toString()
    }

}