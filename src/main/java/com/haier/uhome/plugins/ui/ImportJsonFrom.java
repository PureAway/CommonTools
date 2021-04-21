package com.haier.uhome.plugins.ui;

import com.haier.uhome.plugins.utils.JSONParser4Dart;
import com.haier.uhome.plugins.utils.JSONParser4Java;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.Gray;
import net.sf.json.JSONObject;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ImportJsonFrom extends JFrame {
    private final Project project;
    private JPanel content;
    private JButton convert;
    private RTextScrollPane importPane;
    private RSyntaxTextArea importText;
    private final int DEFAULT_WIDTH = 900;
    private final int DEFAULT_HEIGHT = 500;
    private final Dimension defaultSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);


    public ImportJsonFrom(Project currentProject) {
        project = currentProject;
        initView();
        setFullScreenCenter();
        convert.addActionListener(e -> {
            if (Utils.isEmptyString(importText.getText())) {
                Utils.showErrorMessage("input json can not be null");
                return;
            }
            showConvertDialog();
            ImportJsonFrom.this.setVisible(false);
        });
    }


    private void initView() {
        add(content);
        setTitle("Json2Class");
        setSize(defaultSize);
        setMinimumSize(defaultSize);
        importPane.setViewportView(importText);
        importText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        importPane.setLineNumbersEnabled(true);
        importPane.setFoldIndicatorEnabled(true);
        importText.setCloseCurlyBraces(true);
        importText.setCloseMarkupTags(true);
        importText.setCloseMarkupTags(true);
        Gutter gutter = importPane.getGutter();
        gutter.setBackground(Gray._47);
    }

    private void setFullScreenCenter() {
        double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        setLocation((int) (width - this.getWidth()) / 2, (int) (height - this.getHeight()) / 2);
    }


    private void showConvertDialog() {
        final DialogBuilder dialogBuilder = new DialogBuilder();
        Json2ClassForm json2ClassForm = new Json2ClassForm(project);
        json2ClassForm.setOnGenerateClick((virtualFile, className, isDart, withJson) -> {
            dialogBuilder.getWindow().dispose();
            generateClassFile(virtualFile, className, isDart, withJson);
        });
        dialogBuilder.setCenterPanel(json2ClassForm.getRootView());
        dialogBuilder.setTitle("Json2Class");
        dialogBuilder.removeAllActions();
        dialogBuilder.show();
    }

    private void generateClassFile(VirtualFile virtualFile, String className, boolean isDart, boolean withJson) {
        if (!isDart) {
            generateJavaClass(virtualFile, className);
        } else {
            generateDartClass(virtualFile, className);
        }
    }

    private void generateDartClass(VirtualFile virtualFile, String className) {
        JSONParser4Dart parser = new JSONParser4Dart();
        String path = virtualFile.getPath();
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
        parser.reset(project, directory);
        parser.init(className);
        JSONObject dist = JSONObject.fromObject(importText.getText());
        String resultName = parser.decodeJSONObject(dist, true);
        if (null == resultName) {
            return;
        }
        String fileName = Utils.className2DartFileName(resultName);

        File file = new File(path + fileName + ".dart");
        if (file.exists()) {
            VirtualFile f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            if (f != null) {
                f.refresh(false, true);
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, f), true);
                FileEditorManager.getInstance(project).openFile(f, true);
            }
        }
    }

    private void generateJavaClass(VirtualFile virtualFile, String className) {
        JSONParser4Java parser = new JSONParser4Java();
        String path = virtualFile.getPath();
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
        parser.reset(project, directory);
        parser.init(className, true);
        parser.setGenGetter(true);
        parser.setGenSetter(true);
        JSONObject dist = JSONObject.fromObject(importText.getText());
        String resultName = parser.decodeJSONObject(dist);
        Messages.showInfoMessage(project, "Generating success!", "Success");
        File file = new File(path + resultName + ".java");
        if (file.exists()) {
            VirtualFile f = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            if (f != null) {
                f.refresh(false, true);
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, f), true);
            }
        }
    }
}
