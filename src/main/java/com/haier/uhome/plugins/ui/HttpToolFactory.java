package com.haier.uhome.plugins.ui;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class HttpToolFactory implements ToolWindowFactory {
    private JComboBox method;
    private JTextField url;
    private JButton send;
    private JTabbedPane requestTab;
    private JPanel requestParams;
    private JPanel requestHeaders;
    private JPanel requestBody;
    private JPanel responseTab;
    private JTable queryTable;
    private JPanel rootContent;
    private JButton queryAddBtn;
    private JButton queryRemoveBtn;
    private JTextArea textArea1;
    private ToolWindow myToolWindow;
    private Project project;


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        myToolWindow = toolWindow;
        this.project = project;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(rootContent, "", false);
        toolWindow.getContentManager().addContent(content);

        String columnNames[] = {"KEY", "VALUE"};
        DefaultTableModel table_model = new DefaultTableModel(columnNames, 1);
        queryTable.setModel(table_model);
        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queryTable.setRowHeight(30);

    }
}
