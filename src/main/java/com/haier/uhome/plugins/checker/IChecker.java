package com.haier.uhome.plugins.checker;

public interface IChecker {
    /**
     * 检查当前路径是否为flutter工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    boolean checkFlutter(String path);

    /**
     * 检查当前路径是否包含project_config信息，仅支持syn_module工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    boolean checkPostGet(String path);


    /**
     * 检查当前路径是否为gradle工程
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    boolean checkGradle(String path);

    /**
     * 检查当前路径是否包含build.gradle文件
     *
     * @param path 待检查路径
     * @return 校验结果，true 为 通过
     */
    boolean checkBuildFile(String path);
}
