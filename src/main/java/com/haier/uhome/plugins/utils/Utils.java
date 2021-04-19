package com.haier.uhome.plugins.utils;

import com.haier.uhome.plugins.checker.ProjectChecker;
import com.haier.uhome.plugins.model.Command;
import com.haier.uhome.plugins.model.KillCommand;
import com.haier.uhome.plugins.sdk.FlutterSdk;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class Utils {

    private static final Logger logger = Logger.getInstance(Utils.class);

    /**
     * Is windows OS
     *
     * @return true-windowsOS
     */
    public static boolean isWindowsOS() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * show info message
     *
     * @param message msg
     */
    public static void showInfo(String message) {
        Messages.showMessageDialog(message, "Common Tools", Messages.getInformationIcon());
    }

    /**
     * show error message
     *
     * @param message msg
     */
    public static void showErrorMessage(String message) {
        Messages.showMessageDialog(message, "Common Tools", Messages.getErrorIcon());
    }


    /**
     * Display simple notification - information
     *
     * @param project
     * @param text
     */
    public static void showInfoNotification(Project project, String text) {
        showNotification(project, MessageType.INFO, text);
    }

    /**
     * Display simple notification - error
     *
     * @param project
     * @param text
     */
    public static void showErrorNotification(Project project, String text) {
        showNotification(project, MessageType.ERROR, text);
    }

    /**
     * Display simple notification of given type
     *
     * @param project
     * @param type
     * @param text
     */
    public static void showNotification(Project project, MessageType type, String text) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, type, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }


    /**
     * Easier way to check if string is empty
     *
     * @param text
     * @return
     */
    public static boolean isEmptyString(String text) {
        return (text == null || text.trim().length() == 0);
    }

    private static boolean isBuildRunnerSuccess = false;

    public static void postGet(@NotNull Project project, @NotNull final Command command) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands");
        if (toolWindow != null) {
            toolWindow.show();
            JScrollPane jScrollPane = (JScrollPane) toolWindow.getContentManager().getContent(0).getComponent().getComponent(0);
            JScrollBar verticalBar = jScrollPane.getVerticalScrollBar();
            JTextArea jTextArea = (JTextArea) jScrollPane.getViewport().getComponent(0);
            String commandName = command.getName();
            asyncTask(project, commandName, new ActionListener() {
                @Override
                public void onRunning(ProgressIndicator progressIndicator) {
                    String fillCmd = " ./postget.sh";
                    log(jTextArea, verticalBar, project.getBasePath() + ": " + fillCmd);
                    try {
                        Process process = Runtime.getRuntime().exec(fillCmd, null, new File(project.getBasePath()));
                        BufferedInputStream bufferedErrorStream = new BufferedInputStream(process.getErrorStream());
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
                        BufferedReader bufferedErrorReader = new BufferedReader((new InputStreamReader(bufferedErrorStream, "utf-8")));
                        BufferedReader bufferedInputReader = new BufferedReader((new InputStreamReader(bufferedInputStream, "utf-8")));
                        String lineStr;
                        while (!Utils.isEmptyString(lineStr = bufferedInputReader.readLine())) {
                            log(jTextArea, verticalBar, lineStr);
                        }
                        while (!Utils.isEmptyString(lineStr = bufferedErrorReader.readLine())) {
                            log(jTextArea, verticalBar, lineStr);
                        }
                        int code = process.waitFor();
                        if (code == 0) {
                            log(jTextArea, verticalBar, "postget Success! Exit with code: " + code);
                            isBuildRunnerSuccess = true;
                        } else {
                            isBuildRunnerSuccess = false;
                            log(jTextArea, verticalBar, "postget Error! Exit with code: " + code);
                        }
                        bufferedErrorStream.close();
                        bufferedInputStream.close();
                        bufferedErrorReader.close();
                        bufferedInputReader.close();
                    } catch (Throwable e) {
                        isBuildRunnerSuccess = false;
                        e.printStackTrace();
                        System.out.println(command.getErrorMessage() + ", message:" + e.getLocalizedMessage());
                        showErrorMessage(command.getErrorMessage() + ", message:" + e.getLocalizedMessage());
                    }
                }

                @Override
                public void onSuccess() {
                    if (isBuildRunnerSuccess) {
                        showInfo(command.getSuccessMessage());
                    } else {
                        showErrorMessage("An exception error occurred during command execution. " +
                                "Please manually execute and resolve the error before using this plugin.");
                    }
                }

                @Override
                public void onFailed(Throwable error) {
                    showErrorMessage(command.getErrorMessage() + ", message:" + error.getLocalizedMessage());
                }

                @Override
                public void onFinished() {
                    isBuildRunnerSuccess = false;
                }

                @Override
                public void onCancel() {
                    showInfo("Action canceled!");
                }
            });
        }
    }

    public static void flutterPubGet(@NotNull Project project, @NotNull String sdkPath,
                                     @NotNull String dirPath, @NotNull final Command command,
                                     FlutterSdk flutterSdk, boolean isSYN) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands");
        if (toolWindow != null) {
            toolWindow.show();
            JScrollPane jScrollPane = (JScrollPane) toolWindow.getContentManager().getContent(0).getComponent().getComponent(0);
            JScrollBar verticalBar = jScrollPane.getVerticalScrollBar();
            JTextArea jTextArea = (JTextArea) jScrollPane.getViewport().getComponent(0);
            String commandName = command.getName();
            asyncTask(project, commandName, new ActionListener() {
                @Override
                public void onRunning(ProgressIndicator progressIndicator) {
                    String fillCmd = sdkPath + " pub get";
                    log(jTextArea, verticalBar, dirPath + ": " + fillCmd);
                    ColoredProcessHandler handler = flutterSdk.flutterPackagesGet(dirPath).startNoUIConsole(project);
                    try {
                        Process process = handler.getProcess();
                        BufferedInputStream bufferedErrorStream = new BufferedInputStream(process.getErrorStream());
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
                        BufferedReader bufferedErrorReader = new BufferedReader((new InputStreamReader(bufferedErrorStream, "utf-8")));
                        BufferedReader bufferedInputReader = new BufferedReader((new InputStreamReader(bufferedInputStream, "utf-8")));
                        String lineStr;
                        while (!Utils.isEmptyString(lineStr = bufferedInputReader.readLine())) {
                            log(jTextArea, verticalBar, lineStr);
                        }
                        while (!Utils.isEmptyString(lineStr = bufferedErrorReader.readLine())) {
                            log(jTextArea, verticalBar, lineStr);
                        }
                        int code = process.waitFor();
                        if (code == 0) {
                            log(jTextArea, verticalBar, commandName + " Success! Exit with code: " + code);
                            isBuildRunnerSuccess = true;
                        } else {
                            isBuildRunnerSuccess = false;
                            log(jTextArea, verticalBar, commandName + " Error! Exit with code: " + code);
                        }
                        bufferedErrorStream.close();
                        bufferedInputStream.close();
                        bufferedErrorReader.close();
                        bufferedInputReader.close();
                    } catch (Throwable e) {
                        isBuildRunnerSuccess = false;
                        e.printStackTrace();
                        System.out.println(command.getErrorMessage() + ", message:" + e.getLocalizedMessage());
                        showErrorMessage(command.getErrorMessage() + ", message:" + e.getLocalizedMessage());
                    }
                }

                @Override
                public void onSuccess() {
                    if (isBuildRunnerSuccess) {
                        if (isSYN) {
                            if (!isWindowsOS()) {
                                postGet(project, command);
                            } else {
                                showInfo(command.getSuccessMessage());
                            }
                        } else {
                            showInfo(command.getSuccessMessage());
                        }
                    } else {
                        showErrorMessage("An exception error occurred during command execution. " +
                                "Please manually execute and resolve the error before using this plugin.");
                    }
                }

                @Override
                public void onFailed(Throwable error) {
                    showErrorMessage(command.getErrorMessage() + ", message:" + error.getLocalizedMessage());
                }

                @Override
                public void onFinished() {

                }

                @Override
                public void onCancel() {
                    showInfo("Action canceled!");
                }
            });
        }
    }

    public static void oneKeyExec(@NotNull Project project, @NotNull String sdkPath,
                                  @NotNull String dirPath, @NotNull final Command command, FlutterSdk flutterSdk, boolean isSYN) {
        if (processDeleteCommand(command, dirPath)) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands");
            if (toolWindow != null) {
                toolWindow.show();
                JScrollPane jScrollPane = (JScrollPane) toolWindow.getContentManager().getContent(0).getComponent().getComponent(0);
                JScrollBar verticalBar = jScrollPane.getVerticalScrollBar();
                JTextArea jTextArea = (JTextArea) jScrollPane.getViewport().getComponent(0);
                String commandName = command.getName();
                asyncTask(project, commandName, new ActionListener() {
                    @Override
                    public void onRunning(ProgressIndicator progressIndicator) {
                        String fillCmd = sdkPath + " clean";
                        log(jTextArea, verticalBar, dirPath + ": " + fillCmd);
                        try {
                            Process process = Runtime.getRuntime().exec(fillCmd, null, new File(dirPath));
                            BufferedInputStream bufferedErrorStream = new BufferedInputStream(process.getErrorStream());
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
                            BufferedReader bufferedErrorReader = new BufferedReader((new InputStreamReader(bufferedErrorStream, "utf-8")));
                            BufferedReader bufferedInputReader = new BufferedReader((new InputStreamReader(bufferedInputStream, "utf-8")));
                            String lineStr;
                            while (!Utils.isEmptyString(lineStr = bufferedInputReader.readLine())) {
                                log(jTextArea, verticalBar, lineStr);
                            }
                            while (!Utils.isEmptyString(lineStr = bufferedErrorReader.readLine())) {
                                log(jTextArea, verticalBar, lineStr);
                            }
                            int code = process.waitFor();
                            if (code == 0) {
                                isBuildRunnerSuccess = true;
                                log(jTextArea, verticalBar, "flutter clean Success! Exit with code: " + code);
                            } else {
                                isBuildRunnerSuccess = false;
                                log(jTextArea, verticalBar, "flutter clean Error! Exit with code: " + code);
                            }
                            bufferedErrorStream.close();
                            bufferedInputStream.close();
                            bufferedErrorReader.close();
                            bufferedInputReader.close();
                        } catch (Throwable e) {
                            isBuildRunnerSuccess = false;
                            e.printStackTrace();
                            showErrorMessage(command.getErrorMessage() + ", message:" + e.getLocalizedMessage());
                        }
                    }

                    @Override
                    public void onSuccess() {
                        if (isBuildRunnerSuccess) {
                            flutterPubGet(project, sdkPath, dirPath, command, flutterSdk, isSYN);
                        } else {
                            showErrorMessage("An exception error occurred during command execution. " +
                                    "Please manually execute and resolve the error before using this plugin.");
                        }
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        showErrorMessage(command.getErrorMessage() + ", message:" + error.getLocalizedMessage());
                    }

                    @Override
                    public void onFinished() {

                    }

                    @Override
                    public void onCancel() {
                        showInfo("Action canceled!");
                    }
                });
            }
        } else {
            showErrorMessage("delete lock file failed");
        }
    }

    public static void execCommand(@NotNull Project project, @NotNull String sdkPath,
                                   @NotNull String dirPath, boolean space, @NotNull final Command command) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("CommonCommands");
        if (toolWindow != null) {
            toolWindow.show();
            JScrollPane jScrollPane = (JScrollPane) toolWindow.getContentManager().getContent(0).getComponent().getComponent(0);
            JScrollBar verticalBar = jScrollPane.getVerticalScrollBar();
            JTextArea jTextArea = (JTextArea) jScrollPane.getViewport().getComponent(0);
            String commandName = command.getName();
            asyncTask(project, commandName, new ActionListener() {
                @Override
                public void onRunning(ProgressIndicator progressIndicator) {
                    if (processDeleteCommand(command, dirPath)) {
                        return;
                    }
                    String fillCmd;
                    if (command.getName().startsWith("gradle")) {
                        fillCmd = parseGradleCommand(project, command, dirPath);
                    } else {
                        fillCmd = sdkPath + (space ? " " : "/") + command.getCommand();
                    }
                    log(jTextArea, verticalBar, dirPath + ": " + fillCmd);
                    try {
                        Process process = Runtime.getRuntime().exec(fillCmd, null, new File(dirPath));
                        BufferedInputStream bufferedErrorStream = new BufferedInputStream(process.getErrorStream());
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
                        BufferedReader bufferedErrorReader = new BufferedReader((new InputStreamReader(bufferedErrorStream, "utf-8")));
                        BufferedReader bufferedInputReader = new BufferedReader((new InputStreamReader(bufferedInputStream, "utf-8")));
                        String lineStr;
                        while (!Utils.isEmptyString(lineStr = bufferedInputReader.readLine())) {
                            log(jTextArea, verticalBar, lineStr);
                        }
                        while (!Utils.isEmptyString(lineStr = bufferedErrorReader.readLine())) {
                            log(jTextArea, verticalBar, lineStr);
                        }
                        int code = process.waitFor();
                        if (code == 0) {
                            isBuildRunnerSuccess = true;
                            log(jTextArea, verticalBar, commandName + " Success! Exit with code: " + code);
                        } else {
                            isBuildRunnerSuccess = false;
                            log(jTextArea, verticalBar, commandName + " Error! Exit with code: " + code);
                        }
                        bufferedErrorStream.close();
                        bufferedInputStream.close();
                        bufferedErrorReader.close();
                        bufferedInputReader.close();
                    } catch (Throwable e) {
                        isBuildRunnerSuccess = false;
                        e.printStackTrace();
                        showErrorMessage(command.getErrorMessage() + ", message:" + e.getLocalizedMessage());
                    }
                }

                @Override
                public void onSuccess() {
                    if (isBuildRunnerSuccess) {
                        showInfo(command.getSuccessMessage());
                    } else {
                        showErrorMessage("An exception error occurred during command execution. Please manually execute and resolve the error before using this plugin.");
                    }
                }

                @Override
                public void onFailed(Throwable error) {
                    showErrorMessage(command.getErrorMessage() + ", message:" + error.getLocalizedMessage());
                }

                @Override
                public void onFinished() {
                    isBuildRunnerSuccess = false;
                }

                @Override
                public void onCancel() {
                    showInfo("Action canceled!");
                }
            });
        }
    }

    private static String getCurrentTime() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return sdf.format(date) + " ";
    }


    /**
     * Checks whether a given filename is an Xcode metadata file, suitable for opening externally.
     *
     * @param name the name to check
     * @return true if an xcode project filename
     */
    public static boolean isXcodeFileName(@NotNull String name) {
        return isXcodeProjectFileName(name) || isXcodeWorkspaceFileName(name);
    }

    /**
     * Checks whether a given name is an Xcode workspace filename.
     *
     * @param name the name to check
     * @return true if an xcode workspace filename
     */
    public static boolean isXcodeWorkspaceFileName(@NotNull String name) {
        return name.endsWith(".xcworkspace");
    }

    /**
     * Checks whether a given file name is an Xcode project filename.
     *
     * @param name the name to check
     * @return true if an xcode project filename
     */
    public static boolean isXcodeProjectFileName(@NotNull String name) {
        return name.endsWith(".xcodeproj");
    }


    public static String getClassName(String name) {
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (!Character.isLetter(c)) {
                chars[i] = '_';
                if (i + 1 < chars.length) {
                    chars[i + 1] = Character.toUpperCase(chars[i + 1]);
                }
            }
        }
        return String.valueOf(chars).replaceAll("_", "");
    }

    private static String parseGradleCommand(Project project, Command command, String dirPath) {
        String rootPath = project.getBasePath();
        String[] rootPaths = rootPath.split("/");
        String[] dirPaths = dirPath.split("/");
        int length = dirPaths.length - rootPaths.length;
        if (length > 0) {
            StringBuilder prefix = new StringBuilder("../");
            for (int i = 0; i < length - 1; i++) {
                prefix.append("../");
            }
            if (isWindowsOS()) {
                return prefix + "gradlew " + command.getCommand();
            } else {
                return prefix + "./gradlew " + command.getCommand();
            }
        } else {
            if (isWindowsOS()) {
                return "gradlew " + command.getCommand();
            } else {
                return "./gradlew " + command.getCommand();
            }
        }
    }

    private static boolean processDeleteCommand(@NotNull Command command, @NotNull String dirPath) {
        if (command.getName().startsWith("delete") || command.getName().startsWith("one")) {
            isBuildRunnerSuccess = deleteLockFile(dirPath);
            return true;
        }
        return false;
    }


    private static void log(JTextArea jTextArea, JScrollBar verticalBar, String message) {
        if (!isEmptyString(message) && jTextArea != null && verticalBar != null) {
            message = getCurrentTime() + message;
            System.out.println(message);
            jTextArea.append("\n" + message);
            verticalBar.setValue(verticalBar.getMaximum());
        } else {
            logger.error("log error");
        }
    }

    private static void asyncTask(@NotNull Project project, String title, @NotNull ActionListener listener) {
        new Task.Backgroundable(project, title, true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                listener.onRunning(progressIndicator);
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                listener.onSuccess();
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                super.onThrowable(error);
                listener.onFailed(error);
            }

            @Override
            public void onFinished() {
                super.onFinished();
                listener.onFinished();
            }

            @Override
            public void onCancel() {
                super.onCancel();
                listener.onCancel();
            }
        }.queue();
    }

    private static boolean deleteLockFile(String path) {
        File parentFile = new File(path);
        if (parentFile.exists()) {
            for (File file : parentFile.listFiles()) {
                if (file.getName().equals("pubspec.lock")) {
                    return file.delete();
                }
            }
        }
        return false;
    }

    /**
     * 向上递归查找flutter工程的根目录，如果没有找到就返回当前项目的根目录
     *
     * @param project 当前项目
     * @param path    当前选中的路径
     * @param checker 路径判断器
     * @return flutter工程的根目录
     */
    public static String findFlutterPath(Project project, String path, ProjectChecker checker) {
        if (checker.checkFlutter(path)) {
            return path;
        }
        File file = new File(path);
        if (!file.exists()) {
            return project.getBasePath();
        } else {
            if (path.equals(project.getBasePath())) {
                return path;
            }
            path = file.getParent();
            if (path.equals(project.getBasePath())) {
                return project.getBasePath();
            } else {
                return findFlutterPath(project, path, checker);
            }
        }
    }

    /**
     * 向上递归查找gradle工程的根目录，如果没有找到就返回当前项目的根目录
     *
     * @param project 当前项目
     * @param path    当前选中的路径
     * @param checker 路径判断器
     * @return gradle工程的根目录
     */
    public static String findGradlePath(Project project, String path, ProjectChecker checker) {
        if (checker.checkBuildFile(path)) {
            return path;
        }
        File file = new File(path);
        if (!file.exists()) {
            return project.getBasePath();
        } else {
            if (path.equals(project.getBasePath())) {
                return path;
            }
            path = file.getParent();
            if (path.equals(project.getBasePath())) {
                return project.getBasePath();
            } else {
                return findGradlePath(project, path, checker);
            }
        }
    }

    public static void killProcess(KillCommand killCommand) {
        String[] pids;
        try {
            pids = getPids(killCommand);
        } catch (UnsupportedOperationException var8) {
            showErrorMessage("Could not get process data!");
            return;
        }
        if (null == pids || pids.length == 0) {
            showErrorMessage("No " + killCommand.getKillWhat() + " process is running!");
        } else {
            boolean result = true;
            for (String pid : pids) {
                result &= killProcess(pid);
            }
            if (result) {
                showInfo(killCommand.getKillWhat() + " was killed! Your IDE may show you some other dialogs, it's safe to ignore them.");
            } else {
                showErrorMessage("Could not kill " + killCommand.getKillWhat() + "! Check that your system supports killing processes!");
            }
        }
    }

    private static String[] getPids(KillCommand killCommand) {
        return isWindowsOS() ? getPidsOnWindows(killCommand) : getPidsOnUnix(killCommand);
    }

    private static String[] getPidsOnUnix(KillCommand killCommand) throws UnsupportedOperationException {
        ArrayList<String> pids = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec("pgrep -f " + killCommand.getProcessNameOnUnix());
            process.waitFor();
            if (process.exitValue() != 0 && process.exitValue() != 1) {
                throw new UnsupportedOperationException("pgrep returned error value!");
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                pids.add(line);
            }
            bufferedReader.close();
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
            throw new UnsupportedOperationException("pgrep parsing failed!");
        }
        return pids.toArray(new String[0]);
    }

    private static String[] getPidsOnWindows(KillCommand killCommand) throws UnsupportedOperationException {
        ArrayList<String> pids = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();
        Process process;
        try {
            process = runtime.exec("wmic process where " + killCommand.getProcessWhereOnUnixOnWindows() + " get processid");
            process.waitFor();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                pids.add(line);
            }
            bufferedReader.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("wmic parsing failed!");
        }
        return pids.toArray(new String[pids.size()]);
    }

    private static boolean killProcess(String pid) {
        return isWindowsOS() ? killProcessOnWindows(pid) : killProcessOnUnix(pid);
    }

    private static boolean killProcessOnWindows(String pid) {
        Runtime runtime = Runtime.getRuntime();
        Process process;
        boolean result;
        try {
            process = runtime.exec("taskkill /F /PID " + pid);
            process.waitFor();
            result = true;
        } catch (IOException | InterruptedException var6) {
            var6.printStackTrace();
            result = false;
        }

        return result;
    }

    private static boolean killProcessOnUnix(String pid) {
        Runtime runtime = Runtime.getRuntime();
        Process process;
        boolean result;
        try {
            process = runtime.exec("kill -9 " + pid);
            process.waitFor();
            result = process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public static String className2DartFileName(String value) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i != 0 && Pattern.matches("[a-z]",
                    String.valueOf(value.charAt(i - 1)))
                    && Pattern.matches("[A-Z]", String.valueOf(c))) {
                stringBuilder.append("_");
            }
            stringBuilder.append(String.valueOf(c).toLowerCase());
        }
        return stringBuilder.toString();


    }

    interface ActionListener {

        void onRunning(ProgressIndicator progressIndicator);

        void onSuccess();

        void onFailed(Throwable error);

        void onFinished();

        void onCancel();
    }

}
