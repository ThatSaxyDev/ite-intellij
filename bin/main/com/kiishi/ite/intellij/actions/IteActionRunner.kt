package com.kiishi.ite.intellij.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.kiishi.ite.intellij.runtime.IteRuntime
import com.kiishi.ite.intellij.runtime.IteRuntimeService
import com.kiishi.ite.intellij.settings.IteArguments
import com.kiishi.ite.intellij.terminal.IteTerminalService

enum class IteLaunchMode {
    DEFAULT,
    NEW,
    RESUME,
}

object IteActionRunner {
    fun openTerminal(project: Project, mode: IteLaunchMode) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Opening iTE", true) {
            private var runtime: IteRuntime? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Checking iTE runtime"
                runtime = IteRuntimeService.getInstance().resolveRuntime()
            }

            override fun onSuccess() {
                val resolved = runtime ?: return
                if (!resolved.installed) {
                    IteNotifier.missingRuntime(project)
                    return
                }

                ApplicationManager.getApplication().invokeLater {
                    val args = when (mode) {
                        IteLaunchMode.DEFAULT -> IteArguments.defaultArgs()
                        IteLaunchMode.NEW -> IteArguments.newTerminalArgs()
                        IteLaunchMode.RESUME -> IteArguments.resumeArgs()
                    }
                    project.service<IteTerminalService>().open(
                        runtime = resolved,
                        args = args,
                        reuseExisting = mode != IteLaunchMode.NEW,
                    )
                }
            }

            override fun onThrowable(error: Throwable) {
                IteNotifier.error(project, "iTE failed", error.message ?: error.javaClass.simpleName)
            }
        })
    }

    fun checkInstallation(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking iTE Installation", true) {
            private var runtime: IteRuntime? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Checking iTE runtime"
                runtime = IteRuntimeService.getInstance().resolveRuntime()
            }

            override fun onSuccess() {
                val resolved = runtime ?: return
                if (resolved.installed) {
                    val details = if (resolved.message.isBlank()) "." else ": ${resolved.message}"
                    IteNotifier.info(project, "iTE is ready", "Using the ${resolved.source} executable$details")
                } else {
                    IteNotifier.missingRuntime(project)
                }
            }

            override fun onThrowable(error: Throwable) {
                IteNotifier.error(project, "iTE check failed", error.message ?: error.javaClass.simpleName)
            }
        })
    }

    fun installRuntime(project: Project, openAfterInstall: Boolean) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Installing iTE", false) {
            private var executable: String? = null

            override fun run(indicator: ProgressIndicator) {
                executable = IteRuntimeService.getInstance().installManagedRuntime(indicator)
            }

            override fun onSuccess() {
                val path = executable ?: return
                IteNotifier.info(project, "iTE installed", "Installed iTE at $path.")
                if (openAfterInstall) {
                    openTerminal(project, IteLaunchMode.DEFAULT)
                }
            }

            override fun onThrowable(error: Throwable) {
                IteNotifier.error(project, "iTE install failed", error.message ?: error.javaClass.simpleName)
            }
        })
    }

    fun openDocs() {
        BrowserUtil.browse("https://ite.kiishi.space/docs")
    }
}
