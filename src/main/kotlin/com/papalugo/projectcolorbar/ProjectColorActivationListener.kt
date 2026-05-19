package com.papalugo.projectcolorbar

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFrame

/**
 * Listens for any IntelliJ window gaining focus.
 * This is the most reliable hook — fires whenever the window appears,
 * whether on initial open, project switch, or returning from another window.
 */
class ProjectColorActivationListener : ApplicationActivationListener {

    private val log = Logger.getInstance(ProjectColorActivationListener::class.java)
    private val appliedProjects = mutableSetOf<String>()

    override fun applicationActivated(ideFrame: IdeFrame) {
        val project = ideFrame.project ?: return
        if (project.isDisposed) return

        val settings = ProjectColorSettings.getInstance(project)
        if (!settings.enabled || settings.colorArgb == -1) return

        // Always re-apply — ensures color is restored after theme change or restart
        log.warn("PCB applicationActivated: project=${project.name}, color=${settings.colorArgb}")
        ProjectColorApplier.apply(project)
    }
}
