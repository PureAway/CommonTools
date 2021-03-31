package com.haier.uhome.plugins.model;

public class KillCommand {

    private String killWhat;
    private String processNameOnUnix;
    private String processWhereOnUnixOnWindows;

    public KillCommand(String killWhat, String processNameOnUnix, String processWhereOnUnixOnWindows) {
        this.killWhat = killWhat;
        this.processNameOnUnix = processNameOnUnix;
        this.processWhereOnUnixOnWindows = processWhereOnUnixOnWindows;
    }

    public String getKillWhat() {
        return killWhat;
    }

    public void setKillWhat(String killWhat) {
        this.killWhat = killWhat;
    }

    public String getProcessNameOnUnix() {
        return processNameOnUnix;
    }

    public void setProcessNameOnUnix(String processNameOnUnix) {
        this.processNameOnUnix = processNameOnUnix;
    }

    public String getProcessWhereOnUnixOnWindows() {
        return processWhereOnUnixOnWindows;
    }

    public void setProcessWhereOnUnixOnWindows(String processWhereOnUnixOnWindows) {
        this.processWhereOnUnixOnWindows = processWhereOnUnixOnWindows;
    }


    @Override
    public String toString() {
        return "KillCommand{" +
                "killWhat='" + killWhat + '\'' +
                ", processNameOnUnix='" + processNameOnUnix + '\'' +
                ", processWhereOnUnixOnWindows='" + processWhereOnUnixOnWindows + '\'' +
                '}';
    }
}
