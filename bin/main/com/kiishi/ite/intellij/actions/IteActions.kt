package com.kiishi.ite.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class OpenIteAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        IteActionRunner.openTerminal(project, IteLaunchMode.DEFAULT)
    }
}

class OpenNewIteAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        IteActionRunner.openTerminal(project, IteLaunchMode.NEW)
    }
}

class ResumeLastIteSessionAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        IteActionRunner.openTerminal(project, IteLaunchMode.RESUME)
    }
}

class CheckIteInstallationAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        IteActionRunner.checkInstallation(project)
    }
}

class InstallIteRuntimeAction : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        IteActionRunner.installRuntime(project, openAfterInstall = true)
    }
}
