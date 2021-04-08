package com.haier.uhome.plugins.ui;

import com.haier.uhome.plugins.utils.Utils;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;

public class Json2ClassForm {
    private JPanel rootView;
    private JCheckBox withJson;
    private JTextField className;
    private JButton generateButton;
    private JPanel groupContainer;
    private JPanel pathContainer;
    private OnGenerateClick onGenerateClick;

    public void setOnGenerateClick(OnGenerateClick onGenerateClick) {
        this.onGenerateClick = onGenerateClick;
    }

    public JPanel getRootView() {
        return rootView;
    }

    public Json2ClassForm(Project project) {
        ButtonGroup group = new ButtonGroup();
        JRadioButton dartBtn = new JRadioButton("Dart", true);
        JRadioButton javaBtn = new JRadioButton("Java");
        group.add(dartBtn);
        group.add(javaBtn);
        groupContainer.add(dartBtn);
        groupContainer.add(javaBtn);
        TextFieldWithBrowseButton textFieldWithBrowseButton = new TextFieldWithBrowseButton();
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false,
                false, false, false);
        textFieldWithBrowseButton.addBrowseFolderListener("Save location", project.getBasePath(), null, fileChooserDescriptor);
        pathContainer.add(textFieldWithBrowseButton);
        dartBtn.setContentAreaFilled(false);
        dartBtn.setFocusPainted(false);
        javaBtn.setContentAreaFilled(false);
        javaBtn.setFocusPainted(false);
        dartBtn.addChangeListener(e -> withJson.setVisible(dartBtn.isSelected()));
        generateButton.addActionListener(e -> {
            String saveLocation = textFieldWithBrowseButton.getText().trim();
            if (Utils.isEmptyString(saveLocation)) {
                Utils.showErrorNotification(project, "save location can not be empty");
                return;
            }
            String fileName = className.getText().trim();
            if (Utils.isEmptyString(fileName)) {
                Utils.showErrorNotification(project, "class name can not be empty");
                return;
            }
            if (onGenerateClick != null) {
                onGenerateClick.onClick(saveLocation, fileName, dartBtn.isSelected(), withJson.isSelected());
            }
        });
    }


    public interface OnGenerateClick {
        void onClick(String saveLocation, String className, boolean isDart, boolean withJson);
    }


}
