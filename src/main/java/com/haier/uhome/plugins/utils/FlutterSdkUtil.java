/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package com.haier.uhome.plugins.utils;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.ExecutionException;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class FlutterSdkUtil {
  /**
   * The environment variable to use to tell the flutter tool which app is driving it.
   */
  public static final String FLUTTER_HOST_ENV = "FLUTTER_HOST";

  private static final String FLUTTER_SDK_KNOWN_PATHS = "FLUTTER_SDK_KNOWN_PATHS";
  private static final Logger LOG = Logger.getInstance(FlutterSdkUtil.class);
  private static final String FLUTTER_SNAP_SDK_PATH = "/snap/flutter/common/flutter";

  private FlutterSdkUtil() {
  }

  /**
   * Return the environment variable value to use when shelling out to the Flutter command-line tool.
   */
  public static String getFlutterHostEnvValue() {
    final String clientId = ApplicationNamesInfo.getInstance().getFullProductName().replaceAll(" ", "-");
    final String existingVar = System.getenv(FLUTTER_HOST_ENV);
    return existingVar == null ? clientId : (existingVar + ":" + clientId);
  }

  public static void updateKnownSdkPaths(@NotNull final String newSdkPath) {
    updateKnownPaths(FLUTTER_SDK_KNOWN_PATHS, newSdkPath);
  }

  private static void updateKnownPaths(@SuppressWarnings("SameParameterValue") @NotNull final String propertyKey,
                                       @NotNull final String newPath) {
    final Set<String> allPaths = new LinkedHashSet<>();

    // Add the new value first; this ensures that it's the 'default' flutter sdk.
    allPaths.add(newPath);

    final PropertiesComponent props = PropertiesComponent.getInstance();

    // Add the existing known paths.
    final String[] oldPaths = props.getValues(propertyKey);
    if (oldPaths != null) {
      allPaths.addAll(Arrays.asList(oldPaths));
    }

    // Store the values back.
    if (allPaths.isEmpty()) {
      props.unsetValue(propertyKey);
    }
    else {
      props.setValues(propertyKey, ArrayUtil.toStringArray(allPaths));
    }
  }


  @NotNull
  public static String pathToFlutterTool(@NotNull String sdkPath) throws ExecutionException {
    final String path = findDescendant(sdkPath, "/bin/" + flutterScriptName());
    if (path == null) {
      throw new ExecutionException("Flutter SDK is not configured");
    }
    return path;
  }

  @NotNull
  public static String flutterScriptName() {
    return SystemInfo.isWindows ? "flutter.bat" : "flutter";
  }

  /**
   * Returns the path to the Dart SDK within a Flutter SDK, or null if it doesn't exist.
   */
  @Nullable
  public static String pathToDartSdk(@NotNull String flutterSdkPath) {
    return findDescendant(flutterSdkPath, "/bin/cache/dart-sdk");
  }

  @Nullable
  private static String findDescendant(@NotNull String flutterSdkPath, @NotNull String path) {
    final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(flutterSdkPath + path);
    if (file == null || !file.exists()) {
      return null;
    }
    return file.getPath();
  }

  public static boolean isFlutterSdkHome(@NotNull final String path) {
    final File flutterPubspecFile = new File(path + "/packages/flutter/pubspec.yaml");
    final File flutterToolFile = new File(path + "/bin/flutter");
    final File dartLibFolder = new File(path + "/bin/cache/dart-sdk/lib");
    return flutterPubspecFile.isFile() && flutterToolFile.isFile() && dartLibFolder.isDirectory();
  }

  private static boolean isFlutterSdkHomeWithoutDartSdk(@NotNull final String path) {
    final File flutterPubspecFile = new File(path + "/packages/flutter/pubspec.yaml");
    final File flutterToolFile = new File(path + "/bin/flutter");
    final File dartLibFolder = new File(path + "/bin/cache/dart-sdk/lib");
    return flutterPubspecFile.isFile() && flutterToolFile.isFile() && !dartLibFolder.isDirectory();
  }


  @VisibleForTesting
  public static String parseFlutterSdkPath(String packagesFileContent) {
    for (String line : packagesFileContent.split("\n")) {
      // flutter:file:///Users/.../flutter/packages/flutter/lib/
      line = line.trim();

      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      final String flutterPrefix = "flutter:";
      if (line.startsWith(flutterPrefix)) {
        final String urlString = line.substring(flutterPrefix.length());
        final String path = extractSdkPathFromUri(urlString, true);
        if (path == null) {
          continue;
        }
        return path;
      }
    }

    return null;
  }

  private static String extractSdkPathFromUri(String urlString, boolean isLibIncluded) {
    if (urlString.startsWith("file:")) {
      final Url url = Urls.parseEncoded(urlString);
      if (url == null) {
        return null;
      }
      final String path = url.getPath();
      // go up three levels for .packages or two for .dart_tool/package_config.json
      File file = new File(url.getPath());
      file = file.getParentFile().getParentFile();
      if (isLibIncluded) {
        file = file.getParentFile();
      }
      return file.getPath();
    }
    return null;
  }

}
