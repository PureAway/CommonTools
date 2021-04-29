package com.zcy.plugins.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.event.ChangeListener

class ConsoleToolFactory : ToolWindowFactory {
    private lateinit var mPanel: JPanel
    private lateinit var mScrollPane: JScrollPane
    lateinit var mTxtContent: JTextArea
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(mPanel, "CommonCommands", false)
        toolWindow.contentManager.addContent(content)
        // 禁止编辑
        mTxtContent.isEditable = false

        // 去除边框
        mTxtContent.border = BorderFactory.createLineBorder(JBColor.BLACK, 0)
        mScrollPane.border = BorderFactory.createLineBorder(JBColor.BLACK, 0)
        mPanel.border = BorderFactory.createLineBorder(JBColor.BLACK, 0)
        // 设置透明
        mPanel.isOpaque = false
        mScrollPane.isOpaque = false
        mScrollPane.viewport.isOpaque = false
        mTxtContent.isOpaque = false

        // 鼠标事件
        mTxtContent.removeMouseListener(mouseListener)
        mTxtContent.addMouseListener(mouseListener)

        // 鼠标事件
        mTxtContent.removeMouseListener(mouseAdapter)
        mTxtContent.addMouseListener(mouseAdapter)

        // 输入变化事件
        mTxtContent.caret.removeChangeListener(changeListener)
        mTxtContent.caret.addChangeListener(changeListener)
    }

    /**
     * 鼠标进出/入事件
     */
    private val mouseAdapter: MouseAdapter = object : MouseAdapter() {
        override fun mouseEntered(mouseEvent: MouseEvent) {
            // 鼠标进入Text区后变为文本输入指针
            mTxtContent.cursor = Cursor(Cursor.TEXT_CURSOR)
        }

        override fun mouseExited(mouseEvent: MouseEvent) {
            // 鼠标离开Text区后恢复默认形态
            mTxtContent.cursor = Cursor(Cursor.DEFAULT_CURSOR)
        }
    }

    /**
     * 鼠标改变事件
     */
    private val changeListener = ChangeListener { // 使Text区的文本光标显示
        mTxtContent.caret.isVisible = true
    }

    /**
     * 鼠标右键事件
     */
    private val mouseListener: MouseListener = object : MouseListener {
        override fun mouseClicked(e: MouseEvent) {
            if (e.button == 3) { // 鼠标右键
                // 添加右键菜单的内容
                val list = JBList<String?>()
                val title = arrayOfNulls<String>(2)
                title[0] = "    Select All"
                title[1] = "    Clear All"

                // 设置数据
                list.setListData(title)
                list.isFocusable = false

                // 设置边框
                val lineBorder = BorderFactory.createLineBorder(JBColor.BLACK, 1)
                list.border = lineBorder

                // 创建菜单 添加点击项的监听事件
                val popup = PopupChooserBuilder<Any?>(list)
                    .setItemChoosenCallback {
                        val value = list.selectedValue
                        if (value!!.contains("Clear All")) {
                            mTxtContent.text = ""
                        } else if (value.contains("Select All")) {
                            mTxtContent.selectAll()
                        }
                    }.createPopup()

                // 设置大小
                val dimension = popup.content.preferredSize
                popup.size = Dimension(150, dimension.height)

                // 传入e，获取位置进行显示
                popup.show(RelativePoint(e))
                list.clearSelection()

                // 添加鼠标进入List事件
                list.addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        super.mouseEntered(e)
                    }

                    override fun mouseExited(e: MouseEvent) {
                        super.mouseExited(e)
                        list.clearSelection()
                    }
                })
            }
        }

        override fun mousePressed(e: MouseEvent) {}
        override fun mouseReleased(e: MouseEvent) {}
        override fun mouseEntered(e: MouseEvent) {}
        override fun mouseExited(e: MouseEvent) {}
    }
}