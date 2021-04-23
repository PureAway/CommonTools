package com.haier.uhome.plugins.ui;

import com.haier.uhome.plugins.model.Command;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;

public class GitForm extends JFrame {
    private JPanel content;
    private JTextArea nextVersion;
    private JTextArea mStr;
    private JButton commit;
    private JPanel rightContainer;
    private JLabel lastTag;
    private JTextArea tagText;
    private JButton pull;
    private final int DEFAULT_WIDTH = 900;
    private final int DEFAULT_HEIGHT = 600;
    private final Dimension defaultSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private String lastVersion;

    public GitForm(Project project) {
        initView(project);
        setFullScreenCenter();
        showTags(project);
    }

    private void showTags(Project project) {
        if (Utils.isEmptyString(Utils.gitPath)) {
            Utils.showErrorMessage("can not find git environment in your home path");
            return;
        }
        try {
            String fillCmd = "git tag";
            Process process = Runtime.getRuntime().exec(fillCmd, null, new File(project.getBasePath()));
            BufferedInputStream bufferedErrorStream = new BufferedInputStream(process.getErrorStream());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            BufferedReader bufferedErrorReader = new BufferedReader((new InputStreamReader(bufferedErrorStream, "utf-8")));
            BufferedReader bufferedInputReader = new BufferedReader((new InputStreamReader(bufferedInputStream, "utf-8")));
            String lineStr;

            ArrayList<String> tags = new ArrayList<>();
            while (!Utils.isEmptyString(lineStr = bufferedInputReader.readLine())) {
                tags.add(lineStr);

            }
            Comparator<String> comparator = (s1, s2) -> {
                try {
                    String[] s1Split = s1.split("\\.", -1);
                    String[] s2Split = s2.split("\\.", -1);
                    int len1 = s1Split.length;
                    int len2 = s2Split.length;
                    int lim = Math.min(len1, len2);
                    int i = 0;
                    while (i < lim) {
                        int c1 = "".equals(s1Split[i]) ? 0 : Integer.parseInt(s1Split[i]);
                        int c2 = "".equals(s2Split[i]) ? 0 : Integer.parseInt(s2Split[i]);
                        if (c1 != c2) {
                            return c1 - c2;
                        }
                        i++;
                    }
                    return len1 - len2;
                } catch (Throwable throwable) {
                    return 0;
                }
            };
            tags.sort(comparator);
            StringBuilder stringBuilder = new StringBuilder();
            for (String str : tags) {
                stringBuilder.append(str)
                        .append("\n");
            }
            int code = process.waitFor();
            if (code == 0) {
                stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"));
                tagText.setText(stringBuilder.toString());
                int index = stringBuilder.lastIndexOf("\n");
                lastVersion = stringBuilder.substring(index);
                lastTag.setText(lastVersion);
                nextVersion.setText(getNextVersion());
            }
            bufferedErrorStream.close();
            bufferedInputStream.close();
            bufferedErrorReader.close();
            bufferedInputReader.close();
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private String getNextVersion() {
        String[] result = lastVersion.split("\\.");
        if (null != result && result.length > 1) {
            String last = result[result.length - 1];
            if (Utils.isInteger(last)) {
                int lastVersion = Integer.parseInt(last);
                int nextVersion = lastVersion + 1;
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < result.length - 1; i++) {
                    stringBuilder.append(result[i])
                            .append(".");
                }
                stringBuilder.append(nextVersion);
                return stringBuilder.toString().replace("\n", "");
            }
        }
        return lastVersion + "_next";
    }

    private void initView(Project project) {
        add(content);
        setTitle("Git Tag");
        setSize(defaultSize);
        setMinimumSize(defaultSize);
        rightContainer.setLayout(new VerticalFlowLayout(4));
        Dimension dimension = new Dimension(-1, 36);
        lastTag.setPreferredSize(dimension);
        nextVersion.setPreferredSize(dimension);
        mStr.setPreferredSize(dimension);
        commit.setPreferredSize(new Dimension(-1, 40));
        pull.addActionListener(e -> {
            Command command = new Command("git pull", "git pull --rebase", "git pull --rebase");
            Utils.execCommand(project, "", project.getBasePath(), false, command, new Utils.ExecCallback() {
                @Override
                public void onSuccess() {
                    showTags(project);
                }

                @Override
                public void onFailed() {

                }
            });
        });
        commit.addActionListener(e -> {
            StringBuilder command = new StringBuilder("git tag ");
            if (Utils.isEmptyString(mStr.getText())) {
                command.append(nextVersion.getText());
            } else {
                command.append("-a ")
                        .append(nextVersion.getText())
                        .append(" -m ")
                        .append(mStr.getText());
            }
            Utils.gitTag(project, command.toString(), new Utils.ExecCallback() {
                @Override
                public void onSuccess() {
                    String com = "git push origin " + nextVersion.getText();
                    Utils.pushTag(project, com, new Utils.ExecCallback() {
                        @Override
                        public void onSuccess() {
                            showTags(project);
                        }

                        @Override
                        public void onFailed() {

                        }
                    });
                }

                @Override
                public void onFailed() {


                }
            });

        });
    }

    private void setFullScreenCenter() {
        double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        setLocation((int) (width - this.getWidth()) / 2, (int) (height - this.getHeight()) / 2);
    }
}
