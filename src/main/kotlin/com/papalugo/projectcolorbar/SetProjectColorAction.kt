package com.papalugo.projectcolorbar

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorChooserService
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

/**
 * Opens a color picker and applies the chosen color to the current project window.
 * Accessible via Tools → Set Project Color...
 */
class SetProjectColorAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = ProjectColorSettings.getInstance(project)

        val dialog = ColorPickerDialog(project, settings)
        if (dialog.showAndGet()) {
            val color = dialog.selectedColor ?: return
            settings.colorArgb = color.rgb
            settings.enabled = true
            ProjectColorApplier.apply(project)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}

// ─────────────────────────────────────────────────────────────────────────────

private class ColorPickerDialog(
    project: Project,
    private val settings: ProjectColorSettings
) : DialogWrapper(project) {

    var selectedColor: Color? =
        if (settings.enabled && settings.colorArgb != -1)
            Color(settings.colorArgb, true)
        else null

    // Preset palette — easy one-click choices
    private val presets = listOf(
        "#E53935" to "Red",
        "#8E24AA" to "Purple",
        "#1E88E5" to "Blue",
        "#00ACC1" to "Cyan",
        "#43A047" to "Green",
        "#F4511E" to "Orange",
        "#FFB300" to "Amber",
        "#6D4C41" to "Brown",
        "#546E7A" to "Steel",
        "#F06292" to "Pink",
    )

    private var previewPanel: JPanel? = null

    init {
        title = "Set Project Color"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val root = JPanel(BorderLayout(0, 12))
        root.border = JBUI.Borders.empty(16)
        root.preferredSize = Dimension(420, 320)

        // ── Preview strip ────────────────────────────────────────────────────
        val preview = JPanel().apply {
            preferredSize = Dimension(0, 40)
            background = selectedColor ?: JBColor.GRAY
            border = BorderFactory.createTitledBorder("Preview")
        }
        previewPanel = preview
        root.add(preview, BorderLayout.NORTH)

        // ── Preset swatches ──────────────────────────────────────────────────
        val swatchPanel = JPanel(GridLayout(2, 5, 6, 6))
        swatchPanel.border = BorderFactory.createTitledBorder("Quick colors")
        for ((hex, name) in presets) {
            val c = Color.decode(hex)
            val btn = JButton().apply {
                background = c
                toolTipText = name
                preferredSize = Dimension(36, 36)
                isFocusPainted = false
                border = BorderFactory.createLineBorder(c.darker(), 2)
                addActionListener {
                    selectedColor = c
                    updatePreview(c)
                }
            }
            swatchPanel.add(btn)
        }
        root.add(swatchPanel, BorderLayout.CENTER)

        // ── Custom color button ──────────────────────────────────────────────
        val customBtn = JButton("Choose custom color…")
        customBtn.addActionListener {
            val chosen = JColorChooser.showDialog(
                root,
                "Pick a color",
                selectedColor ?: Color(70, 130, 180)
            )
            if (chosen != null) {
                selectedColor = chosen
                updatePreview(chosen)
            }
        }
        root.add(customBtn, BorderLayout.SOUTH)

        return root
    }

    private fun updatePreview(color: Color) {
        previewPanel?.background = color
        previewPanel?.repaint()
    }
}
