package com.haier.uhome.plugins.ui;

import com.haier.uhome.plugins.checker.ProjectChecker;
import com.haier.uhome.plugins.model.Command;
import com.haier.uhome.plugins.model.KillCommand;
import com.haier.uhome.plugins.sdk.FlutterSdk;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.rmi.CORBA.Util;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CommandForm extends JFrame {

    private static final Logger log = Logger.getInstance(CommandForm.class);

    private JPanel content;
    private final int DEFAULT_WIDTH = 900;
    private final int DEFAULT_HEIGHT = 500;
    private final Dimension defaultSize = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private final ArrayList<Command> flutterDataList = new ArrayList<>();
    private final ArrayList<Command> postGetDataList = new ArrayList<>();
    private final ArrayList<Command> gradlewDataList = new ArrayList<>();
    private final ArrayList<Command> adbDataList = new ArrayList<>();
    private final Project project;
    private String path;
    private boolean isFlutter;
    private boolean isGradle;
    private final boolean isSYN;
    private String flutterSdkPath;
    private String postGetPath;
    private KillCommand flutterKillCommand, gradleKillCommand;

    public CommandForm(Project currentProject, String virtualDir) {
        project = currentProject;
        path = virtualDir;
        FlutterSdk flutterSdk = FlutterSdk.getFlutterSdk(project);
        ProjectChecker checker = new ProjectChecker();
        isFlutter = checker.checkFlutter(path);
        String flutterPath = project.getBasePath();
        if (!isFlutter) {
            flutterPath = Utils.findFlutterPath(project, path, checker);
            isFlutter = checker.checkFlutter(flutterPath);
        }
        if (isFlutter) {
            path = flutterPath;
            flutterKillCommand = new KillCommand("Flutter",
                    "flutter", "commandline name='flutter'");
        }
        if (!isFlutter) {
            String gradlewPath = project.getBasePath();
            isGradle = checker.checkGradle(gradlewPath);
        }
        if (isGradle) {
            path = Utils.findGradlePath(project, path, checker);
            gradleKillCommand = new KillCommand("Gradle", "gradle-launcher",
                    "commandline like '%gradle-launcher%' and name like '%java%'");
        }
        isSYN = checker.checkPostGet(project.getBasePath());
        if (isSYN) {
            postGetPath = project.getBasePath();
        }
        if (null != flutterSdk) {
            flutterSdkPath = flutterSdk.getHomePath() + (Utils.isWindowsOS() ? "/bin/flutter.bat" : "/bin/flutter");
            log.info("find flutter home : " + flutterSdkPath);
        }
        initData();
        initView();
        setFullScreenCenter();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showData();
            }
        }, 180);
    }


    private void initData() {
        if (isFlutter) {
            flutterDataList.add(new Command("flutter clean", "clean", "flutter clean"));
            flutterDataList.add(new Command("flutter pub get", "pub get", "flutter pub get"));
            flutterDataList.add(new Command("delete lock file", "delete lock file", "delete lock file"));
            flutterDataList.add(new Command("build_runner clean", "pub run build_runner clean",
                    "flutter pub run build_runner clean"));
            flutterDataList.add(new Command("build_runner build",
                    "pub run build_runner build", "flutter pub run build_runner build"));
            flutterDataList.add(new Command("build_runner rebuild",
                    "pub run build_runner build --delete-conflicting-outputs", "flutter pub run build_runner build --delete-conflicting-outputs"));
        }

        if (Utils.isWindowsOS()) {
            if (isSYN) {
                postGetDataList.add(new Command("postget.sh", "postget.sh", "postget.sh").setNeedSpace(false));
            }
        } else {
            if (isSYN) {
                postGetDataList.add(new Command("postget.sh", "./postget.sh", "./postget.sh").setNeedSpace(false));
            }
        }
        if (isGradle) {
            gradlewDataList.add(new Command("gradlew clean", "clean",
                    "gradlew clean").setNeedSpace(false));
            gradlewDataList.add(new Command("gradlew build",
                    "build", "gradlew build").setNeedSpace(false));
            gradlewDataList.add(new Command("gradlew assembleRelease",
                    "assembleRelease", "gradlew assembleRelease").setNeedSpace(false));
        }
        adbDataList.add(new Command("adb logcat", "adb logcat -v time > log.txt", "adb logcat -v time > log.txt"));
        adbDataList.add(new Command("adb devices", "adb devices", "adb devices"));
        adbDataList.add(new Command("adb kill-server", "adb kill-server", "adb kill-server"));
        adbDataList.add(new Command("adb start-server", "adb start-server", "adb start-server"));
    }

    private void initView() {
        add(content);
        setTitle("Common Commands");
        setSize(defaultSize);
        setMinimumSize(defaultSize);
    }

    private void setFullScreenCenter() {
        double width = Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        double height = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        setLocation((int) (width - this.getWidth()) / 2, (int) (height - this.getHeight()) / 2);
    }

    private void showData() {
        JPanel jbList = new JPanel();
        jbList.setLayout(new VerticalLayout(4));
        jbList.setSize(content.getSize());
        if (isFlutter) {
            jbList.add(getTitleWithKillFlutterCommand());
            addSpace(jbList);
            for (Command command : flutterDataList) {
                JPanel item = buildItemPanel(command, flutterSdkPath);
                jbList.add(item);
                addSpace(jbList);
            }
        }
        if (isSYN) {
            jbList.add(getCommandTitle("SYN"));
            addSpace(jbList);
            for (Command command : postGetDataList) {
                JPanel item = buildItemPanel(command, postGetPath);
                jbList.add(item);
                addSpace(jbList);
            }
        }
        if (isGradle) {
            jbList.add(getTitleWithKillGradleCommand());
            addSpace(jbList);
            for (Command command : gradlewDataList) {
                JPanel item = buildItemPanel(command, "");
                jbList.add(item);
                addSpace(jbList);
            }
        }
        jbList.add(getCommandTitle("ADB"));
        addSpace(jbList);
        for (Command command : adbDataList) {
            JPanel item = buildItemPanel(command, "");
            jbList.add(item);
            addSpace(jbList);
        }
        jbList.setBackground(JBColor.WHITE);
        JBScrollPane scrollPane = new JBScrollPane(jbList);
        Dimension preferredSize = new Dimension(content.getWidth(), content.getHeight());
        scrollPane.setPreferredSize(preferredSize);
        content.add(scrollPane);
        content.revalidate();
    }

    private JPanel getTitleWithKillGradleCommand() {
        JPanel item = new JPanel();
        item.setLayout(new HorizontalLayout(-64));
        JBLabel jbLabel = new JBLabel("Gradlew");
        jbLabel.setFont(new Font(null, Font.BOLD, 40));
        jbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jbLabel.setPreferredSize(new Dimension(content.getWidth(), 60));
        item.add(jbLabel);
        ImageIcon killIcon = new ImageIcon(getClass().getResource("/icons/gradle.png"));
        JButton killButton = new JButton();
        killButton.setSize(48, 30);
        killButton.setPreferredSize(new Dimension(48, 30));
        killButton.setIcon(killIcon);
        killButton.setToolTipText("点击杀死gradle进程");
        killButton.addActionListener(e -> {
            Utils.killProcess(gradleKillCommand);
        });
        item.add(killButton);
        return item;
    }

    private JPanel getTitleWithKillFlutterCommand() {
        JPanel item = new JPanel();
        item.setLayout(new HorizontalLayout(-60));
        JBLabel jbLabel = new JBLabel("Flutter");
        jbLabel.setFont(new Font(null, Font.BOLD, 40));
        jbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jbLabel.setPreferredSize(new Dimension(content.getWidth(), 60));
        item.add(jbLabel);
        ImageIcon killIcon = new ImageIcon(getClass().getResource("/icons/flutter.png"));
        JButton killButton = new JButton();
        killButton.setSize(48, 30);
        killButton.setPreferredSize(new Dimension(48, 30));
        killButton.setIcon(killIcon);
        killButton.setToolTipText("点击杀死flutter进程");
        killButton.addActionListener(e -> {
            Utils.killProcess(flutterKillCommand);
        });
        item.add(killButton);
        return item;
    }

    private void addSpace(JPanel jbList) {
        JPanel space = new JPanel();
        space.setPreferredSize(new Dimension(content.getWidth() - 16, 1));
        space.setBackground(JBColor.border());
        jbList.add(space);
    }

    private JBLabel getCommandTitle(String title) {
        JBLabel jbLabel = new JBLabel(title);
        jbLabel.setFont(new Font(null, Font.BOLD, 40));
        jbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jbLabel.setPreferredSize(new Dimension((content.getWidth() - 40), 60));
        return jbLabel;
    }


    @NotNull
    private JPanel buildItemPanel(Command command, String sdkPath) {
        JPanel item = new JPanel();
        item.setLayout(new HorizontalLayout(-120));
        JBLabel name = new JBLabel(command.getName());
        name.setFont(new Font(null, Font.BOLD, 18));
        name.setHorizontalAlignment(SwingConstants.CENTER);
        name.setPreferredSize(new Dimension(content.getWidth(), 32));
        item.add(name);
        JPanel function = new JPanel();
        function.setLayout(new HorizontalLayout(10));
        function.setPreferredSize(new Dimension(120, 32));
        item.add(function);
        ImageIcon copyIcon = new ImageIcon(getClass().getResource("/icons/copy.png"));
        JButton copyButton = new JButton();
        copyButton.setPreferredSize(new Dimension(48, 30));
        copyButton.setIcon(copyIcon);
        copyButton.setToolTipText("点击将会copy命令：" + command.getCommand());
        copyButton.addActionListener(e -> {
            Clipboard systemClipboard = getToolkit().getSystemClipboard();
            Transferable content = new StringSelection(command.getCmd());
            systemClipboard.setContents(content, null);
            Utils.showInfoNotification(project, "命令" + command.getCmd() + "已经拷贝至剪贴板");
            CommandForm.this.setVisible(false);
        });
        function.add(copyButton);
        if (command.getCommand().startsWith("delete")) {
            copyButton.setVisible(false);
        }

        ImageIcon execIcon = new ImageIcon(getClass().getResource("/icons/run.png"));
        JButton execButton = new JButton();
        execButton.setPreferredSize(new Dimension(48, 30));
        execButton.setIcon(execIcon);
        execButton.setToolTipText("点击将会执行命令：" + command.getCommand());
        execButton.addActionListener(e -> {
            Utils.execCommand(project, sdkPath, path, command.isNeedSpace(), command);
            CommandForm.this.setVisible(false);
        });
        function.add(execButton);
        if (command.getCommand().startsWith("adb")) {
            execButton.setVisible(false);
        }
        item.setPreferredSize(new Dimension((content.getWidth() - 40), 40));
        return item;
    }

}
