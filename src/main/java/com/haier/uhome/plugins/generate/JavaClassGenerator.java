package com.haier.uhome.plugins.generate;

import com.haier.uhome.plugins.utils.Utils;
import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class JavaClassGenerator {
    private Map<String, PsiClass> dataSet = new HashMap<>();
    private Project project;
    private PsiDirectory directory;
    private PsiElementFactory factory;
    private boolean genGetter;
    private boolean genSetter;

    public JavaClassGenerator(Project proj, PsiDirectory dir) {
        dataSet.clear();
        project = proj;
        directory = dir;
        factory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    public void append(final String s, final String clsName) {
        ApplicationManager.getApplication().invokeLater(() -> new WriteCommandAction(project) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                PsiClass dist = dataSet.get(clsName);
                if (s.startsWith("//")) {
                    PsiElement comment = factory.createCommentFromText(s, dist);
                    dist.addBefore(comment, dist.getRBrace());
                } else {
                    String r = s.replaceAll("-", "_");
                    PsiField field = factory.createFieldFromText(r, dist);
                    dist.add(field);
                    if (genGetter) {
                        PsiMethod getter = GenerateMembersUtil.generateGetterPrototype(field);
                        dist.add(getter);
                    }
                    if (genSetter) {
                        PsiMethod setter = GenerateMembersUtil.generateSetterPrototype(field);
                        dist.add(setter);
                    }
                }

                if (s.contains("List<")) {
                    PsiImportStatement[] imports = ((PsiJavaFile) dist.getContainingFile()).getImportList().getImportStatements();
                    GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
                    PsiClass[] psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName("List", searchScope);
                    if (imports != null) {
                        boolean isAdded = false;
                        for (PsiImportStatement importStatement : imports) {
                            if (importStatement.getQualifiedName().equals("java.util.List")) {
                                isAdded = true;
                            }
                        }
                        if (!isAdded) {
                            for (PsiClass psiClass : psiClasses) {
                                if (psiClass.getQualifiedName().equals("java.util.List")) {
                                    PsiImportStatement importStatement = factory.createImportStatement(psiClass);
                                    ((PsiJavaFile) dist.getContainingFile()).getImportList().add(importStatement);
                                    break;
                                }
                            }
                        }
                    } else {
                        for (PsiClass psiClass : psiClasses) {
                            if (psiClass.getQualifiedName().equals("java.util.List")) {
                                PsiImportStatement importStatement = factory.createImportStatement(psiClass);
                                ((PsiJavaFile) dist.getContainingFile()).getImportList().add(importStatement);
                                break;
                            }
                        }
                    }
                }
            }
        }.execute());
    }

    public String preGen(String name, String last) {
        String className = name;
        final PsiFile psiFile = directory.findFile(name + ".java");
        String clazzName = name;
        if (psiFile != null) {
            clazzName = last + name;
            Utils.showErrorMessage("File [" + name + ".java" + "] already exists, assign [" + clazzName + "] to current class.");
            if (directory.findFile(clazzName + ".java") != null) {
                for (int i = 1; i <= 50; i++) {
                    if (directory.findFile(clazzName + i + ".java") == null) {
                        Utils.showErrorMessage("File [" + clazzName + ".java" + "] already exists, assign [" + clazzName + i + "] to current class.");
                        clazzName = clazzName + i;
                        className = clazzName;
                        break;
                    }
                }
            }
        }

        final PsiClass clazz = JavaDirectoryService.getInstance().createClass(directory, clazzName, "generator_common");
        dataSet.put(name, clazz);
        return className;
    }

    public void setGenGetter(boolean genGetter) {
        this.genGetter = genGetter;
    }

    public void setGenSetter(boolean genSetter) {
        this.genSetter = genSetter;
    }
}
