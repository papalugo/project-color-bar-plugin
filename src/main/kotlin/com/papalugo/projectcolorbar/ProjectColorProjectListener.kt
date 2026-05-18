package com.papalugo.projectcolorbar

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.wm.WindowManager
import javax.swing.SwingUtilities

/**
 * Escuta o evento projectOpened — dispara quando um projeto é aberto
 * ou quando se troca de projeto. Complementa o ApplicationActivationListener
 * para os casos em que a janela já tem foco e o activated não dispara.
 */
class ProjectColorProjectListener : ProjectManagerListener {

    private val log = Logger.getInstance(ProjectColorProjectListener::class.java)

    override fun projectOpened(project: Project) {
        val settings = ProjectColorSettings.getInstance(project)
        log.warn("PCB projectOpened: project=${project.name} enabled=${settings.enabled} color=${settings.colorArgb}")
        if (!settings.enabled || settings.colorArgb == -1) return

        // Polling: aguarda o frame estar visível
        waitForFrame(project)
    }

    private fun waitForFrame(project: Project, attempt: Int = 0) {
        if (project.isDisposed) return

        val frame = WindowManager.getInstance().getFrame(project)
        log.warn("PCB waitForFrame attempt=$attempt frame=$frame showing=${frame?.isShowing}")

        if (frame != null && frame.isShowing) {
            log.warn("PCB applying color via projectOpened")
            ProjectColorApplier.apply(project)
        } else if (attempt < 40) {
            SwingUtilities.invokeLater { waitForFrame(project, attempt + 1) }
        } else {
            log.warn("PCB timeout in projectOpened after $attempt attempts")
        }
    }
}
