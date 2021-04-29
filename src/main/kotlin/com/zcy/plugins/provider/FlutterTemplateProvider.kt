package com.zcy.plugins.provider

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class FlutterTemplateProvider : DefaultLiveTemplatesProvider {
    override fun getDefaultLiveTemplateFiles(): Array<String> {
        return arrayOf("liveTemplates/tools")
    }

    override fun getHiddenLiveTemplateFiles(): Array<String?>? {
        return null
    }
}