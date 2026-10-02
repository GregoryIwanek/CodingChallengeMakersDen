package pl.gi.codingchallenge.feature.autocomplete.util

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Reads a `sp`-declared dimension resource as a [TextUnit].
 *
 * Compose has no built-in equivalent of `dimensionResource()` for text
 * sizes. `resources.getDimension(id)` resolves an sp resource to raw
 * pixels using `value * density * fontScale` — the device's font-scale
 * accessibility setting is already baked in by the time that number
 * comes back. Wrapping it directly in `.sp` would apply font-scale a
 * second time during Compose's own text measurement. Dividing out
 * `density` and `fontScale` here recovers the original declared sp
 * magnitude, so Compose's text pipeline applies font-scale exactly
 * once, correctly.
 */
@Composable
internal fun spRes(@DimenRes id: Int): TextUnit {
    val px: Float = LocalContext.current.resources.getDimension(id)
    val density: Density = LocalDensity.current
    return (px / density.density / density.fontScale).sp
}

/** Short alias for [stringResource]. */
@Composable
internal fun strRes(@StringRes id: Int): String = stringResource(id)

/** Short alias for [dimensionResource]. */
@Composable
internal fun dimRes(@DimenRes id: Int): Dp = dimensionResource(id)

/** Short alias for [colorResource]. */
@Composable
internal fun colRes(@ColorRes id: Int): Color = colorResource(id)
