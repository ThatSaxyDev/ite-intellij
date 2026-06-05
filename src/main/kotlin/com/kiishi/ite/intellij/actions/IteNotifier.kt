package com.kiishi.ite.intellij.actions

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ide.BrowserUtil

object IteNotifier {
    fun info(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.INFORMATION)
    }

    fun warning(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.WARNING)
    }

    fun error(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.ERROR)
    }

    fun missingRuntime(project: Project) {
        val notification = group()
            .createNotification(
                "iTE is not installed",
                "Install iTE to use the AI coding agent in a JetBrains IDE terminal.",
                NotificationType.WARNING,
            )
            .addAction(object : NotificationAction("Install iTE") {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent, notification: com.intellij.notification.Notification) {
                    notification.expire()
                    IteActionRunner.installRuntime(project, openAfterInstall = true)
                }
            })
            .addAction(object : NotificationAction("Open Docs") {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent, notification: com.intellij.notification.Notification) {
                    notification.expire()
                    BrowserUtil.browse("https://ite.kiishi.space/docs")
                }
            })

        notification.notify(project)
    }

    private fun notify(project: Project?, title: String, content: String, type: NotificationType) {
        group().createNotification(title, content, type).notify(project)
    }

    private fun group() = NotificationGroupManager.getInstance().getNotificationGroup("iTE")
}
