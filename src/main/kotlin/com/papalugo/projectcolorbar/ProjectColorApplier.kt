package com.papalugo.projectcolorbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.border.MatteBorder

object ProjectColorApplier {

    private const val TOP_BAND_HEIGHT    = 7    // px — faixa superior
    private const val SIDE_BAND_WIDTH    = 4    // px — faixas laterais (esq/dir)
    private const val BAND_OPACITY       = 0.95f
    private const val COMPONENT_NAME_TOP  = "ProjectColorBand_Top"
    private const val COMPONENT_NAME_LEFT = "ProjectColorBand_Left"
    private const val COMPONENT_NAME_RIGHT= "ProjectColorBand_Right"

    fun apply(project: Project) {
        val settings = ProjectColorSettings.getInstance(project)
        if (!settings.enabled || settings.colorArgb == -1) return
        val color = Color(settings.colorArgb, true)

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
        val rootPane  = frame.rootPane
        val glassPane = rootPane.glassPane as? JComponent ?: return

        // 1. Remove faixas antigas
        removeOldBands(rootPane)

        // 2. Cria as três faixas (topo, esquerda, direita)
        val bandTop   = ColorBandComponent(color, BandOrientation.HORIZONTAL)
        val bandLeft  = ColorBandComponent(color, BandOrientation.VERTICAL_LEFT)
        val bandRight = ColorBandComponent(color, BandOrientation.VERTICAL_RIGHT)
        bandTop.name   = COMPONENT_NAME_TOP
        bandLeft.name  = COMPONENT_NAME_LEFT
        bandRight.name = COMPONENT_NAME_RIGHT

        glassPane.isVisible = true
        glassPane.layout    = null
        glassPane.add(bandTop)
        glassPane.add(bandLeft)
        glassPane.add(bandRight)

        positionBands(bandTop, bandLeft, bandRight, frame)

        // 3. Borda colorida fina no contentPane (linha de separação)
        (rootPane.contentPane as? JComponent)?.border = MatteBorder(2, 0, 0, 0, color)

        // 4. Tint sutil no background
        rootPane.background = blendWithBackground(color, 0.12f)
        frame.background    = blendWithBackground(color, 0.06f)

        rootPane.revalidate()
        rootPane.repaint()

        // 5. Reposicionar ao redimensionar
        frame.componentListeners
            .filter { it.javaClass.name.contains("ProjectColorApplier") }
            .forEach { frame.removeComponentListener(it) }
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                positionBands(bandTop, bandLeft, bandRight, frame)
            }
        })
    }

    private fun clearFrame(frame: JFrame) {
        val rootPane = frame.rootPane
        rootPane.background = null
        frame.background    = null
        removeOldBands(rootPane)
        (rootPane.contentPane as? JComponent)?.border = null
        (rootPane.glassPane as? JComponent)?.let { glass ->
            if (glass.componentCount == 0) glass.isVisible = false
        }
        rootPane.revalidate()
        rootPane.repaint()
    }

    private fun removeOldBands(rootPane: JRootPane) {
        val glass = rootPane.glassPane as? JComponent ?: return
        val toRemove = glass.components.filter { c ->
            c.name in setOf(COMPONENT_NAME_TOP, COMPONENT_NAME_LEFT, COMPONENT_NAME_RIGHT)
        }
        toRemove.forEach { glass.remove(it) }
        glass.revalidate()
    }

    private fun positionBands(
        top: ColorBandComponent,
        left: ColorBandComponent,
        right: ColorBandComponent,
        frame: JFrame
    ) {
        val glass = frame.rootPane.glassPane as? JComponent ?: return
        val w = glass.width
        val h = glass.height
        top.setBounds(0, 0, w, TOP_BAND_HEIGHT)
        left.setBounds(0, 0, SIDE_BAND_WIDTH, h)
        right.setBounds(w - SIDE_BAND_WIDTH, 0, SIDE_BAND_WIDTH, h)
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

    // ── orientações das faixas ────────────────────────────────────────────────

    private enum class BandOrientation { HORIZONTAL, VERTICAL_LEFT, VERTICAL_RIGHT }

    // ── componente de faixa genérico ──────────────────────────────────────────

    private class ColorBandComponent(
        private val color: Color,
        private val orientation: BandOrientation
    ) : JComponent() {

        init { isOpaque = false }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            val solid = Color(color.red, color.green, color.blue, (255 * BAND_OPACITY).toInt())
            val fade  = Color(color.red, color.green, color.blue, (255 * 0.75f).toInt())

            val grad: GradientPaint = when (orientation) {
                BandOrientation.HORIZONTAL ->
                    // Esquerda→direita (igual ao original)
                    GradientPaint(0f, 0f, solid, width.toFloat(), 0f, fade)

                BandOrientation.VERTICAL_LEFT ->
                    // Topo→base na barra esquerda
                    GradientPaint(0f, 0f, solid, 0f, height.toFloat(), fade)

                BandOrientation.VERTICAL_RIGHT ->
                    // Topo→base na barra direita (mesma direção, simetria visual)
                    GradientPaint(0f, 0f, solid, 0f, height.toFloat(), fade)
            }

            g2.paint = grad
            g2.fillRect(0, 0, width, height)

            // Detalhe de brilho e sombra conforme orientação
            when (orientation) {
                BandOrientation.HORIZONTAL -> {
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(0, 0, width, 2)                   // brilho no topo
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(0, height - 1, width, 1)          // sombra na base
                }
                BandOrientation.VERTICAL_LEFT -> {
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(0, 0, 2, height)                  // brilho na borda interna
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(width - 1, 0, 1, height)          // sombra na borda externa
                }
                BandOrientation.VERTICAL_RIGHT -> {
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(0, 0, 1, height)                  // sombra na borda interna
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(width - 2, 0, 2, height)          // brilho na borda externa
                }
            }

            g2.dispose()
        }
    }
}
