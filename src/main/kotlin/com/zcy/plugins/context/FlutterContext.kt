package com.zcy.plugins.context

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile

class FlutterContext protected constructor() : TemplateContextType("FLUTTER", "Flutter") {
    override fun isInContext(file: PsiFile, offset: Int): Boolean {
        return file.name.endsWith(".dart")
    }
}