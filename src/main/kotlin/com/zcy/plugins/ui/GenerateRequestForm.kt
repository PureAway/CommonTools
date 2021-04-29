package com.zcy.plugins.ui

import com.intellij.ui.Gray
import com.zcy.plugins.utils.Utils.getFieldName
import com.zcy.plugins.utils.Utils.isEmptyString
import io.objectbox.RequestHistory
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Dimension
import java.awt.Toolkit
import java.net.MalformedURLException
import java.net.URL
import javax.swing.JFrame
import javax.swing.JPanel

class GenerateRequestForm(requestHistory: RequestHistory) : JFrame() {
    private lateinit var content: JPanel
    private lateinit var dartPane: RTextScrollPane
    private lateinit var dartText: RSyntaxTextArea
    private lateinit var javaPane: RTextScrollPane
    private lateinit var javaText: RSyntaxTextArea
    private val DEFAULT_WIDTH = 900
    private val DEFAULT_HEIGHT = 600
    private val defaultSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
    private fun generateJavaCode(requestHistory: RequestHistory) {
        try {
            val u = URL(requestHistory.url)
            val stringBuilder = StringBuilder("    @")
                .append(requestHistory.method)
                .append("(\"")
                .append(u.path)
                .append("\")\n")
                .append("    Observer<Response> ")
            val splitResult = requestHistory.url.split("/".toRegex()).toTypedArray()
            val methodName = splitResult[splitResult.size - 1]
            stringBuilder.append(methodName)
                .append("(\n            ")
                .append("@NonNull @Url String url,\n")
            val queryMaps = requestHistory.queryMaps
            if (null != queryMaps && !queryMaps.isEmpty()) {
                for ((key) in queryMaps) {
                    stringBuilder.append("            ")
                        .append("@NonNull @Query(\"")
                        .append(key)
                        .append("\") String ")
                        .append(getFieldName(key))
                        .append(",\n")
                }
            }
            val headerMaps = requestHistory.headers
            if (null != headerMaps && !headerMaps.isEmpty()) {
                for ((key) in headerMaps) {
                    stringBuilder.append("            ")
                        .append("@NonNull @Header(\"")
                        .append(key)
                        .append("\") String ")
                        .append(getFieldName(key))
                        .append(",\n")
                }
            }
            val bodyJson = requestHistory.bodyJson
            if (!isEmptyString(bodyJson)) {
                stringBuilder.append("            ")
                    .append("@NonNull @Body ")
                    .append("RequestBody requestBody);")
            } else {
                if (null != queryMaps && !queryMaps.isEmpty() || null != headerMaps && !headerMaps.isEmpty()) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"))
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","))
                }
                stringBuilder.append(");")
            }
            javaText.text = stringBuilder.toString()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            println(e.message)
        }
    }

    private fun generateDartCode(requestHistory: RequestHistory) {
        try {
            val u = URL(requestHistory.url)
            val stringBuilder = StringBuilder("  @Url('")
                .append(u.protocol)
                .append("://")
                .append(u.host)
                .append("')\n")
                .append("  @")
                .append(requestHistory.method)
                .append("('")
                .append(u.path)
                .append("')\n")
                .append("  Future<dynamic> ")
            val splitResult = requestHistory.url.split("/".toRegex()).toTypedArray()
            val methodName = splitResult[splitResult.size - 1]
            stringBuilder.append(methodName)
                .append("(\n")
                .append("      @BaseUrl() String baseUrl,\n")
            val queryMaps = requestHistory.queryMaps
            if (null != queryMaps && !queryMaps.isEmpty()) {
                for ((key) in queryMaps) {
                    stringBuilder.append("      ")
                        .append("@Query('")
                        .append(key)
                        .append("') String ")
                        .append(getFieldName(key))
                        .append(",\n")
                }
            }
            val headerMaps = requestHistory.headers
            if (null != headerMaps && !headerMaps.isEmpty()) {
                for ((key) in headerMaps) {
                    stringBuilder.append("      ")
                        .append("@Header('")
                        .append(key)
                        .append("') String ")
                        .append(getFieldName(key))
                        .append(",\n")
                }
            }
            val bodyJson = requestHistory.bodyJson
            if (!isEmptyString(bodyJson)) {
                stringBuilder.append("      ")
                    .append("@Body() ")
                    .append("Map<String, dynamic> requestMap);")
            } else {
                if (null != queryMaps && !queryMaps.isEmpty() || null != headerMaps && !headerMaps.isEmpty()) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"))
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","))
                }
                stringBuilder.append(");")
            }
            dartText.text = stringBuilder.toString()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            println(e.message)
        }
    }

    private fun initView() {
        add(content)
        title = "GenerateRequest"
        size = defaultSize
        minimumSize = defaultSize
        dartPane.setViewportView(dartText)
        dartText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_DART
        dartPane.lineNumbersEnabled = true
        dartPane.isFoldIndicatorEnabled = true
        dartText.closeCurlyBraces = true
        dartText.closeMarkupTags = true
        dartText.closeMarkupTags = true
        var gutter = dartPane.gutter
        gutter.background = Gray._47
        javaPane.setViewportView(javaText)
        javaText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA
        javaPane.lineNumbersEnabled = true
        javaPane.isFoldIndicatorEnabled = true
        javaText.closeCurlyBraces = true
        javaText.closeMarkupTags = true
        javaText.closeMarkupTags = true
        gutter = javaPane.gutter
        gutter.background = Gray._47
        javaText.text = "Coming soon"
    }

    private fun setFullScreenCenter() {
        val width = Toolkit.getDefaultToolkit().screenSize.getWidth()
        val height = Toolkit.getDefaultToolkit().screenSize.getHeight()
        setLocation((width - this.width).toInt() / 2, (height - this.height).toInt() / 2)
    }

    init {
        initView()
        setFullScreenCenter()
        generateDartCode(requestHistory)
        generateJavaCode(requestHistory)
    }
}