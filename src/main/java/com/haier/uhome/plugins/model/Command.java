package com.haier.uhome.plugins.model;

public class Command {

    private String name;
    private String command;
    private String cmd;
    private String successMessage;
    private String errorMessage;
    private boolean needSpace;

    public Command(String name, String command,String cmd) {
        this.command = command;
        this.name = name;
        this.cmd = cmd;
        this.successMessage = "Complete!\nRunning " + name + " successfully.";
        this.errorMessage = "Could not running " + name + "!";
        needSpace = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public boolean isNeedSpace() {
        return needSpace;
    }

    public Command setNeedSpace(boolean needSpace) {
        this.needSpace = needSpace;
        return this;
    }

    @Override
    public String toString() {
        return "Command{" +
                "name='" + name + '\'' +
                ", command='" + command + '\'' +
                ", successMessage='" + successMessage + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", needSpace=" + needSpace +
                '}';
    }
}
