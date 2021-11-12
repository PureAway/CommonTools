package com.zcy.plugins.ui

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.ide.plugins.newui.VerticalLayout
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.content.ContentFactory
import com.zcy.plugins.ui.Json2ClassForm.OnGenerateClick
import com.zcy.plugins.utils.JSONParser4Dart
import com.zcy.plugins.utils.JSONParser4Java
import com.zcy.plugins.utils.Utils
import com.zcy.plugins.utils.Utils.className2DartFileName
import com.zcy.plugins.utils.Utils.isEmptyString
import com.zcy.plugins.utils.Utils.showErrorNotification
import io.objectbox.*
import io.objectbox.Box
import net.sf.json.JSONObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.apache.http.client.utils.URIBuilder
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.text.BadLocationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class HttpToolFactory : ToolWindowFactory {
    private lateinit var methodBox: JComboBox<String>
    private lateinit var url: JTextField
    private lateinit var send: JButton
    private lateinit var queryTable: JTable
    private lateinit var rootContent: JPanel
    private lateinit var queryAddBtn: JButton
    private lateinit var queryRemoveBtn: JButton
    private lateinit var uaBox: JComboBox<String>
    private lateinit var contentTypeBox: JComboBox<String>
    private lateinit var headersTable: JTable
    private lateinit var headersAddBtn: JButton
    private lateinit var headersRemoveBtn: JButton
    private lateinit var signBtn: JButton
    private lateinit var code: JLabel
    private lateinit var convertButton: JButton
    private lateinit var bodyPane: RTextScrollPane
    private lateinit var bodyText: RSyntaxTextArea
    private lateinit var requestPane: RTextScrollPane
    private lateinit var requestText: RSyntaxTextArea
    private lateinit var history: JPanel
    private lateinit var responseTab: JTabbedPane
    private lateinit var importJson: JButton
    private lateinit var generateRequest: JButton
    private lateinit var project: Project
    private var resultJson: String? = null
    private var clickTimes = 0
    private var store: BoxStore? = null
    private var box: Box<RequestHistory?>? = null
    private var requestHistory: RequestHistory? = null
    private var requestUrl: String? = null
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val file = File(project.workspaceFile?.parent?.canonicalPath)
        store = MyObjectBox.builder().directory(file).project(project).build()
        box = store?.boxFor(RequestHistory::class.java)
        this.project = project
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(rootContent, "", false)
        toolWindow.contentManager.addContent(content)
        convertButton.isVisible = false
        generateRequest.isVisible = false
        initTables()
        refreshHistoryList()
        initActionListeners()
        bodyPane.setViewportView(bodyText)
        bodyText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
        bodyPane.lineNumbersEnabled = true
        bodyPane.isFoldIndicatorEnabled = true
        bodyText.closeCurlyBraces = true
        bodyText.closeMarkupTags = true
        bodyText.closeMarkupTags = true
        requestPane.setViewportView(requestText)
        requestText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
        requestPane.lineNumbersEnabled = true
        requestPane.isFoldIndicatorEnabled = true
        requestText.closeCurlyBraces = true
        requestText.closeMarkupTags = true
        requestText.closeMarkupTags = true
        val signIcon = ImageIcon(javaClass.getResource("/icons/xixi.png"))
        signBtn.icon = signIcon
    }

    private fun initActionListeners() {
        send.addActionListener { submitRequest() }
        importJson.addActionListener {
            val importJsonFrom = ImportJsonFrom(project)
            importJsonFrom.isVisible = true
        }
        generateRequest.addActionListener {
            val generateRequestForm = GenerateRequestForm(requestHistory!!)
            generateRequestForm.isVisible = true
        }
        queryAddBtn.addActionListener { addTableRow(queryTable) }
        queryRemoveBtn.addActionListener { removeTableRow(queryTable) }
        headersAddBtn.addActionListener { addTableRow(headersTable) }
        headersRemoveBtn.addActionListener { removeTableRow(headersTable) }
        var gutter = bodyPane.gutter
        gutter.background = Gray._47
        gutter = requestPane.gutter
        gutter.background = Gray._47
        convertButton.addActionListener { showConvertDialog() }
        url.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                val document = e.document
                try {
                    val s = document.getText(0, document.length)
                    if (requestUrl != null && requestUrl == s) {
                        return
                    }
                    val cb = Toolkit.getDefaultToolkit().systemClipboard
                    requestText.text = ""
                    bodyText.text = ""
                    if (s == cb.getData(DataFlavor.stringFlavor)) {
                        initTables()
                        val u = URL(s)
                        convertButton.isVisible = false
                        generateRequest.isVisible = false
                        resultJson = null
                        val q = splitQuery(u)
                        for ((key, value) in q) {
                            (queryTable.model as DefaultTableModel).addRow(
                                arrayOf(
                                    key, value
                                )
                            )
                        }
                        SwingUtilities.invokeLater {
                            try {
                                document.remove(0, document.length)
                                val url = u.protocol + "://" + u.host + u.path
                                document.insertString(0, url, null)
                                requestUrl = url
                            } catch (e1: BadLocationException) {
                                e1.printStackTrace()
                            }
                        }
                    }
                } catch (e1: BadLocationException) {
                    e1.printStackTrace()
                } catch (e1: UnsupportedFlavorException) {
                    e1.printStackTrace()
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
            }

            override fun removeUpdate(e: DocumentEvent) {
            }

            override fun changedUpdate(e: DocumentEvent) {
            }
        })
        signBtn.addActionListener {
            clickTimes++
            if (clickTimes == 8) {
                clickTimes = 0
                showSignDialog()
            }
        }
    }


    private fun showSignDialog() {
        val dialogBuilder = DialogBuilder()
        val rootView = JPanel()
        rootView.preferredSize = Dimension(300, 150)
        rootView.layout = VerticalLayout(5)
        val psiBtn = JButton("Get PSI Sign")
        rootView.add(psiBtn)
        psiBtn.addActionListener {
            dialogBuilder.window.dispose()
            Utils.psiForm?.let {
                Utils.psiForm?.isVisible = false
                Utils.psiForm = null
            }
            Utils.psiForm = PsiSignForm(project)
            Utils.psiForm?.isVisible = true
        }
        val uhomeBtn = JButton("Get Uhome Sign")
        rootView.add(uhomeBtn)
        uhomeBtn.addActionListener {
            dialogBuilder.window.dispose()
            Utils.uhomeForm?.let {
                Utils.uhomeForm?.isVisible = false
                Utils.uhomeForm = null
            }
            Utils.uhomeForm = UhomeSignForm(project)
            Utils.uhomeForm?.isVisible = true
        }
        dialogBuilder.setCenterPanel(rootView)
        dialogBuilder.setTitle("Get Sign")
        dialogBuilder.removeAllActions()
        dialogBuilder.show()
    }

    private fun showConvertDialog() {
        val dialogBuilder = DialogBuilder()
        val json2ClassForm = Json2ClassForm(project)
        json2ClassForm.setOnGenerateClick(object : OnGenerateClick {
            override fun onClick(virtualFile: VirtualFile, className: String, isDart: Boolean, withJson: Boolean) {
                dialogBuilder.window.dispose()
                generateClassFile(virtualFile, className, isDart, withJson)
            }
        })
        dialogBuilder.setCenterPanel(json2ClassForm.rootView)
        dialogBuilder.setTitle("Json2Class")
        dialogBuilder.removeAllActions()
        dialogBuilder.show()
    }

    private fun generateClassFile(virtualFile: VirtualFile, className: String, isDart: Boolean, withJson: Boolean) {
        if (!isDart) {
            generateJavaClass(virtualFile, className)
        } else {
            generateDartClass(virtualFile, className, withJson)
        }
    }

    private fun generateDartClass(virtualFile: VirtualFile, className: String, withJson: Boolean) {
        val parser = JSONParser4Dart()
        val path = virtualFile.path
        val directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile)
        parser.reset(project, directory)
        parser.init(className)
        val dist = JSONObject.fromObject(resultJson)
        val resultName = parser.decodeJSONObject(dist, true) ?: return
        val fileName = className2DartFileName(resultName)
        val file = File("$path$fileName.dart")
        if (file.exists()) {
            val f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            ApplicationManager.getApplication().runReadAction {
                if (f != null) {
                    f.refresh(false, true)
                    FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, f), true)
                    FileEditorManager.getInstance(project).openFile(f, true)
                }
            }
        }
    }

    private fun generateJavaClass(virtualFile: VirtualFile, className: String) {
        val parser = JSONParser4Java()
        val path = virtualFile.path
        val directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile)
        parser.reset(project, directory)
        parser.init(className, true)
        parser.setGenGetter(true)
        parser.setGenSetter(true)
        val dist = JSONObject.fromObject(resultJson)
        val resultName = parser.decodeJSONObject(dist)
        val file = File(path, "$resultName.java")
        if (file.exists()) {
            val f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            ApplicationManager.getApplication().runReadAction {
                if (f != null) {
                    f.refresh(false, true)
                    Messages.showInfoMessage(project, "Generating success!", "Success")
                    FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, f), true)
                }
            }
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun splitQuery(url: URL): Map<String, String> {
        val query_pairs: MutableMap<String, String> = LinkedHashMap()
        val query = url.query
        if (query != null && !query.equals("", ignoreCase = true)) {
            val pairs = query.split("&".toRegex()).toTypedArray()
            if (pairs.isNotEmpty()) for (pair in pairs) {
                val idx = pair.indexOf("=")
                query_pairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] =
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }
        }
        return query_pairs
    }

    private fun addTableRow(table: JTable?) {
        (table!!.model as DefaultTableModel).addRow(arrayOf<String?>(null, null))
    }

    private fun removeTableRow(table: JTable?) {
        var row = table!!.selectedRow
        if (row == -1) {
            row = table.rowCount - 1
        }
        if (row > -1) {
            (table.model as DefaultTableModel).removeRow(row)
        }
    }

    private fun submitRequest() {
        if (isEmptyString(url.text)) {
            showErrorNotification(project, "url can not be null")
            return
        }
        responseTab.selectedIndex = 0
        val client = OkHttpClient()
        val builder = URIBuilder()
        val scheme = url.text.split(":".toRegex()).toTypedArray()[0].toLowerCase()
        builder.scheme = scheme
        val hp = url.text.split("//".toRegex()).toTypedArray()[1]
        val hostPath = hp.split("/".toRegex(), 1).toTypedArray()
        builder.host = hostPath[0]
        if (hostPath.size > 1) {
            builder.path = hostPath[1]
        }
        for (i in 0 until queryTable.rowCount) {
            try {
                builder.addParameter(queryTable.getValueAt(i, 0).toString(), queryTable.getValueAt(i, 1).toString())
            } catch (e: Exception) {
                e.printStackTrace()
                println(e.message)
            }
        }
        try {
            val uri = builder.build()
            var body: RequestBody? = null
            if (methodBox.selectedItem.toString().equals("POST", ignoreCase = true)) {
                body = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return contentTypeBox.selectedItem.toString().toMediaTypeOrNull()
                    }

                    override fun contentLength(): Long {
                        return requestText.text.toByteArray().size.toLong()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(requestText.text.encodeUtf8())
                    }
                }
            }
            val rb = Request.Builder().url(uri.toString()).method(
                methodBox.selectedItem.toString(), body
            )
            if (!contentTypeBox.selectedItem.toString()
                    .equals("", ignoreCase = true)
            ) {
                rb.addHeader("Content-Type", contentTypeBox.selectedItem.toString())
            }
            if (!uaBox.selectedItem.toString()
                    .equals("", ignoreCase = true)
            ) {
                rb.addHeader("User-Agent", uaBox.selectedItem.toString())
            }
            for (i in 0 until headersTable.rowCount) {
                try {
                    rb.addHeader(headersTable.getValueAt(i, 0).toString(), headersTable.getValueAt(i, 1).toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val request = rb.build()
            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    println("Error in request")
                    println(e.message)
                    showErrorNotification(project, "request error occurred!")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseCode = response.code
                    val responseHeaders = response.headers
                    code.text = responseCode.toString()
                    if (response.body != null) {
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            convertButton.isVisible = true
                            val requestHistory = RequestHistory()
                            requestHistory.method = methodBox.selectedItem.toString()
                            requestHistory.url = url.text
                            requestHistory.queryMaps = getMapValues(queryTable)
                            requestHistory.headers = getMapValues(headersTable)
                            requestHistory.bodyJson = requestText.text
                            requestHistory.date = Date()
                            val queryResult = box!!.query().equal(RequestHistory_.url, url.text).build().findFirst()
                            if (null != queryResult) {
                                requestHistory.id = queryResult.id
                            }
                            box!!.put(requestHistory)
                            this@HttpToolFactory.requestHistory = requestHistory
                            generateRequest.isVisible = true
                            refreshHistoryList()
                        }
                        try {
                            SwingUtilities.invokeAndWait {
                                for (i in 0 until responseHeaders.size) {
                                    bodyText.syntaxEditingStyle =
                                        response.body?.contentType()?.type + "/" + response.body?.contentType()?.subtype
                                    val responseBody = response.body
                                    var source: BufferedSource? = null
                                    try {
                                        source = responseBody?.source()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    try {
                                        source!!.request(Long.MAX_VALUE) // Buffer the entire body.
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    val buffer = source!!.buffer()
                                    var charset = StandardCharsets.UTF_8
                                    val contentType = responseBody?.contentType()
                                    if (contentType != null) {
                                        charset = contentType.charset(StandardCharsets.UTF_8)
                                    }
                                    var s = buffer.clone().readString(charset)
                                    try {
                                        bodyText.text = s
                                        if (response.body?.contentType()?.subtype.equals("json", ignoreCase = true)) {
                                            val parser = JsonParser()
                                            val el = parser.parse(s)
                                            val gson =
                                                GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls()
                                                    .create()
                                            s = gson.toJson(el)
                                            bodyText.text = s
                                            bodyText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
                                            resultJson = s
                                        } else if (response.body?.contentType()?.subtype
                                                .equals("xml", ignoreCase = true)
                                            || response.body?.contentType()?.subtype
                                                .equals("rss+xml", ignoreCase = true)
                                            || response.body?.contentType()?.subtype.equals("smil", ignoreCase = true)
                                        ) {
                                            val xmlInput: Source = StreamSource(StringReader(s))
                                            val stringWriter = StringWriter()
                                            val xmlOutput = StreamResult(stringWriter)
                                            val transformerFactory = TransformerFactory.newInstance()
                                            transformerFactory.setAttribute("indent-number", 4)
                                            val transformer = transformerFactory.newTransformer()
                                            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                                            transformer.transform(xmlInput, xmlOutput)
                                            val s2 = xmlOutput.writer.toString()
                                            bodyText.text = s2
                                            bodyText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_XML
                                            resultJson = null
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
            showErrorNotification(project, "request error occurred!")
        }
    }

    private fun refreshHistoryList() {
        val historyList = box!!.all
        if (null != historyList && historyList.isNotEmpty()) {
            historyList.reverse()
            history.removeAll()
            history.layout = VerticalLayout(2)
            for (requestHistory in historyList) {
                val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)
                val item = buildItemPanel(requestHistory, simpleDateFormat)
                history.add(item)
            }
        } else {
            history.removeAll()
        }
    }

    private fun buildItemPanel(requestHistory: RequestHistory?, simpleDateFormat: SimpleDateFormat): JPanel {
        val item = JPanel()
        item.border = BorderFactory.createLineBorder(JBColor.GRAY, 1)
        item.layout = HorizontalLayout(5)
        val date = JBLabel(simpleDateFormat.format(requestHistory!!.date))
        val method = JBLabel(requestHistory.method)
        if (requestHistory.method.equals("post", ignoreCase = true)) {
            method.foreground = JBColor.green
        } else if (requestHistory.method.equals("get", ignoreCase = true)) {
            method.foreground = JBColor.yellow
        } else if (requestHistory.method.equals("put", ignoreCase = true)) {
            method.foreground = JBColor.BLUE
        } else if (requestHistory.method.equals("delete", ignoreCase = true)) {
            method.foreground = JBColor.RED
        } else {
            method.foreground = JBColor.GRAY
        }
        val execIcon = ImageIcon(javaClass.getResource("/icons/run.png"))
        val execButton = JButton()
        execButton.preferredSize = Dimension(30, 30)
        execButton.icon = execIcon
        val deleteIcon = ImageIcon(javaClass.getResource("/icons/delete.png"))
        val deleteButton = JButton()
        deleteButton.preferredSize = Dimension(30, 30)
        deleteButton.icon = deleteIcon
        val url = JBLabel(requestHistory.url)
        item.add(date)
        item.add(method)
        item.add(url)
        item.add(execButton)
        item.add(deleteButton)
        execButton.addActionListener { importRequest(requestHistory) }
        deleteButton.addActionListener {
            box!!.remove(requestHistory)
            refreshHistoryList()
        }
        return item
    }

    private fun importRequest(requestHistory: RequestHistory?) {
        initTables()
        bodyText.text = ""
        requestText.text = ""
        methodBox.selectedItem = requestHistory!!.method.toUpperCase(Locale.CHINA)
        url.text = requestHistory.url
        if (null != requestHistory.queryMaps) {
            for ((key, value) in requestHistory.queryMaps) {
                (queryTable.model as DefaultTableModel).addRow(arrayOf(key, value))
            }
        }
        if (null != requestHistory.headers) {
            for ((key, value) in requestHistory.headers) {
                (headersTable.model as DefaultTableModel).addRow(arrayOf(key, value))
            }
        }
        if (!isEmptyString(requestHistory.bodyJson)) {
            println("ZCY " + requestHistory.bodyJson)
            requestText.text = requestHistory.bodyJson
        }
    }

    private fun getMapValues(input: JTable?): HashMap<String, String> {
        val hashMap = HashMap<String, String>()
        for (i in 0 until input!!.rowCount) {
            try {
                hashMap[input.getValueAt(i, 0).toString()] = input.getValueAt(i, 1).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                println(e.message)
            }
        }
        return hashMap
    }

    private fun initTables() {
        val columnNames = arrayOf("Key", "Value")
        val queryTableModel = DefaultTableModel(columnNames, 0)
        queryTable.model = queryTableModel
        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        queryTable.rowHeight = 30
        val headerTableModel = DefaultTableModel(columnNames, 0)
        headersTable.model = headerTableModel
        headersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        headersTable.rowHeight = 30
    }
}