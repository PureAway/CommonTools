package com.haier.uhome.plugins.generate;

import com.haier.uhome.plugins.utils.Utils;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DartClassGenerator {

    private PsiFile psiFile;
    private Project project;
    private PsiDirectory directory;
    private PsiElementFactory factory;

    public DartClassGenerator(Project proj, PsiDirectory dir) {
        psiFile = null;
        project = proj;
        directory = dir;
    }

    public void append(final String s) {
        ApplicationManager.getApplication().invokeLater(() -> new WriteCommandAction(project) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {

            }
        }.execute());
    }

    public String preGen(String name) {
        String fileName = Utils.className2DartFileName(name);
        final PsiFile psiFile = directory.findFile(fileName + ".dart");
        if (psiFile != null) {
            Utils.showErrorMessage("File [" + fileName + ".dart" + "] already exists, assign [" + name + "] to current class.");
            return null;
        } else {
            this.psiFile = createClassFromTemplate(directory, fileName, Collections.emptyMap());
        }
        return name;
    }

    private PsiFile createClassFromTemplate(@NotNull PsiDirectory dir, String name,
                                            @NotNull Map<String,
                                                    String> additionalProperties) throws IncorrectOperationException {
        Project project = dir.getProject();
        FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate("generator_dart");
        Properties defaultProperties = FileTemplateManager.getInstance(project).getDefaultProperties();
        Properties properties = new Properties(defaultProperties);
        Iterator iterator = additionalProperties.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry) iterator.next();
            properties.setProperty(entry.getKey(), entry.getValue());
        }
        String ext = "dart";
        String fileName = name + "." + ext;

        PsiElement element;
        try {
            element = FileTemplateUtil.createFromTemplate(template, fileName, properties, dir);
        } catch (IncorrectOperationException var14) {
            throw var14;
        } catch (Exception var15) {
            return null;
        }
        PsiFile psiFile = element.getContainingFile();
        SmartPsiElementPointer<PsiFile> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile);
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null) {
            return pointer.getElement();
        }
        return null;
    }

}
