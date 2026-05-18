package com.papalugo.projectcolorbar

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFrame

/**
 * Escuta quando qualquer janela do IntelliJ ganha foco.
 * É o hook mais confiável — dispara sempre que a janela aparece,
 * seja na abertura inicial, ao trocar de projeto, ou ao voltar
 * de outra janela.
 *
 * Usa um Set para não aplicar mais de uma vez por projeto
 * (evita repintar a cada troca de foco após já ter sido aplicado).
 */
class ProjectColorActivationListener : ApplicationActivationListener {

    private val log = Logger.getInstance(ProjectColorActivationListener::class.java)
    private val appliedProjects = mutableSetOf<String>()

    override fun applicationActivated(ideFrame: IdeFrame) {
        val project = ideFrame.project ?: return
        if (project.isDisposed) return

        val settings = ProjectColorSettings.getInstance(project)
        if (!settings.enabled || settings.colorArgb == -1) return

        // Aplica sempre — garante que após troca de tema ou restart a cor volta
        log.warn("PCB applicationActivated: project=${project.name}, color=${settings.colorArgb}")
        ProjectColorApplier.apply(project)
    }
}
