package com.kiishi.ite.intellij.settings

import com.intellij.openapi.options.Configurable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class IteSettingsConfigurable : Configurable {
    private val executableField = JTextField()
    private val argsField = JTextField()
    private val resumeLastSessionField = JCheckBox("Resume the most recent saved iTE session by default")
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

        constraints.gridx = 1
        constraints.gridy = 2
        form.add(resumeLastSessionField, constraints)

        panel = form
        reset()
        return form
    }

    override fun isModified(): Boolean {
        val state = IteSettingsState.getInstance().state
        return executableField.text.trim() != state.executable ||
            parseArgs(argsField.text) != state.args ||
            resumeLastSessionField.isSelected != state.resumeLastSession
    }

    override fun apply() {
        val state = IteSettingsState.getInstance().state
        state.executable = executableField.text.trim().ifEmpty { "ite" }
        state.args = parseArgs(argsField.text).toMutableList()
        state.resumeLastSession = resumeLastSessionField.isSelected
    }

    override fun reset() {
        val state = IteSettingsState.getInstance().state
        executableField.text = state.executable
        argsField.text = state.args.joinToString(" ")
        resumeLastSessionField.isSelected = state.resumeLastSession
    }

    private fun addRow(
        form: JPanel,
        constraints: GridBagConstraints,
        row: Int,
        label: String,
        field: JTextField,
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
}
