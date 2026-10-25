package lk.moneytrail.app.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icons used by the app, drawn in Kotlin as Compose ImageVectors so no XML drawables
 * are needed. The line icons copy the SVG paths from the Figma screens and are
 * coloured by Icon's tint. GoogleLogo keeps its own colours: show it with
 * `tint = Color.Unspecified`.
 */
object MoneyTrailIcons {

    private const val EYE = "M2,12s4,-7 10,-7 10,7 10,7 -4,7 -10,7S2,12 2,12z"
    private const val PUPIL = "M9,12a3,3 0,1 0,6 0a3,3 0,1 0,-6 0"

    val Visibility = lineIcon("Visibility", 2f, EYE, PUPIL)

    val VisibilityOff = lineIcon("VisibilityOff", 2f, EYE, PUPIL, "M4,4l16,16")

    val ChevronLeft = lineIcon("ChevronLeft", 2.2f, "M15,18l-6,-6 6,-6")

    val ArrowForward = lineIcon("ArrowForward", 2.4f, "M5,12h14M13,6l6,6 -6,6")

    val Lock = lineIcon(
        "Lock", 2f,
        "M7,10h10a3,3 0,0 1,3 3v5a3,3 0,0 1,-3 3h-10a3,3 0,0 1,-3 -3v-5a3,3 0,0 1,3 -3z",
        "M8,10V7a4,4 0,0 1,8 0v3",
        "M10.5,15.5a1.5,1.5 0,1 0,3 0a1.5,1.5 0,1 0,-3 0"
    )

    val Mail = lineIcon(
        "Mail", 2f,
        "M6,5h12a3,3 0,0 1,3 3v8a3,3 0,0 1,-3 3h-12a3,3 0,0 1,-3 -3v-8a3,3 0,0 1,3 -3z",
        "M4,7.5l8,5.5 8,-5.5"
    )

    val Check = lineIcon("Check", 2.5f, "M5,12.5l4.5,4.5L19,7.5")

    val Circle = lineIcon("Circle", 2.5f, "M5,12a7,7 0,1 0,14 0a7,7 0,1 0,-14 0")

    val ChevronRight = lineIcon("ChevronRight", 2.2f, "M9,18l6,-6 -6,-6")

    val Close = lineIcon("Close", 2.2f, "M6,6l12,12M18,6L6,18")

    val Plus = lineIcon("Plus", 2.4f, "M12,5v14M5,12h14")

    val Calendar = lineIcon(
        "Calendar", 2f,
        "M6,4h12a3,3 0,0 1,3 3v11a3,3 0,0 1,-3 3h-12a3,3 0,0 1,-3 -3v-11a3,3 0,0 1,3 -3z",
        "M3,9h18M8,2v4M16,2v4"
    )

    val Folder = lineIcon(
        "Folder", 2f,
        "M3,7a2,2 0,0 1,2 -2h4l2,2h8a2,2 0,0 1,2 2v8a2,2 0,0 1,-2 2H5a2,2 0,0 1,-2 -2z"
    )

    val Goal = lineIcon("Goal", 2f, "M4,21v-17M4,4h12l-2,4 2,4h-12")

    val Insights = lineIcon("Insights", 2f, "M4,20V10M10,20V4M16,20v-7M22,20H2")

    val More = lineIcon(
        "More", 2f,
        "M3.5,12a1.5,1.5 0,1 0,3 0a1.5,1.5 0,1 0,-3 0",
        "M10.5,12a1.5,1.5 0,1 0,3 0a1.5,1.5 0,1 0,-3 0",
        "M17.5,12a1.5,1.5 0,1 0,3 0a1.5,1.5 0,1 0,-3 0"
    )

    val Note = lineIcon(
        "Note", 2f,
        "M14,3H6a2,2 0,0 0,-2 2v14a2,2 0,0 0,2 2h12a2,2 0,0 0,2 -2V9z",
        "M14,3v6h6",
        "M8,13h8M8,17h5"
    )

