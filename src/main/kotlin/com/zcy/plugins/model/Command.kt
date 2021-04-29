package com.zcy.plugins.model


class Command(var name: String, var command: Array<String>, var cmd: String) {
    var successMessage: String
    var errorMessage: String

    override fun toString(): String {
        return "Command{" +
                "name='" + name + '\'' +
                ", command='" + command.joinToString(prefix = "[",
            separator = ",",
            postfix = "]",
            limit = command.size,
            truncated = "...",
            transform = {
                it
            }) + '\'' +
                ", successMessage='" + successMessage + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}'
    }

    init {
        successMessage = "Complete!\nRunning $name successfully."
        errorMessage = "Could not running $name!"
    }
}