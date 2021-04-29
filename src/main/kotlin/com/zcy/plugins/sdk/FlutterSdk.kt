/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package com.zcy.plugins.sdk

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.*

class FlutterSdk private constructor(val home: VirtualFile) {
    private val cachedConfigValues: Map<String, String> = HashMap()
    val homePath: String
        get() = home.path

    companion object {
        const val DART_SDK_SUFFIX = "/bin/cache/dart-sdk"
        private val LOG = Logger.getInstance(
            FlutterSdk::class.java
        )
        private val projectSdkCache: MutableMap<String?, FlutterSdk?> = HashMap()

        /**
         * Return the FlutterSdk for the given project.
         *
         *
         * Returns null if the Dart SDK is not set or does not exist.
         */
        fun getFlutterSdk(project: Project): FlutterSdk? {
            if (project.isDisposed) {
                return null
            }
            val dartSdk :DartSdk = DartSdk.getDartSdk(project) ?: return null
            val dartPath = dartSdk.homePath
            if (!dartPath.endsWith(DART_SDK_SUFFIX)) {
                return null
            }
            val sdkPath = dartPath.substring(0, dartPath.length - DART_SDK_SUFFIX.length)
            return forPath(sdkPath)
        }

        fun forPath(path: String): FlutterSdk? {
            val home = LocalFileSystem.getInstance().findFileByPath(path)
            return if (home == null || !isFlutterSdkHome(path)) {
                null
            } else {
                saveSdkInCache(home)
            }
        }

        private fun saveSdkInCache(home: VirtualFile): FlutterSdk {
            val cacheKey = home.canonicalPath
            synchronized(projectSdkCache) {
                if (!projectSdkCache.containsKey(cacheKey)) {
                    projectSdkCache[cacheKey] = FlutterSdk(home)
                }
            }
            return projectSdkCache[cacheKey]!!
        }

        fun isFlutterSdkHome(path: String): Boolean {
            val flutterPubspecFile = File("$path/packages/flutter/pubspec.yaml")
            val flutterToolFile = File("$path/bin/flutter")
            val dartLibFolder = File("$path/bin/cache/dart-sdk/lib")
            return flutterPubspecFile.isFile && flutterToolFile.isFile && dartLibFolder.isDirectory
        }
    }
}