package com.zcy.plugins.generate

import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.zcy.plugins.utils.Utils
import java.util.*

class JavaClassGenerator(project: Project, dir: PsiDirectory) {
    private val dataSet: HashMap<String, PsiClass> = HashMap()
    private val project: Project
    private val directory: PsiDirectory
    private val factory: PsiElementFactory
    private var genGetter = false
    private var genSetter = false
    fun append(s: String, clsName: String) {
        ApplicationManager.getApplication().invokeLater {
            object : WriteCommandAction<Any?>(project) {
                override fun run(p0: Result<Any?>) {
                    val dist: PsiClass? = dataSet[clsName]
                    if (s.startsWith("//")) {
                        val comment: PsiElement = factory.createCommentFromText(s, dist)
                        dist?.addBefore(comment, dist.rBrace)
                    } else {
                        val r = s.replace("-".toRegex(), "_")
                        val field: PsiField = factory.createFieldFromText(r, dist)
                        dist?.add(field)
                        if (genGetter) {
                            val getter: PsiMethod = GenerateMembersUtil.generateGetterPrototype(field)
                            dist?.add(getter)
                        }
                        if (genSetter) {
                            val setter: PsiMethod = GenerateMembersUtil.generateSetterPrototype(field)
                            dist?.add(setter)
                        }
                    }
                    if (s.contains("List<")) {
                        val imports: Array<PsiImportStatement> =
                                (dist?.containingFile as PsiJavaFile).importList?.importStatements
                                        as Array<PsiImportStatement>
                        val searchScope = GlobalSearchScope.allScope(project)
                        val psiClasses: Array<PsiClass> =
                                PsiShortNamesCache.getInstance(project).getClassesByName("List", searchScope)
                        var isAdded = false
                        for (importStatement in imports) {
                            if (importStatement.qualifiedName.equals("java.util.List")) {
                                isAdded = true
                            }
                        }
                        if (!isAdded) {
                            for (psiClass in psiClasses) {
                                if (psiClass.qualifiedName.equals("java.util.List")) {
                                    val importStatement: PsiImportStatement =
                                            factory.createImportStatement(psiClass)
                                    (dist.containingFile as PsiJavaFile).importList?.add(importStatement)
                                    break
                                }
                            }
                        }
                    }
                }
            }.execute()
        }
    }

    fun preGen(name: String, last: String): String {
        var className = name
        val psiFile = directory.findFile("$name.java")
        var clazzName = name
        if (psiFile != null) {
            clazzName = last + name
            Utils.showErrorMessage("File [$name.java] already exists, assign [$clazzName] to current class.")
            if (directory.findFile("$clazzName.java") != null) {
                for (i in 1..50) {
                    if (directory.findFile("$clazzName$i.java") == null) {
                        Utils.showErrorMessage("File [$clazzName.java] already exists, assign [$clazzName$i] to current class.")
                        clazzName += i
                        className = clazzName
                        break
                    }
                }
            }
        }
        val clazz: PsiClass = JavaDirectoryService.getInstance().createClass(directory, clazzName, "generator_common")
        dataSet[name] = clazz
        return className
    }

    fun setGenGetter(genGetter: Boolean) {
        this.genGetter = genGetter
    }

    fun setGenSetter(genSetter: Boolean) {
        this.genSetter = genSetter
    }

    init {
        dataSet.clear()
        this.project = project
        directory = dir
        factory = JavaPsiFacade.getInstance(this.project).elementFactory
    }
}