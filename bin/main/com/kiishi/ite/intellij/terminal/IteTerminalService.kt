package com.kiishi.ite.intellij.terminal

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.kiishi.ite.intellij.runtime.IteRuntime

@Service(Service.Level.PROJECT)
class IteTerminalService(private val project: Project) {
    private var currentTab: TerminalToolWindowTab? = null

    fun open(runtime: IteRuntime, args: List<String>, reuseExisting: Boolean) {
        if (reuseExisting) {
            val existing = currentTab
            if (existing != null && !isDisposed(existing.content)) {
                selectTab(existing)
                return
            }
        }

        val command = listOf(runtime.executable) + args
        val tab = TerminalToolWindowTabsManager.getInstance(project)
            .createTabBuilder()
            .workingDirectory(project.basePath)
            .shellCommand(command)
            .tabName("iTE")
            .requestFocus(true)
            .createTab()

        currentTab = tab
        selectTab(tab)
    }

    private fun selectTab(tab: TerminalToolWindowTab) {
        val content = tab.content
        content.manager?.setSelectedContent(content)
        ToolWindowManager.getInstance(project).getToolWindow("Terminal")?.activate(null)
    }

    @Suppress("DEPRECATION")
    private fun isDisposed(disposable: Disposable): Boolean {
        return com.intellij.openapi.util.Disposer.isDisposed(disposable)
    }
}
