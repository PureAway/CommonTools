package com.zcy.plugins.generate

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.util.IncorrectOperationException
import com.zcy.plugins.utils.Utils
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.util.*

class DartClassGenerator(private val project: Project, private val directory: PsiDirectory) {
    private var psiFile: PsiFile? = null
    fun append(s: String) {
        ApplicationManager.getApplication().runWriteAction {
            object : WriteCommandAction<Any?>(project) {
                override fun run(p0: Result<Any?>) {
                    val path = psiFile!!.virtualFile.path
                    try {
                        val fileWritter = FileWriter(path, true)
                        val bw = BufferedWriter(fileWritter)
                        bw.write(s)
                        bw.close()
                        val virtualFile = psiFile!!.virtualFile
                        if (null != virtualFile) {
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showInfoMessage(project, "Generating success!", "Success")
                                virtualFile.refresh(false, true)
                                FileEditorManager.getInstance(project)
                                    .openTextEditor(OpenFileDescriptor(project, virtualFile), true)
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }.execute()
        }
    }

    fun preGen(name: String): String? {
        val fileName: String = Utils.className2DartFileName(name)
        val psiFile = directory.findFile("$fileName.dart")
        if (psiFile != null) {
            Utils.showErrorMessage("File [$fileName.dart] already exists, assign [$name] to current class.")
            return null
        } else {
            this.psiFile = createClassFromTemplate(directory, fileName, emptyMap())
        }
        return name
    }

    @Throws(IncorrectOperationException::class)
    private fun createClassFromTemplate(
        dir: PsiDirectory, name: String,
        additionalProperties: Map<String, String>
    ): PsiFile? {
        val project = dir.project
        val template = FileTemplateManager.getInstance(project).getInternalTemplate("generator_dart")
        val defaultProperties = FileTemplateManager.getInstance(project).defaultProperties
        val properties = Properties(defaultProperties)
        properties.setProperty("NAME", name)
        val iterator: Iterator<*> = additionalProperties.entries.iterator()
        while (iterator.hasNext()) {
            val entry: java.util.Map.Entry<String, String> =
                iterator.next() as java.util.Map.Entry<String, String>
            properties.setProperty(entry.key, entry.value)
        }
        val ext = "dart"
        val fileName = "$name.$ext"
        val element: PsiElement
        element = try {
            FileTemplateUtil.createFromTemplate(template, fileName, properties, dir)
        } catch (var14: IncorrectOperationException) {
            throw var14
        } catch (var15: Exception) {
            return null
        }
        val psiFile = element.containingFile
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile)
        val virtualFile = psiFile.virtualFile
        return if (virtualFile != null) {
            pointer.element
        } else null
    }
}