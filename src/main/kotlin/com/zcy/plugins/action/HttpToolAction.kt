package com.zcy.plugins.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.zcy.plugins.utils.Utils

class HttpToolAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        Utils.findGitPath(project)
        project?.let {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HttpClient")
            toolWindow?.show()
        }
    }
}