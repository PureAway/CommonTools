package com.zcy.plugins.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.zcy.plugins.utils.Utils
import java.awt.event.ActionEvent
import javax.swing.*

class Json2ClassForm(project: Project?) {
    lateinit var rootView: JPanel
    private lateinit var className: JTextField
    private lateinit var generateButton: JButton
    private lateinit var groupContainer: JPanel
    private lateinit var pathContainer: JPanel
    private var onGenerateClick: OnGenerateClick? = null
    private var virtualFile: VirtualFile? = null
    fun setOnGenerateClick(onGenerateClick: OnGenerateClick?) {
        this.onGenerateClick = onGenerateClick
    }

    interface OnGenerateClick {
        fun onClick(virtualFile: VirtualFile, className: String, isDart: Boolean, withJson: Boolean)
    }

    init {
        val group = ButtonGroup()
        val dartBtn = JRadioButton("Dart", true)
        val javaBtn = JRadioButton("Java")
        group.add(dartBtn)
        group.add(javaBtn)
        groupContainer.add(dartBtn)
        groupContainer.add(javaBtn)
        val textFieldWithBrowseButton = TextFieldWithBrowseButton()
        pathContainer.add(textFieldWithBrowseButton)
        dartBtn.isContentAreaFilled = false
        dartBtn.isFocusPainted = false
        javaBtn.isContentAreaFilled = false
        javaBtn.isFocusPainted = false
        textFieldWithBrowseButton.addActionListener {
            val fileChooserDescriptor = FileChooserDescriptor(
                false, true, false,
                false, false, false
            )
            virtualFile = FileChooser.chooseFile(fileChooserDescriptor, rootView, project, null)
            virtualFile?.path?.let {
                textFieldWithBrowseButton.text = virtualFile!!.path
            }
        }
        generateButton.addActionListener {
            if (null == virtualFile) {
                Utils.showErrorNotification(project, "save location can not be empty")
                return@addActionListener
            }
            val saveLocation = virtualFile?.path?.trim { it <= ' ' }
            if (Utils.isEmptyString(saveLocation)) {
                Utils.showErrorNotification(project, "save location can not be empty")
                return@addActionListener
            }
            val fileName = className.text.trim { it <= ' ' }
            if (Utils.isEmptyString(fileName)) {
                Utils.showErrorNotification(project, "class name can not be empty")
                return@addActionListener
            }
            onGenerateClick?.let {
                onGenerateClick!!.onClick(virtualFile!!, fileName, dartBtn.isSelected, true)
            }
        }
    }
}