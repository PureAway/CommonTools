package com.zcy.plugins.sdk

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import java.util.*

class DartSdk private constructor(
    val homePath: String,
    /**
     * @return presentable version with revision, like "1.9.1_r44672" or "1.9.0-dev.10.9_r44532" or "1.10.0-edge.44829"
     */
    val version: String
) {

    companion object {
        const val DART_SDK_LIB_NAME = "Dart SDK"
        private const val UNKNOWN_VERSION = "unknown"
        fun getDartSdk(project: Project): DartSdk? {
            val sdk: DartSdk? = findDartSdkAmongLibraries(
                LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries
            )
            sdk?.let {
                val dependencies: MutableList<Any?> = ArrayList(3)
                dependencies.add(ProjectRootManager.getInstance(project))
                ContainerUtil.addIfNotNull<VirtualFile?>(
                    dependencies,
                    LocalFileSystem.getInstance().findFileByPath(sdk.homePath + "/version")
                )
                ContainerUtil.addIfNotNull<VirtualFile?>(
                    dependencies,
                    LocalFileSystem.getInstance().findFileByPath(sdk.homePath + "/lib/core/core.dart")
                )
                return CachedValueProvider.Result(sdk, ArrayUtil.toObjectArray(dependencies)).value
            }
            return CachedValueProvider.Result<DartSdk>(
                null,
                ProjectRootManager.getInstance(project)
            ).value
        }

        private fun findDartSdkAmongLibraries(libs: Array<Library>): DartSdk? {
            for (library in libs) {
                if (DART_SDK_LIB_NAME == library.name) {
                    return getSdkByLibrary(library)
                }
            }
            return null
        }

        fun getSdkByLibrary(library: Library): DartSdk? {
            val roots = library.getFiles(OrderRootType.CLASSES)
            val dartCoreRoot = findDartCoreRoot(Arrays.asList(*roots))
            if (dartCoreRoot != null) {
                val homePath = dartCoreRoot.parent.parent.path
                val version = StringUtil.notNullize(UNKNOWN_VERSION)
                return DartSdk(homePath, version)
            }
            return null
        }

        fun findDartCoreRoot(classesRoots: List<VirtualFile>): VirtualFile? {
            for (root in classesRoots) {
                if (root.isInLocalFileSystem &&
                    root.isDirectory &&
                    root.path.endsWith("/lib/core") && root.findChild("core.dart") != null
                ) {
                    return root
                }
            }
            return null
        }
    }
}