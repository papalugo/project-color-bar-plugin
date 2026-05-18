package com.papalugo.projectcolorbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.awt.*
import javax.swing.*
import javax.swing.border.MatteBorder

object ProjectColorApplier {

    private const val TOP_BAND_HEIGHT = 12      // px — borda superior (dobro do original 6px)
    private const val SIDE_BAND_THICKNESS = 3   // px — bordas laterais e inferior
    private const val BAND_OPACITY = 0.95f
    private const val COMPONENT_NAME = "ProjectColorBand"

    fun apply(project: Project) {
        val settings = ProjectColorSettings.getInstance(project)
        if (!settings.enabled || settings.colorArgb == -1) return
        val color = Color(settings.colorArgb, true)

        // Tenta pintar; se a janela ainda não existir (startup), agenda nova tentativa
        fun tryPaint(attemptsLeft: Int) {
            val frame = getFrame(project)
            if (frame != null) {
                SwingUtilities.invokeLater { paintFrame(frame, color) }
            } else if (attemptsLeft > 0) {
                SwingUtilities.invokeLater { tryPaint(attemptsLeft - 1) }
            }
        }
        tryPaint(attemptsLeft = 10)
    }

    fun clear(project: Project) {
        val frame = getFrame(project) ?: return
        SwingUtilities.invokeLater { clearFrame(frame) }
    }

    // ── internal ──────────────────────────────────────────────────────────────

    private fun getFrame(project: Project): JFrame? =
        WindowManager.getInstance().getFrame(project)

    private fun paintFrame(frame: JFrame, color: Color) {
        val rootPane = frame.rootPane

        // 1. Faixa colorida no topo via glass pane
        removeOldBand(rootPane)
        val band = ColorBandComponent(color, TOP_BAND_HEIGHT)
        band.name = COMPONENT_NAME
        val glassPane = rootPane.glassPane as? JComponent ?: return
        glassPane.isVisible = true
        glassPane.layout = null
        glassPane.add(band)
        positionBand(band, frame)

        // 2. Borda colorida: topo fino (só uma linha de separação), laterais e base mais finos
        (rootPane.contentPane as? JComponent)?.border = MatteBorder(
            2,                        // top — linha fina de separação
            SIDE_BAND_THICKNESS,      // left
            SIDE_BAND_THICKNESS,      // bottom
            SIDE_BAND_THICKNESS,      // right
            color
        )

        // 3. Tint sutil no background (ajuda no macOS unified title bar)
        rootPane.background = blendWithBackground(color, 0.12f)
        frame.background   = blendWithBackground(color, 0.06f)

        rootPane.revalidate()
        rootPane.repaint()

        // Reposicionar a faixa ao redimensionar
        frame.componentListeners
            .filter { it.javaClass.name.contains("ProjectColorApplier") }
            .forEach { frame.removeComponentListener(it) }
        frame.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent?) {
                positionBand(band, frame)
            }
        })
    }

    private fun clearFrame(frame: JFrame) {
        val rootPane = frame.rootPane
        rootPane.background = null
        frame.background = null
        removeOldBand(rootPane)
        (rootPane.contentPane as? JComponent)?.border = null
        (rootPane.glassPane as? JComponent)?.let { glass ->
            if (glass.componentCount == 0) glass.isVisible = false
        }
        rootPane.revalidate()
        rootPane.repaint()
    }

    private fun removeOldBand(rootPane: JRootPane) {
        val glass = rootPane.glassPane as? JComponent ?: return
        glass.components
            .filter { it.name == COMPONENT_NAME }
            .forEach { glass.remove(it) }
        glass.revalidate()
    }

    private fun positionBand(band: ColorBandComponent, frame: JFrame) {
        val glass = frame.rootPane.glassPane as? JComponent ?: return
        band.setBounds(0, 0, glass.width, TOP_BAND_HEIGHT)
        glass.revalidate()
        glass.repaint()
    }

    private fun blendWithBackground(color: Color, alpha: Float): Color {
        val bg = Color(245, 245, 245)
        val r = (bg.red   * (1 - alpha) + color.red   * alpha).toInt().coerceIn(0, 255)
        val g = (bg.green * (1 - alpha) + color.green * alpha).toInt().coerceIn(0, 255)
        val b = (bg.blue  * (1 - alpha) + color.blue  * alpha).toInt().coerceIn(0, 255)
        return Color(r, g, b)
    }

    // ── componente da faixa superior ──────────────────────────────────────────

    private class ColorBandComponent(
        private val color: Color,
        height: Int
    ) : JComponent() {

        init {
            preferredSize = Dimension(Int.MAX_VALUE, height)
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            // Gradiente horizontal suave
            val grad = GradientPaint(
                0f, 0f,
                Color(color.red, color.green, color.blue, (255 * BAND_OPACITY).toInt()),
                width.toFloat(), 0f,
                Color(color.red, color.green, color.blue, (255 * 0.75f).toInt())
            )
            g2.paint = grad
            g2.fillRect(0, 0, width, height)

            // Linha brilhante no topo
            g2.color = Color(255, 255, 255, 60)
            g2.fillRect(0, 0, width, 2)

            // Sombra na base da faixa
            g2.color = Color(0, 0, 0, 40)
            g2.fillRect(0, height - 1, width, 1)

            g2.dispose()
        }
    }
}
