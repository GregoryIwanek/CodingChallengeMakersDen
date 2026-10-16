package pl.gi.codingchallenge.feature.detail

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import pl.gi.codingchallenge.feature.detail.testing.DetailTestTags

private const val PULSE_DURATION_MILLIS: Int = 700
private const val CARET: String = "▍"
private val SHIMMER_LINE_WIDTHS: List<Float> = listOf(1f, 0.9f, 0.6f)

/** The "Explain this repo" card: one branch per [ExplainUiState]. Retry is [onExplain] again. */
@Composable
internal fun ExplainCard(
    state: ExplainUiState,
    onExplain: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DetailTestTags.EXPLAIN_CARD)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.detail_explain_padding)),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.detail_explain_spacing)
            )
        ) {
            Text(
                text = stringResource(R.string.detail_explain_title),
                style = MaterialTheme.typography.titleMedium
            )
            when (state) {
                ExplainUiState.Idle -> Button(onClick = onExplain) {
                    Text(stringResource(R.string.detail_explain_button))
                }
                ExplainUiState.WaitingForFirstToken -> {
                    ShimmerLines()
                    StopButton(onStop = onStop)
                }
                is ExplainUiState.Streaming -> {
                    StreamingText(text = state.text)
                    StopButton(onStop = onStop)
                }
                is ExplainUiState.Done -> {
                    SummaryText(text = state.text)
                    if (state.stoppedByUser) {
                        Text(
                            text = stringResource(R.string.detail_explain_stopped),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is ExplainUiState.Refused -> PartialWithRetry(
                    partial = state.partial,
                    message = R.string.detail_explain_refused,
                    messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onRetry = onExplain
                )
                is ExplainUiState.Failed -> PartialWithRetry(
                    partial = state.partial,
                    message = state.reason.messageRes(),
                    messageColor = MaterialTheme.colorScheme.error,
                    onRetry = onExplain
                )
            }
        }
    }
}

@StringRes
private fun ExplainFailure.messageRes(): Int = when (this) {
    ExplainFailure.NoReadme -> R.string.detail_explain_error_no_readme
    ExplainFailure.ServiceError -> R.string.detail_explain_error_service
    ExplainFailure.Network -> R.string.detail_explain_error_network
}

@Composable
private fun StopButton(onStop: () -> Unit) {
    TextButton(onClick = onStop) { Text(stringResource(R.string.detail_explain_stop)) }
}

@Composable
private fun SummaryText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

/** Keeps whatever text arrived before the stream ended, then explains why, with a Retry. */
@Composable
private fun PartialWithRetry(
    partial: String,
    @StringRes message: Int,
    messageColor: Color,
    onRetry: () -> Unit
) {
    if (partial.isNotEmpty()) {
        SummaryText(text = partial)
    }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = messageColor
    )
    TextButton(onClick = onRetry) { Text(stringResource(R.string.detail_explain_retry)) }
}

/** Placeholder lines; alpha is read in graphicsLayer, so the pulse redraws without recomposing. */
@Composable
private fun ShimmerLines() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha: Float by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MILLIS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.detail_explain_spacing)
        )
    ) {
        SHIMMER_LINE_WIDTHS.forEach { widthFraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(dimensionResource(R.dimen.detail_explain_shimmer_line_height))
                    .graphicsLayer { this.alpha = alpha }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

/**
 * The text so far plus a blinking caret. The caret's alpha lives in the text's span, so this
 * recomposes every animation frame; fine for one Text.
 */
@Composable
private fun StreamingText(text: String) {
    val transition = rememberInfiniteTransition(label = "caret")
    val caretAlpha: Float by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MILLIS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretAlpha"
    )
    val caretColor: Color = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = caretColor.copy(alpha = caretAlpha))) { append(CARET) }
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

private const val PREVIEW_TEXT: String =
    "OkHttp is an efficient HTTP client for Android and the JVM. It pools connections, " +
        "supports HTTP/2 and transparently compresses responses."

@Preview(name = "Idle", showBackground = true)
@Composable
private fun ExplainCardIdlePreview() {
    ExplainCardPreview(state = ExplainUiState.Idle)
}

@Preview(name = "Waiting", showBackground = true)
@Composable
private fun ExplainCardWaitingPreview() {
    ExplainCardPreview(state = ExplainUiState.WaitingForFirstToken)
}

@Preview(name = "Streaming", showBackground = true)
@Composable
private fun ExplainCardStreamingPreview() {
    ExplainCardPreview(
        state = ExplainUiState.Streaming(text = "OkHttp is an efficient HTTP client")
    )
}

@Preview(name = "Done", showBackground = true)
@Composable
private fun ExplainCardDonePreview() {
    ExplainCardPreview(state = ExplainUiState.Done(text = PREVIEW_TEXT))
}

@Preview(name = "Stopped", showBackground = true)
@Composable
private fun ExplainCardStoppedPreview() {
    ExplainCardPreview(
        state = ExplainUiState.Done(text = "OkHttp is an efficient", stoppedByUser = true)
    )
}

@Preview(name = "Refused", showBackground = true)
@Composable
private fun ExplainCardRefusedPreview() {
    ExplainCardPreview(state = ExplainUiState.Refused(partial = ""))
}

@Preview(name = "Failed", showBackground = true)
@Composable
private fun ExplainCardFailedPreview() {
    ExplainCardPreview(
        state = ExplainUiState.Failed(
            partial = "OkHttp is an efficient",
            reason = ExplainFailure.Network
        )
    )
}

@Composable
private fun ExplainCardPreview(state: ExplainUiState) {
    MaterialTheme {
        ExplainCard(state = state, onExplain = {}, onStop = {})
    }
}
