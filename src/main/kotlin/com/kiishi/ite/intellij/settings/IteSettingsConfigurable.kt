package com.kiishi.ite.intellij.settings

import com.intellij.openapi.options.Configurable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class IteSettingsConfigurable : Configurable {
    private val executableField = JTextField()
    private val argsField = JTextField()
    private val resumePromptModeField = JComboBox(RESUME_MODES.map { arrayOf(it.first, it.second) }.toTypedArray())
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "iTE"

    override fun createComponent(): JComponent {
        val form = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets.set(4, 4, 4, 4)
        }

        addRow(form, constraints, 0, "Executable", executableField)
        addRow(form, constraints, 1, "Arguments", argsField)
        addRow(form, constraints, 2, "Resume last session", resumePromptModeField)

        panel = form
        reset()
        return form
    }

    override fun isModified(): Boolean {
        val state = IteSettingsState.getInstance().state
        return executableField.text.trim() != state.executable ||
            parseArgs(argsField.text) != state.args ||
            selectedResumeMode() != state.resumePromptMode
    }

    override fun apply() {
        val state = IteSettingsState.getInstance().state
        state.executable = executableField.text.trim().ifEmpty { "ite" }
        state.args = parseArgs(argsField.text).toMutableList()
        state.resumePromptMode = selectedResumeMode()
    }

    override fun reset() {
        val state = IteSettingsState.getInstance().state
        executableField.text = state.executable
        argsField.text = state.args.joinToString(" ")
        resumePromptModeField.selectedItem = state.resumePromptMode
    }

    private fun selectedResumeMode(): String {
        val value = resumePromptModeField.selectedItem as? String ?: IteSettingsState.PROMPT_MODE_ASK
        return if (value in PROMPT_MODES) value else IteSettingsState.PROMPT_MODE_ASK
    }

    private fun addRow(
        form: JPanel,
        constraints: GridBagConstraints,
        row: Int,
        label: String,
        field: JComponent,
    ) {
        constraints.gridx = 0
        constraints.gridy = row
        constraints.weightx = 0.0
        form.add(JLabel(label), constraints)

        constraints.gridx = 1
        constraints.weightx = 1.0
        form.add(field, constraints)
    }

    private fun parseArgs(value: String): List<String> {
        return value.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private companion object {
        private val PROMPT_MODES = setOf(
            IteSettingsState.PROMPT_MODE_ASK,
            IteSettingsState.PROMPT_MODE_AUTO,
            IteSettingsState.PROMPT_MODE_NEVER,
        )

        private val RESUME_MODES = listOf(
            IteSettingsState.PROMPT_MODE_ASK to "Ask once per project (default)",
            IteSettingsState.PROMPT_MODE_AUTO to "Resume automatically",
            IteSettingsState.PROMPT_MODE_NEVER to "Never resume",
        )
    }
}
