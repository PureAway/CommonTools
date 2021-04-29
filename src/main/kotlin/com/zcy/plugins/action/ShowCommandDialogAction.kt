package com.zcy.plugins.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.zcy.plugins.ui.CommandForm
import com.zcy.plugins.utils.Utils

class ShowCommandDialogAction : AnAction("Show Command Dialog") {

    override fun actionPerformed(e: AnActionEvent) {
        val virtualFile = e.getData(LangDataKeys.VIRTUAL_FILE)
        val project = e.project
        Utils.findGitPath(project)
        project?.let {
            val path: String? = virtualFile?.path ?: project.basePath
            showCommandForm(project, path)
        }
    }

    private fun showCommandForm(project: Project, path: String?) {
        Utils.commandForm?.let {
            Utils.commandForm?.isVisible = false
            Utils.commandForm = null
        }
        Utils.commandForm = CommandForm(project, path)
        Utils.commandForm?.isVisible = true
    }

}