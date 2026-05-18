package com.papalugo.projectcolorbar

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Persists the chosen color (as ARGB int) for a given project.
 * Stored in .idea/projectColorBar.xml via PersistentStateComponent.
 */
@State(
    name = "ProjectColorSettings",
    storages = [Storage("projectColorBar.xml")]
)
@Service(Service.Level.PROJECT)
class ProjectColorSettings : PersistentStateComponent<ProjectColorSettings.State> {

    data class State(
        var colorArgb: Int = -1,   // -1 means "no color set"
        var enabled: Boolean = false
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var colorArgb: Int
        get() = myState.colorArgb
        set(value) { myState.colorArgb = value }

    var enabled: Boolean
        get() = myState.enabled
        set(value) { myState.enabled = value }

    companion object {
        fun getInstance(project: Project): ProjectColorSettings =
            project.getService(ProjectColorSettings::class.java)
    }
}
