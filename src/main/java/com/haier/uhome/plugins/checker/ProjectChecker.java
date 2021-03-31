package com.haier.uhome.plugins.checker;

import com.haier.uhome.plugins.utils.Utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ProjectChecker implements IChecker {

    private final List<String> flutterCheckFiles = Arrays.asList("lib", ".packages", "pubspec.yaml");
    private final List<String> postGetCheckFiles = Arrays.asList("project_config", "postget.sh");
    private final List<String> gradleCheckFiles = Arrays.asList(".gradle", "gradle", "build.gradle",
            "gradlew", "gradlew.bat", "settings.gradle");
    private final List<String> buildCheckFiles = Arrays.asList("build.gradle");

    @Override
    public boolean checkFlutter(String path) {
        if (Utils.isEmptyString(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory() || dir.listFiles() == null) {
            return false;
        }
        int count = 0;
        for (File file : dir.listFiles()) {
            if (flutterCheckFiles.contains(file.getName())) {
                count++;
            }
        }
        return count == flutterCheckFiles.size();
    }

    @Override
    public boolean checkPostGet(String path) {
        if (Utils.isEmptyString(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory() || dir.listFiles() == null) {
            return false;
        }
        int count = 0;
        for (File file : dir.listFiles()) {
            if (postGetCheckFiles.contains(file.getName())) {
                count++;
            }
        }
        return count == postGetCheckFiles.size();
    }

    @Override
    public boolean checkGradle(String path) {
        if (Utils.isEmptyString(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory() || dir.listFiles() == null) {
            return false;
        }
        int count = 0;
        for (File file : dir.listFiles()) {
            if (gradleCheckFiles.contains(file.getName())) {
                count++;
            }
        }
        return count == gradleCheckFiles.size();
    }

    @Override
    public boolean checkBuildFile(String path) {
        if (Utils.isEmptyString(path)) {
            return false;
        }
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory() || dir.listFiles() == null) {
            return false;
        }
        int count = 0;
        for (File file : dir.listFiles()) {
            if (buildCheckFiles.contains(file.getName())) {
                count++;
            }
        }
        return count == buildCheckFiles.size();
    }
}
