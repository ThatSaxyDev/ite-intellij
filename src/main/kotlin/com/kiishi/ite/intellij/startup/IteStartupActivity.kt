package com.kiishi.ite.intellij.startup

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.startup.StartupActivity
import com.kiishi.ite.intellij.actions.IteNotifier
import com.kiishi.ite.intellij.runtime.IteRuntime
import com.kiishi.ite.intellij.runtime.IteRuntimeService
import com.kiishi.ite.intellij.settings.IteSettingsState

class IteStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking iTE", true) {
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

                val properties = PropertiesComponent.getInstance()
                if (IteSettingsState.getInstance().state.resumeLastSession && !properties.getBoolean(WELCOME_SHOWN_KEY, false)) {
                    properties.setValue(WELCOME_SHOWN_KEY, true)
                    IteNotifier.info(
                        project,
                        "iTE is ready",
                        "Use Tools | iTE | Open iTE to resume the latest workspace session.",
                    )
                }
            }

            override fun onThrowable(error: Throwable) {
                IteNotifier.error(project, "iTE startup check failed", error.message ?: error.javaClass.simpleName)
            }
        })
    }

    companion object {
        private const val WELCOME_SHOWN_KEY = "ite.intellij.welcomeShown"
    }
}
