/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package com.haier.uhome.plugins.sdk;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FlutterSdk {

    public static final String DART_SDK_SUFFIX = "/bin/cache/dart-sdk";


    private static final Logger LOG = Logger.getInstance(FlutterSdk.class);

    private static final Map<String, FlutterSdk> projectSdkCache = new HashMap<>();

    private final @NotNull
    VirtualFile myHome;
    private final Map<String, String> cachedConfigValues = new HashMap<>();

    private FlutterSdk(@NotNull final VirtualFile home) {
        myHome = home;
    }

    @NotNull
    public VirtualFile getHome() {
        return myHome;
    }

    @NotNull
    public String getHomePath() {
        return myHome.getPath();
    }

    /**
     * Return the FlutterSdk for the given project.
     * <p>
     * Returns null if the Dart SDK is not set or does not exist.
     */
    @Nullable
    public static FlutterSdk getFlutterSdk(@NotNull final Project project) {
        if (project.isDisposed()) {
            return null;
        }

        final DartSdk dartSdk = DartSdk.getDartSdk(project);
        if (dartSdk == null) {
            return null;
        }

        final String dartPath = dartSdk.getHomePath();
        if (!dartPath.endsWith(DART_SDK_SUFFIX)) {
            return null;
        }

        final String sdkPath = dartPath.substring(0, dartPath.length() - DART_SDK_SUFFIX.length());
        return FlutterSdk.forPath(sdkPath);
    }

    @Nullable
    public static FlutterSdk forPath(@NotNull final String path) {
        final VirtualFile home = LocalFileSystem.getInstance().findFileByPath(path);
        if (home == null || !isFlutterSdkHome(path)) {
            return null;
        } else {
            return saveSdkInCache(home);
        }
    }

    @NotNull
    private static FlutterSdk saveSdkInCache(VirtualFile home) {
        String cacheKey = home.getCanonicalPath();
        synchronized (projectSdkCache) {
            if (!projectSdkCache.containsKey(cacheKey)) {
                projectSdkCache.put(cacheKey, new FlutterSdk(home));
            }
        }
        return projectSdkCache.get(cacheKey);
    }


    public static boolean isFlutterSdkHome(@NotNull final String path) {
        final File flutterPubspecFile = new File(path + "/packages/flutter/pubspec.yaml");
        final File flutterToolFile = new File(path + "/bin/flutter");
        final File dartLibFolder = new File(path + "/bin/cache/dart-sdk/lib");
        return flutterPubspecFile.isFile() && flutterToolFile.isFile() && dartLibFolder.isDirectory();
    }


}
