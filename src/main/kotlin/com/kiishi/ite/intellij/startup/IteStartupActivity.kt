package com.kiishi.ite.intellij.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.kiishi.ite.intellij.actions.IteActionRunner
import com.kiishi.ite.intellij.actions.IteLaunchMode
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

                when (IteSettingsState.effectiveResumePromptMode()) {
                    IteSettingsState.PROMPT_MODE_NEVER -> Unit
                    IteSettingsState.PROMPT_MODE_AUTO -> {
                        markResolved(project)
                        IteActionRunner.openTerminal(project, IteLaunchMode.RESUME)
                    }
                    IteSettingsState.PROMPT_MODE_ASK -> offerResume(project)
                }
            }

            override fun onThrowable(error: Throwable) {
                IteNotifier.error(project, "iTE startup check failed", error.message ?: error.javaClass.simpleName)
            }
        })
    }

    private fun offerResume(project: Project) {
        if (hasDecided(project)) {
            return
        }

        IteNotifier.resumePrompt(
            project = project,
            onResume = {
                markDecision(project, RESUME_DECISION_RESUMED)
                IteActionRunner.openTerminal(project, IteLaunchMode.RESUME)
            },
            onSkip = {
                markDecision(project, RESUME_DECISION_SKIPPED)
            },
        )
    }

    private fun projectProperties(project: Project): PropertiesComponent {
        return PropertiesComponent.getInstance(project)
    }

    private fun hasDecided(project: Project): Boolean {
        return projectProperties(project).getBoolean(RESUME_DECISION_KEY, false)
    }

    private fun markDecision(project: Project, value: Boolean) {
        projectProperties(project).setValue(RESUME_DECISION_KEY, value)
    }

    private fun markResolved(project: Project) {
        markDecision(project, true)
    }

    companion object {
        private const val RESUME_DECISION_KEY = "ite.intellij.resumeDecided"
        private const val RESUME_DECISION_RESUMED = true
        private const val RESUME_DECISION_SKIPPED = true
    }
}
