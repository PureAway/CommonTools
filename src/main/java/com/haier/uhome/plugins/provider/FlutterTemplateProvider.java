package com.haier.uhome.plugins.provider;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;

public class FlutterTemplateProvider implements DefaultLiveTemplatesProvider {

    @Override
    public String[] getDefaultLiveTemplateFiles() {
        return new String[]{"liveTemplates/tools"};
    }

    @Override
    public String[] getHiddenLiveTemplateFiles() {
        return null;
    }
}
