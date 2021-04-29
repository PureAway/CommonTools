package com.zcy.plugins.model

class KillCommand(var killWhat: String, var processNameOnUnix: String, var processWhereOnUnixOnWindows: String) {
    override fun toString(): String {
        return "KillCommand{" +
                "killWhat='" + killWhat + '\'' +
                ", processNameOnUnix='" + processNameOnUnix + '\'' +
                ", processWhereOnUnixOnWindows='" + processWhereOnUnixOnWindows + '\'' +
                '}'
    }
}