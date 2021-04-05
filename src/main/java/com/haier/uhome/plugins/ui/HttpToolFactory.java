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
    private JPanel responseTab;
    private JTable queryTable;
    private JPanel rootContent;
    private JButton queryAddBtn;
    private JButton queryRemoveBtn;
    private JComboBox uaBox;
    private JComboBox contentTypeBox;
    private JComboBox acceptBox;
    private JTable headersTable;
    private JButton headersAddBtn;
    private JButton headersRemoveBtn;
    private JRadioButton urlencodedRb;
    private JRadioButton formDataRb;
    private JButton bodyAddBtn;
    private JButton bodyRemoveBtn;
    private JTable bodyTable;
    private JTextArea code;
    private JTextArea bodyText;
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
