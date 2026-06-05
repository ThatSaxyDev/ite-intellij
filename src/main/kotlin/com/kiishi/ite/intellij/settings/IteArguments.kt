package com.kiishi.ite.intellij.settings

object IteArguments {
    fun defaultArgs(): List<String> {
        val state = IteSettingsState.getInstance().state
        val args = state.args.filter { it.isNotBlank() }.toMutableList()
        if (state.resumeLastSession && "--resume-last" !in args) {
            args += "--resume-last"
        }
        return args
    }

    fun newTerminalArgs(): List<String> {
        return IteSettingsState.getInstance().state.args
            .filter { it.isNotBlank() && it != "--resume-last" }
    }

    fun resumeArgs(): List<String> {
        val args = IteSettingsState.getInstance().state.args.filter { it.isNotBlank() }.toMutableList()
        if ("--resume-last" !in args) {
            args += "--resume-last"
        }
        return args
    }
}
