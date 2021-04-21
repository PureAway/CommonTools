package com.haier.uhome.plugins.ui;

import com.haier.uhome.plugins.model.RequestHistory;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.ui.Gray;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GenerateRequestForm extends JFrame {
    private JPanel content;
    private RTextScrollPane dartPane;
    private RSyntaxTextArea dartText;
    private RTextScrollPane javaPane;
    private RSyntaxTextArea javaText;
    private final int DEFAULT_WIDTH = 900;
    private final int DEFAULT_HEIGHT = 600;
    private final Dimension defaultSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    public GenerateRequestForm(RequestHistory requestHistory) {
        initView();
        setFullScreenCenter();
        generateDartCode(requestHistory);
        generateJavaCode(requestHistory);
    }


    private void generateJavaCode(RequestHistory requestHistory) {
        try {
            URL u = new URL(requestHistory.url);
            StringBuilder stringBuilder = new StringBuilder("    @")
                    .append(requestHistory.method)
                    .append("(\"")
                    .append(u.getPath())
                    .append("\")\n")
                    .append("    Observer<Response> ");
            String[] splitResult = requestHistory.url.split("/");
            String methodName = splitResult[splitResult.length - 1];
            stringBuilder.append(methodName)
                    .append("(\n            ")
                    .append("@NonNull @Url String url,\n");
            HashMap<String, String> queryMaps = requestHistory.queryMaps;
            if (null != queryMaps && !queryMaps.isEmpty()) {
                for (Map.Entry<String, String> entry : queryMaps.entrySet()) {
                    stringBuilder.append("            ")
                            .append("@NonNull @Query(\"")
                            .append(entry.getKey())
                            .append("\") String ")
                            .append(Utils.getFieldName(entry.getKey()))
                            .append(",\n");
                }
            }
            HashMap<String, String> headerMaps = requestHistory.headers;
            if (null != headerMaps && !headerMaps.isEmpty()) {
                for (Map.Entry<String, String> entry : headerMaps.entrySet()) {
                    stringBuilder.append("            ")
                            .append("@NonNull @Header(\"")
                            .append(entry.getKey())
                            .append("\") String ")
                            .append(Utils.getFieldName(entry.getKey()))
                            .append(",\n");
                }
            }
            String bodyJson = requestHistory.bodyJson;
            if (!Utils.isEmptyString(bodyJson)) {
                stringBuilder.append("            ")
                        .append("@NonNull @Body ")
                        .append("RequestBody requestBody);");
            } else {
                if ((null != queryMaps && !queryMaps.isEmpty()) || (null != headerMaps && !headerMaps.isEmpty())) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"));
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                }
                stringBuilder.append(");");
            }
            javaText.setText(stringBuilder.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void generateDartCode(RequestHistory requestHistory) {
        try {
            URL u = new URL(requestHistory.url);
            StringBuilder stringBuilder = new StringBuilder("  @Url('")
                    .append(u.getProtocol())
                    .append("://")
                    .append(u.getHost())
                    .append("')\n")
                    .append("  @")
                    .append(requestHistory.method)
                    .append("('")
                    .append(u.getPath())
                    .append("')\n")
                    .append("  Future<dynamic> ");
            String[] splitResult = requestHistory.url.split("/");
            String methodName = splitResult[splitResult.length - 1];
            stringBuilder.append(methodName)
                    .append("(\n")
                    .append("      @BaseUrl() String baseUrl,\n");
            HashMap<String, String> queryMaps = requestHistory.queryMaps;
            if (null != queryMaps && !queryMaps.isEmpty()) {
                for (Map.Entry<String, String> entry : queryMaps.entrySet()) {
                    stringBuilder.append("      ")
                            .append("@Query('")
                            .append(entry.getKey())
                            .append("') String ")
                            .append(Utils.getFieldName(entry.getKey()))
                            .append(",\n");
                }
            }
            HashMap<String, String> headerMaps = requestHistory.headers;
            if (null != headerMaps && !headerMaps.isEmpty()) {
                for (Map.Entry<String, String> entry : headerMaps.entrySet()) {
                    stringBuilder.append("      ")
                            .append("@Header('")
                            .append(entry.getKey())
                            .append("') String ")
                            .append(Utils.getFieldName(entry.getKey()))
                            .append(",\n");
                }
            }
            String bodyJson = requestHistory.bodyJson;
            if (!Utils.isEmptyString(bodyJson)) {
                stringBuilder.append("      ")
                        .append("@Body() ")
                        .append("Map<String, dynamic> requestMap);");
            } else {
                if ((null != queryMaps && !queryMaps.isEmpty()) || (null != headerMaps && !headerMaps.isEmpty())) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"));
                    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
                }
                stringBuilder.append(");");
            }
            dartText.setText(stringBuilder.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void initView() {
        add(content);
        setTitle("GenerateRequest");
        setSize(defaultSize);
        setMinimumSize(defaultSize);
        dartPane.setViewportView(dartText);
        dartText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DART);
        dartPane.setLineNumbersEnabled(true);
        dartPane.setFoldIndicatorEnabled(true);
        dartText.setCloseCurlyBraces(true);
        dartText.setCloseMarkupTags(true);
        dartText.setCloseMarkupTags(true);
        Gutter gutter = dartPane.getGutter();
        gutter.setBackground(Gray._47);
        javaPane.setViewportView(javaText);
        javaText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        javaPane.setLineNumbersEnabled(true);
        javaPane.setFoldIndicatorEnabled(true);
        javaText.setCloseCurlyBraces(true);
        javaText.setCloseMarkupTags(true);
        javaText.setCloseMarkupTags(true);
        gutter = javaPane.getGutter();
        gutter.setBackground(Gray._47);
        javaText.setText("Coming soon");
    }

    private void setFullScreenCenter() {
        double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        setLocation((int) (width - this.getWidth()) / 2, (int) (height - this.getHeight()) / 2);
    }
}
