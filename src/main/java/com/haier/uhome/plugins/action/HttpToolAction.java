package com.haier.uhome.plugins.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class HttpToolAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (null != project) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("HttpClient");
            if (null != toolWindow) {
                toolWindow.show();
            }
        }
    }
}
