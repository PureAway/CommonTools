package com.zcy.plugins.checker

import com.intellij.openapi.project.Project

interface IChecker {

    /**
     * 检查当前路径是否为flutter工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    fun checkFlutter(path: String?): Boolean


    /**
     * 检查当前路径是否包含project_config信息，仅支持syn_module工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    fun checkPostGet(path: String?): Boolean


    /**
     * 检查当前打开的工作目录是否包含.git目录
     *
     * @param project 打开的工作目录
     * @return 校验结果，true 为 通过
     */
    fun checkoutGit(project: Project?): Boolean

}