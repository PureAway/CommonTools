/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package com.haier.uhome.plugins.sdk;

import com.google.common.collect.ImmutableList;
import com.haier.uhome.plugins.console.FlutterConsoles;
import com.haier.uhome.plugins.utils.FlutterSdkUtil;
import com.haier.uhome.plugins.utils.MostlySilentColoredProcessHandler;
import com.haier.uhome.plugins.utils.Utils;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * A Flutter command to run, with its arguments.
 */
public class FlutterCommand {
    private static final Logger LOG = Logger.getInstance(FlutterCommand.class);

    private static final Set<Type> pubRelatedCommands = new HashSet<>(
            Arrays.asList(Type.PUB_GET));

    @NotNull
    protected final FlutterSdk sdk;

    @Nullable
    protected final String workDir;

    @NotNull
    private final Type type;

    @NotNull
    protected final List<String> args;

    /**
     * @see FlutterSdk for methods to create specific commands.
     */
    FlutterCommand(@NotNull FlutterSdk sdk, @Nullable String workDir, @NotNull Type type, String... args) {
        this.sdk = sdk;
        this.workDir = workDir;
        this.type = type;
        this.args = ImmutableList.copyOf(args);
    }

    /**
     * Returns a displayable version of the command that will be run.
     */
    public String getDisplayCommand() {
        final List<String> words = new ArrayList<>();
        words.add("flutter");
        words.addAll(type.subCommand);
        words.addAll(args);
        return String.join(" ", words);
    }

    protected boolean isPubRelatedCommand() {
        return pubRelatedCommands.contains(type);
    }

    /**
     * Starts running the command, without showing its output in a console.
     * <p>
     * If unable to start (for example, if a command is already running), returns null.
     */
    public Process start(@Nullable Consumer<ProcessOutput> onDone, @Nullable ProcessListener processListener) {
        // TODO(skybrian) add Project parameter if it turns out later that we need to set ANDROID_HOME.
        final ColoredProcessHandler handler = startProcessOrShowError(null);
        if (handler == null) {
            return null;
        }

        if (processListener != null) {
            handler.addProcessListener(processListener);
        }

        // Capture all process output if requested.
        if (onDone != null) {
            final CapturingProcessAdapter listener = new CapturingProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    super.processTerminated(event);
                    onDone.accept(getOutput());
                }
            };
            handler.addProcessListener(listener);
        }

        // Transition to "running" state.
        handler.startNotify();

        return handler.getProcess();
    }

    /**
     * Starts running the command, showing its output in a non-module console.
     * <p>
     * Shows the output in a tab in the tool window that's not associated
     * with a particular module. Returns the process handler.
     * <p>
     * If unable to start (for example, if a command is already running), returns null.
     */
    public ColoredProcessHandler startInConsole(@NotNull Project project) {
        final ColoredProcessHandler handler = startProcessOrShowError(project);
        if (handler != null) {
            FlutterConsoles.displayProcessLater(handler, project, null, handler::startNotify);
        }
        return handler;
    }

    /**
     * Starts running the command, showing its output in a module console.
     * <p>
     * Shows the output in the tool window's tab corresponding to the passed-in module.
     * Returns the process.
     * <p>
     * If unable to start (for example, if a command is already running), returns null.
     */
    public Process startInModuleConsole(@NotNull Module module, @Nullable Runnable onDone, @Nullable ProcessListener processListener) {
        final ColoredProcessHandler handler = startProcessOrShowError(module.getProject());
        if (handler == null) {
            return null;
        }
        if (processListener != null) {
            handler.addProcessListener(processListener);
        }
        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                if (onDone != null) {
                    onDone.run();
                }
            }
        });

        FlutterConsoles.displayProcessLater(handler, module.getProject(), module, handler::startNotify);
        return handler.getProcess();
    }

    @Override
    public String toString() {
        return "FlutterCommand(" + getDisplayCommand() + ")";
    }


    /**
     * Starts a process that runs a flutter command, unless one is already running.
     * <p>
     * If a project is supplied, it will be used to determine the ANDROID_HOME variable for the subprocess.
     * <p>
     * Returns the handler if successfully started.
     */
    @NotNull
    public FlutterCommandStartResult startProcess(@Nullable Project project) {
        final ColoredProcessHandler handler;
        try {
            final GeneralCommandLine commandLine = createGeneralCommandLine(project);
            LOG.info(commandLine.toString());
            handler = new MostlySilentColoredProcessHandler(commandLine);
            return new FlutterCommandStartResult(handler);
        } catch (ExecutionException e) {
            return new FlutterCommandStartResult(e);
        }
    }

    /**
     * Starts a process that runs a flutter command, unless one is already running.
     * <p>
     * If a project is supplied, it will be used to determine the ANDROID_HOME variable for the subprocess.
     * <p>
     * Returns the handler if successfully started, or null if there was a problem.
     */
    @Nullable
    public ColoredProcessHandler startProcessOrShowError(@Nullable Project project) {
        final FlutterCommandStartResult result = startProcess(project);
        if (result.status == FlutterCommandStartResultStatus.EXCEPTION && result.exception != null) {
            Utils.showErrorNotification(project, type.title +
                    " flutter.command.exception.message " + result.exception.getMessage());
        }
        return result.processHandler;
    }

    /**
     * Creates the command line to run.
     * <p>
     * If a project is supplied, it will be used to determine the ANDROID_HOME variable for the subprocess.
     */
    @NotNull
    public GeneralCommandLine createGeneralCommandLine(@Nullable Project project) {
        final GeneralCommandLine line = new GeneralCommandLine();
        line.setCharset(CharsetToolkit.UTF8_CHARSET);
        line.withEnvironment(FlutterSdkUtil.FLUTTER_HOST_ENV, FlutterSdkUtil.getFlutterHostEnvValue());
        line.setExePath(FileUtil.toSystemDependentName(sdk.getHomePath() + "/bin/" + FlutterSdkUtil.flutterScriptName()));
        if (!Utils.isEmptyString(workDir)) {
            line.setWorkDirectory(workDir);
        }
        line.addParameters(type.subCommand);
        line.addParameters(args);
        return line;
    }

    enum Type {
        CLEAN("Flutter clean", "clean"),
        PUB_GET("Flutter pub get", "pub", "get");

        final public String title;
        final ImmutableList<String> subCommand;

        Type(String title, String... subCommand) {
            this.title = title;
            this.subCommand = ImmutableList.copyOf(subCommand);
        }

    }
}
