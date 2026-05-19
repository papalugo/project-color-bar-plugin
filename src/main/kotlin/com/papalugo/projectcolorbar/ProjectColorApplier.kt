package com.papalugo.projectcolorbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.border.MatteBorder

object ProjectColorApplier {

    private const val TOP_BAND_HEIGHT    = 7    // px — top bar
    private const val BOTTOM_BAND_HEIGHT = 3    // px — bottom bar
    private const val SIDE_BAND_WIDTH    = 3    // px — left and right bars
    private const val BAND_OPACITY       = 0.95f
    private const val COMPONENT_NAME_TOP    = "ProjectColorBand_Top"
    private const val COMPONENT_NAME_LEFT   = "ProjectColorBand_Left"
    private const val COMPONENT_NAME_RIGHT  = "ProjectColorBand_Right"
    private const val COMPONENT_NAME_BOTTOM = "ProjectColorBand_Bottom"

    fun apply(project: Project) {
        val settings = ProjectColorSettings.getInstance(project)
        if (!settings.enabled || settings.colorArgb == -1) return
        val color = Color(settings.colorArgb, true)

        // Retry until the frame is available (e.g. during startup)
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

        // 1. Remove any previously applied bands
        removeOldBands(rootPane)

        // 2. Create the four color bands (top, left, right, bottom)
        val bandTop    = ColorBandComponent(color, BandOrientation.HORIZONTAL)
        val bandLeft   = ColorBandComponent(color, BandOrientation.VERTICAL_LEFT)
        val bandRight  = ColorBandComponent(color, BandOrientation.VERTICAL_RIGHT)
        val bandBottom = ColorBandComponent(color, BandOrientation.HORIZONTAL_BOTTOM)
        bandTop.name    = COMPONENT_NAME_TOP
        bandLeft.name   = COMPONENT_NAME_LEFT
        bandRight.name  = COMPONENT_NAME_RIGHT
        bandBottom.name = COMPONENT_NAME_BOTTOM

        glassPane.isVisible = true
        glassPane.layout    = null
        glassPane.add(bandTop)
        glassPane.add(bandLeft)
        glassPane.add(bandRight)
        glassPane.add(bandBottom)

        positionBands(bandTop, bandLeft, bandRight, bandBottom, frame)

        // 3. Thin separator line on the content pane top edge
        (rootPane.contentPane as? JComponent)?.border = MatteBorder(2, 0, 0, 0, color)

        // 4. Subtle background tint
        rootPane.background = blendWithBackground(color, 0.12f)
        frame.background    = blendWithBackground(color, 0.06f)

        rootPane.revalidate()
        rootPane.repaint()

        // 5. Reposition bands on window resize
        frame.componentListeners
            .filter { it.javaClass.name.contains("ProjectColorApplier") }
            .forEach { frame.removeComponentListener(it) }
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                positionBands(bandTop, bandLeft, bandRight, bandBottom, frame)
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
            c.name in setOf(COMPONENT_NAME_TOP, COMPONENT_NAME_LEFT, COMPONENT_NAME_RIGHT, COMPONENT_NAME_BOTTOM)
        }
        toRemove.forEach { glass.remove(it) }
        glass.revalidate()
    }

    private fun positionBands(
        top: ColorBandComponent,
        left: ColorBandComponent,
        right: ColorBandComponent,
        bottom: ColorBandComponent,
        frame: JFrame
    ) {
        val glass = frame.rootPane.glassPane as? JComponent ?: return
        val w = glass.width
        val h = glass.height
        top.setBounds(0, 0, w, TOP_BAND_HEIGHT)
        left.setBounds(0, 0, SIDE_BAND_WIDTH, h)
        right.setBounds(w - SIDE_BAND_WIDTH, 0, SIDE_BAND_WIDTH, h)
        bottom.setBounds(0, h - BOTTOM_BAND_HEIGHT, w, BOTTOM_BAND_HEIGHT)
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

    // ── band orientation ──────────────────────────────────────────────────────

    private enum class BandOrientation { HORIZONTAL, HORIZONTAL_BOTTOM, VERTICAL_LEFT, VERTICAL_RIGHT }

    // ── color band component ──────────────────────────────────────────────────

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
                    // Left to right
                    GradientPaint(0f, 0f, solid, width.toFloat(), 0f, fade)

                BandOrientation.HORIZONTAL_BOTTOM ->
                    // Right to left — mirrors the top bar for visual symmetry
                    GradientPaint(width.toFloat(), 0f, solid, 0f, 0f, fade)

                BandOrientation.VERTICAL_LEFT ->
                    // Top to bottom
                    GradientPaint(0f, 0f, solid, 0f, height.toFloat(), fade)

                BandOrientation.VERTICAL_RIGHT ->
                    // Top to bottom
                    GradientPaint(0f, 0f, solid, 0f, height.toFloat(), fade)
            }

            g2.paint = grad
            g2.fillRect(0, 0, width, height)

            // Highlight and shadow details per orientation
            when (orientation) {
                BandOrientation.HORIZONTAL -> {
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(0, 0, width, 2)                   // highlight at top edge
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(0, height - 1, width, 1)          // shadow at bottom edge
                }
                BandOrientation.HORIZONTAL_BOTTOM -> {
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(0, 0, width, 1)                   // shadow at top edge (separator)
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(0, height - 2, width, 2)          // highlight at bottom edge
                }
                BandOrientation.VERTICAL_LEFT -> {
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(0, 0, 2, height)                  // highlight on inner edge
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(width - 1, 0, 1, height)          // shadow on outer edge
                }
                BandOrientation.VERTICAL_RIGHT -> {
                    g2.color = Color(0, 0, 0, 40)
                    g2.fillRect(0, 0, 1, height)                  // shadow on inner edge
                    g2.color = Color(255, 255, 255, 60)
                    g2.fillRect(width - 2, 0, 2, height)          // highlight on outer edge
                }
            }

            g2.dispose()
        }
    }
}
