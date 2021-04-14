package com.haier.uhome.plugins.ui;

import com.google.gson.*;
import com.haier.uhome.plugins.model.MyObjectBox;
import com.haier.uhome.plugins.model.RequestHistory;
import com.haier.uhome.plugins.model.RequestHistory_;
import com.haier.uhome.plugins.utils.JSONParser4Dart;
import com.haier.uhome.plugins.utils.JSONParser4Java;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.ide.plugins.newui.VerticalLayout;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.squareup.okhttp.*;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import net.sf.json.JSONObject;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import org.apache.http.client.utils.URIBuilder;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

public class HttpToolFactory implements ToolWindowFactory {
    private JComboBox methodBox;
    private JTextField url;
    private JButton send;
    private JTable queryTable;
    private JPanel rootContent;
    private JButton queryAddBtn;
    private JButton queryRemoveBtn;
    private JComboBox uaBox;
    private JComboBox contentTypeBox;
    private JTable headersTable;
    private JButton headersAddBtn;
    private JButton headersRemoveBtn;
    private JTextArea code;
    private JButton convertButton;
    private RTextScrollPane bodyPane;
    private RSyntaxTextArea bodyText;
    private RTextScrollPane requestPane;
    private RSyntaxTextArea requestText;
    private JPanel history;
    private JTabbedPane responseTab;
    private Project project;
    private String resultJson = null;
    private BoxStore store;
    private Box<RequestHistory> box;


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        store = MyObjectBox.builder().name("request-history-db").build();
        box = store.boxFor(RequestHistory.class);
        this.project = project;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(rootContent, "", false);
        toolWindow.getContentManager().addContent(content);
        convertButton.setVisible(false);
        initTables();
        refreshHistoryList();
        initActionListeners();
        bodyPane.setViewportView(bodyText);
        bodyText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        bodyPane.setLineNumbersEnabled(true);
        bodyPane.setFoldIndicatorEnabled(true);
        bodyText.setCloseCurlyBraces(true);
        bodyText.setCloseMarkupTags(true);
        bodyText.setCloseMarkupTags(true);
        requestPane.setViewportView(requestText);
        requestText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        requestPane.setLineNumbersEnabled(true);
        requestPane.setFoldIndicatorEnabled(true);
        requestText.setCloseCurlyBraces(true);
        requestText.setCloseMarkupTags(true);
        requestText.setCloseMarkupTags(true);
    }

    private void initActionListeners() {
        send.addActionListener(e -> submitRequest());
        queryAddBtn.addActionListener(e -> addTableRow(queryTable));
        queryRemoveBtn.addActionListener(e -> {
            removeTableRow(queryTable);
        });
        headersAddBtn.addActionListener(e -> addTableRow(headersTable));
        headersRemoveBtn.addActionListener(e -> {
            removeTableRow(headersTable);
        });
        Gutter gutter = bodyPane.getGutter();
        gutter.setBackground(Gray._47);
        gutter = requestPane.getGutter();
        gutter.setBackground(Gray._47);
        convertButton.addActionListener(e -> showConvertDialog());
        url.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                Document document = e.getDocument();
                try {
                    String s = document.getText(0, document.getLength());
                    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                    if (s.equals(cb.getData(DataFlavor.stringFlavor))) {
                        URL u = new URL(s);
                        String[] columnNames = new String[]{"Key", "Value"};
                        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
                        queryTable.setModel(tableModel);
                        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        convertButton.setVisible(false);
                        resultJson = null;
                        Map<String, String> q = splitQuery(u);
                        for (Map.Entry<String, String> el : q.entrySet()) {
                            ((DefaultTableModel) queryTable.getModel()).addRow(new String[]{el.getKey(), el.getValue()});
                        }
                        SwingUtilities.invokeLater(() -> {
                            try {
                                document.remove(0, document.getLength());
                                document.insertString(0, u.getProtocol() + "://" + u.getHost() + u.getPath(), null);
                            } catch (BadLocationException e1) {
                                e1.printStackTrace();
                            }
                        });
                    }
                } catch (BadLocationException | UnsupportedFlavorException | IOException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });
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
            generateDartClass(virtualFile, className, withJson);
        }
    }

    private void generateDartClass(VirtualFile virtualFile, String className, boolean withJson) {
        JSONParser4Dart parser = new JSONParser4Dart();
        String path = virtualFile.getPath();
        PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
        parser.reset(project, directory);
        parser.init(className);
        JSONObject dist = JSONObject.fromObject(resultJson);
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
        JSONObject dist = JSONObject.fromObject(resultJson);
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


    private Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        if (query != null && !query.equalsIgnoreCase("")) {
            String[] pairs = query.split("&");
            if (pairs != null && pairs.length > 0)
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
        }
        return query_pairs;
    }

    private void addTableRow(JTable table) {
        ((DefaultTableModel) table.getModel()).addRow(new String[]{null, null});
    }

    private void removeTableRow(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            row = table.getRowCount() - 1;
        }
        if (row > -1) {
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
    }

    private void submitRequest() {
        if (Utils.isEmptyString(url.getText())) {
            Utils.showErrorNotification(project, "url can not be null");
            return;
        }
        responseTab.setSelectedIndex(0);
        OkHttpClient client = new OkHttpClient();
        URIBuilder builder = new URIBuilder();
        String scheme = url.getText().split(":")[0].toLowerCase();
        builder.setScheme(scheme);
        String hp = url.getText().split("//")[1];
        String[] hostPath = hp.split("/", 1);
        builder.setHost(hostPath[0]);
        if (hostPath.length > 1) {
            builder.setPath(hostPath[1]);
        }
        for (int i = 0; i < queryTable.getRowCount(); i++) {
            try {
                builder.addParameter(queryTable.getValueAt(i, 0).toString(), queryTable.getValueAt(i, 1).toString());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        try {
            URI uri = builder.build();
            RequestBody body = null;
            if (methodBox.getSelectedItem().toString().equalsIgnoreCase("POST")) {
                body = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse(contentTypeBox.getSelectedItem().toString());
                    }

                    @Override
                    public long contentLength() {
                        return requestText.getText().getBytes().length;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        sink.write(ByteString.encodeUtf8(requestText.getText()));
                    }
                };
            }
            Request.Builder rb = new Request.Builder().url(uri.toString()).method(methodBox.getSelectedItem().toString(), body);
            if (contentTypeBox.getSelectedItem().toString() != null && !contentTypeBox.getSelectedItem().toString().equalsIgnoreCase("")) {
                rb.addHeader("Content-Type", contentTypeBox.getSelectedItem().toString());
            }
            if (uaBox.getSelectedItem().toString() != null && !uaBox.getSelectedItem().toString().equalsIgnoreCase("")) {
                rb.addHeader("User-Agent", uaBox.getSelectedItem().toString());
            }
            for (int i = 0; i < headersTable.getRowCount(); i++) {
                try {
                    rb.addHeader(headersTable.getValueAt(i, 0).toString(), headersTable.getValueAt(i, 1).toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Request request = rb.build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    System.out.println("Error in request");
                    System.out.println(e.getMessage());
                    Utils.showErrorNotification(project, "request error occurred!");
                }

                @Override
                public void onResponse(Response response) {
                    int responseCode = response.code();
                    Headers responseHeaders = response.headers();
                    code.setText(String.valueOf(responseCode));
                    if (response.body() != null) {
                        if (responseCode == HTTP_OK) {
                            convertButton.setVisible(true);
                            RequestHistory requestHistory = new RequestHistory();
                            requestHistory.method = methodBox.getSelectedItem().toString();
                            requestHistory.url = url.getText();
                            requestHistory.queryMaps = getMapValues(queryTable);
                            requestHistory.headers = getMapValues(headersTable);
                            requestHistory.bodyJson = requestText.getText();
                            requestHistory.date = new Date();
                            RequestHistory queryResult = box.query().equal(RequestHistory_.url, url.getText()).build().findFirst();
                            if (null != queryResult) {
                                requestHistory.id = queryResult.id;
                            }
                            box.put(requestHistory);
                            refreshHistoryList();
                        }
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                for (int i = 0; i < responseHeaders.size(); i++) {
                                    bodyText.setSyntaxEditingStyle(response.body().contentType().type() + "/" + response.body().contentType().subtype());
                                    ResponseBody responseBody = response.body();
                                    BufferedSource source = null;
                                    try {
                                        source = responseBody.source();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        source.request(Long.MAX_VALUE); // Buffer the entire body.
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    Buffer buffer = source.buffer();
                                    Charset charset = StandardCharsets.UTF_8;
                                    MediaType contentType = responseBody.contentType();
                                    if (contentType != null) {
                                        charset = contentType.charset(StandardCharsets.UTF_8);
                                    }
                                    String s = buffer.clone().readString(charset);
                                    try {
                                        bodyText.setText((s));
                                        if (response.body().contentType().subtype().equalsIgnoreCase("json")) {
                                            JsonParser parser = new JsonParser();
                                            JsonElement el = parser.parse(s);
                                            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
                                            s = gson.toJson(el);
                                            bodyText.setText((s));
                                            bodyText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                                            resultJson = s;
                                        } else if (response.body().contentType().subtype().equalsIgnoreCase("xml")
                                                || response.body().contentType().subtype().equalsIgnoreCase("rss+xml")
                                                || response.body().contentType().subtype().equalsIgnoreCase("smil")) {
                                            Source xmlInput = new StreamSource(new StringReader(s));
                                            StringWriter stringWriter = new StringWriter();
                                            StreamResult xmlOutput = new StreamResult(stringWriter);
                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                            transformerFactory.setAttribute("indent-number", 4);

                                            Transformer transformer = transformerFactory.newTransformer();
                                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                            transformer.transform(xmlInput, xmlOutput);
                                            String s2 = xmlOutput.getWriter().toString();
                                            bodyText.setText((s2));
                                            bodyText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                                            resultJson = null;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            Utils.showErrorNotification(project, "request error occurred!");
        }
    }

    private void refreshHistoryList() {
        List<RequestHistory> historyList = box.getAll();
        if (null != historyList && !historyList.isEmpty()) {
            Collections.reverse(historyList);
            history.removeAll();
            history.setLayout(new VerticalLayout(2));
            for (RequestHistory requestHistory : historyList) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA);
                JPanel item = buildItemPanel(requestHistory, simpleDateFormat);
                history.add(item);
            }
        }
    }

    private JPanel buildItemPanel(RequestHistory requestHistory, SimpleDateFormat simpleDateFormat) {
        JPanel item = new JPanel();
        item.setBorder(BorderFactory.createLineBorder(JBColor.GRAY, 1));
        item.setLayout(new HorizontalLayout(5));
        JBLabel date = new JBLabel(simpleDateFormat.format(requestHistory.date));
        JBLabel method = new JBLabel(requestHistory.method);
        if (requestHistory.method.equalsIgnoreCase("post")) {
            method.setForeground(JBColor.green);
        } else if (requestHistory.method.equalsIgnoreCase("get")) {
            method.setForeground(JBColor.yellow);
        } else if (requestHistory.method.equalsIgnoreCase("put")) {
            method.setForeground(JBColor.BLUE);
        } else if (requestHistory.method.equalsIgnoreCase("delete")) {
            method.setForeground(JBColor.RED);
        } else {
            method.setForeground(JBColor.GRAY);
        }
        JBLabel url = new JBLabel(requestHistory.url);
        item.add(date);
        item.add(method);
        item.add(url);
        item.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                importRequest(requestHistory);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        return item;
    }

    private void importRequest(RequestHistory requestHistory) {
        System.out.println("ZCY " + requestHistory.toString());
        initTables();
        bodyText.setText("");
        methodBox.setSelectedItem(requestHistory.method.toUpperCase(Locale.CHINA));
        url.setText(requestHistory.url);
        if (null != requestHistory.queryMaps) {
            for (Map.Entry<String, String> entry : requestHistory.queryMaps.entrySet()) {
                ((DefaultTableModel) queryTable.getModel()).addRow(new String[]{entry.getKey(), entry.getValue()});
            }
        }
        if (null != requestHistory.headers) {
            for (Map.Entry<String, String> entry : requestHistory.headers.entrySet()) {
                ((DefaultTableModel) headersTable.getModel()).addRow(new String[]{entry.getKey(), entry.getValue()});
            }
        }
        if (!Utils.isEmptyString(requestHistory.bodyJson)) {
            bodyText.setText(requestHistory.bodyJson);
        }
    }

    private HashMap<String, String> getMapValues(JTable input) {
        HashMap<String, String> hashMap = new HashMap<>();
        for (int i = 0; i < input.getRowCount(); i++) {
            try {
                hashMap.put(input.getValueAt(i, 0).toString(), input.getValueAt(i, 1).toString());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
        return hashMap;
    }

    private void initTables() {
        String[] columnNames = {"Key", "Value"};
        DefaultTableModel queryTableModel = new DefaultTableModel(columnNames, 0);
        queryTable.setModel(queryTableModel);
        queryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queryTable.setRowHeight(30);

        DefaultTableModel headerTableModel = new DefaultTableModel(columnNames, 0);
        headersTable.setModel(headerTableModel);
        headersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        headersTable.setRowHeight(30);
    }

}