    /** Bank card, for the account picker. */
    val Card = lineIcon(
        "Card", 2f,
        "M5,5h14a3,3 0,0 1,3 3v8a3,3 0,0 1,-3 3h-14a3,3 0,0 1,-3 -3v-8a3,3 0,0 1,3 -3z",
        "M2,10h20"
    )

    val Backspace = lineIcon(
        "Backspace", 2f,
        "M21,5H9l-7,7 7,7h12a2,2 0,0 0,2 -2V7a2,2 0,0 0,-2 -2z",
        "M17,9l-6,6M11,9l6,6"
    )

    val Trash = lineIcon("Trash", 2f, "M4,7h16M10,11v6M14,11v6M6,7l1,13h10l1,-13M9,7V4h6v3")

    /** A pencil, for editing a goal. */
    val Edit = lineIcon("Edit", 2f, "M4,20h4l10.5,-10.5a2.8,2.8 0,0 0,-4 -4L4,16z", "M13.5,6.5l4,4")

    /** Two sliders, for Settings. */
    val Settings = lineIcon(
        "Settings", 2f,
        "M4,7h9M17,7h3M13,7a2,2 0,1 0,4 0a2,2 0,1 0,-4 0",
        "M4,17h3M11,17h9M7,17a2,2 0,1 0,4 0a2,2 0,1 0,-4 0"
    )

    val SignOut = lineIcon(
        "SignOut", 2f,
        "M9,21H5a2,2 0,0 1,-2 -2V5a2,2 0,0 1,2 -2h4",
        "M16,17l5,-5 -5,-5M21,12H9"
    )

    /** A crossed-out cloud, for the offline banner. */
    val CloudOff = lineIcon(
        "CloudOff", 2f,
        "M7,19h10.5a4.5,4.5 0,0 0,0.9 -8.91A6,6 0,0 0,6.3 10.6a4.25,4.25 0,0 0,0.7 8.4z",
        "M3,3l18,18"
    )

    /** Google's "G" for the "Continue with Google" button. */
    val GoogleLogo: ImageVector = ImageVector.Builder(
        name = "GoogleLogo",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 48f,
        viewportHeight = 48f
    ).apply {
        addPath(
            pathData = addPathNodes("M24,9.5c3.54,0 6.71,1.22 9.21,3.6l6.85,-6.85C35.9,2.38 30.47,0 24,0 14.62,0 6.51,5.38 2.56,13.22l7.98,6.19C12.43,13.72 17.74,9.5 24,9.5z"),
            fill = SolidColor(Color(0xFFEA4335))
        )
        addPath(
            pathData = addPathNodes("M46.98,24.55c0,-1.57 -0.15,-3.09 -0.38,-4.55H24v9.02h12.94c-0.58,2.96 -2.26,5.48 -4.78,7.18l7.73,6c4.51,-4.18 7.09,-10.36 7.09,-17.65z"),
            fill = SolidColor(Color(0xFF4285F4))
        )
        addPath(
            pathData = addPathNodes("M10.53,28.59c-0.48,-1.45 -0.76,-2.99 -0.76,-4.59s0.27,-3.14 0.76,-4.59l-7.98,-6.19C0.92,16.46 0,20.12 0,24c0,3.88 0.92,7.54 2.56,10.78l7.97,-6.19z"),
            fill = SolidColor(Color(0xFFFBBC05))
        )
        addPath(
            pathData = addPathNodes("M24,48c6.48,0 11.93,-2.13 15.89,-5.81l-7.73,-6c-2.15,1.45 -4.92,2.3 -8.16,2.3 -6.26,0 -11.57,-4.22 -13.47,-9.91l-7.98,6.19C6.51,42.62 14.62,48 24,48z"),
            fill = SolidColor(Color(0xFF34A853))
        )
    }.build()
}

/** A 24x24 outline icon made of round-capped strokes, like the Figma icons. */
private fun lineIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
