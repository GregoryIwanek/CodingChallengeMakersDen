package pl.gi.codingchallenge.feature.detail

sealed interface ExplainUiState {
    data object Idle : ExplainUiState
    data object WaitingForFirstToken : ExplainUiState
    data class Streaming(val text: String) : ExplainUiState
    data class Done(val text: String, val stoppedByUser: Boolean = false) : ExplainUiState
    data class Refused(val partial: String) : ExplainUiState
    data class Failed(val partial: String, val reason: ExplainFailure) : ExplainUiState
}

/** Why a stream failed, as a UI-level reason; the card maps each one to a string resource. */
enum class ExplainFailure { NoReadme, ServiceError, Network }
