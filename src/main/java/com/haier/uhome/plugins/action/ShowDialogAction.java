package com.haier.uhome.plugins.action;

import com.haier.uhome.plugins.ui.CommandForm;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ShowDialogAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = e.getData(LangDataKeys.VIRTUAL_FILE);
        Project project = e.getProject();
        Utils.findGitPath(project);
        if (null != project) {
            String path;
            if (null == virtualFile) {
                path = project.getBasePath();
            } else {
                path = virtualFile.getPath();
            }
            CommandForm commandForm = new CommandForm(project, path);
            commandForm.setVisible(true);
        }
    }
}
