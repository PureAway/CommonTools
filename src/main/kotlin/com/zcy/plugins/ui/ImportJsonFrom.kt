package com.zcy.plugins.ui

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.ui.Gray
import com.zcy.plugins.ui.Json2ClassForm.OnGenerateClick
import com.zcy.plugins.utils.JSONParser4Dart
import com.zcy.plugins.utils.JSONParser4Java
import com.zcy.plugins.utils.Utils.className2DartFileName
import com.zcy.plugins.utils.Utils.isEmptyString
import com.zcy.plugins.utils.Utils.showErrorMessage
import net.sf.json.JSONObject
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel

class ImportJsonFrom(private val project: Project) : JFrame() {
    private lateinit var content: JPanel
    private lateinit var convert: JButton
    private lateinit var importPane: RTextScrollPane
    private lateinit var importText: RSyntaxTextArea
    private val DEFAULT_WIDTH = 900
    private val DEFAULT_HEIGHT = 500
    private val defaultSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
    private fun initView() {
        add(content)
        title = "Json2Class"
        size = defaultSize
        minimumSize = defaultSize
        importPane.setViewportView(importText)
        importText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
        importPane.lineNumbersEnabled = true
        importPane.isFoldIndicatorEnabled = true
        importText.closeCurlyBraces = true
        importText.closeMarkupTags = true
        importText.closeMarkupTags = true
        val gutter = importPane.gutter
        gutter.background = Gray._47
    }

    private fun setFullScreenCenter() {
        val width = Toolkit.getDefaultToolkit().screenSize.getWidth()
        val height = Toolkit.getDefaultToolkit().screenSize.getHeight()
        setLocation((width - this.width).toInt() / 2, (height - this.height).toInt() / 2)
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
            generateDartClass(virtualFile, className)
        }
    }

    private fun generateDartClass(virtualFile: VirtualFile, className: String) {
        val parser = JSONParser4Dart()
        val path = virtualFile.path
        val directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile)
        parser.reset(project, directory)
        parser.init(className)
        val dist: JSONObject = JSONObject.fromObject(importText.text)
        val resultName: String = parser.decodeJSONObject(dist, true) ?: return
        val fileName = className2DartFileName(resultName)
        val file = File("$path$fileName.dart")
        if (file.exists()) {
            val f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            if (f != null) {
                f.refresh(false, true)
                FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, f), true)
                FileEditorManager.getInstance(project).openFile(f, true)
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
        val dist: JSONObject = JSONObject.fromObject(importText.text)
        val resultName: String = parser.decodeJSONObject(dist)
        Messages.showInfoMessage(project, "Generating success!", "Success")
        val file = File("$path$resultName.java")
        if (file.exists()) {
            val f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            if (f != null) {
                f.refresh(false, true)
                FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, f), true)
            }
        }
    }

    init {
        initView()
        setFullScreenCenter()
        convert.addActionListener {
            if (isEmptyString(importText.text)) {
                showErrorMessage("input json can not be null")
                return@addActionListener
            }
            showConvertDialog()
            this@ImportJsonFrom.isVisible = false
        }
    }
}