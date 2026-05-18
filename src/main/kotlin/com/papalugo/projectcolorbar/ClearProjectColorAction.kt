package com.papalugo.projectcolorbar

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Removes the custom color from the current project window.
 */
class ClearProjectColorAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = ProjectColorSettings.getInstance(project)
        settings.enabled = false
        settings.colorArgb = -1
        ProjectColorApplier.clear(project)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled =
            project != null && ProjectColorSettings.getInstance(project).enabled
    }
}
