package com.zcy.plugins.checker

import com.intellij.openapi.project.Project
import com.zcy.plugins.utils.Utils
import java.io.File

class ProjectChecker : IChecker {

    val flutterCheckFiles = listOf("lib", ".packages", "pubspec.yaml")
    val postGetCheckFiles = listOf("project_config", "postget.sh")
    val gitCheckFiles = listOf(".git")

    /**
     * 检查当前路径是否为flutter工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    override fun checkFlutter(path: String?): Boolean {
        return check(path, flutterCheckFiles)
    }

    fun check(path: String?, checkFiles: List<String>): Boolean {
        if (Utils.isEmptyString(path)) {
            return false
        }
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory || null == dir.listFiles()) {
            return false
        }
        var count = 0
        dir.listFiles().forEach { file ->
            if (checkFiles.contains(file.name)) {
                count++
            }
        }
        return checkFiles.size == count
    }

    /**
     * 检查当前路径是否包含project_config信息，仅支持syn_module工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    override fun checkPostGet(path: String?): Boolean {
        return check(path, postGetCheckFiles)
    }


    /**
     * 检查当前打开的工作目录是否包含.git目录
     *
     * @param project 打开的工作目录
     * @return 校验结果，true 为 通过
     */
    override fun checkoutGit(project: Project?): Boolean {
        val dir = File(project!!.basePath)
        if (!dir.exists() || !dir.isDirectory || dir.listFiles() == null) {
            return false
        }
        var count = 0
        for (file in dir.listFiles()) {
            if (gitCheckFiles.contains(file.name)) {
                count++
            }
        }
        return count == gitCheckFiles.size
    }
}