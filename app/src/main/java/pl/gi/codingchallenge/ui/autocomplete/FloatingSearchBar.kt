package pl.gi.codingchallenge.ui.autocomplete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.ui.autocomplete.testing.AutocompleteTestTags
import pl.gi.codingchallenge.util.colRes
import pl.gi.codingchallenge.util.dimRes
import pl.gi.codingchallenge.util.spRes
import pl.gi.codingchallenge.util.strRes

@Composable
internal fun FloatingSearchBar(
    text: String,
    active: Boolean,
    hasSuggestions: Boolean,
    onTextChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onLeadingIconClick: () -> Unit
) {
    val barCornerRadius: RoundedCornerShape =
        RoundedCornerShape(dimRes(R.dimen.autocomplete_bar_corner_radius))
    val focusManager: FocusManager = LocalFocusManager.current
    val focusRequester: FocusRequester = remember { FocusRequester() }
    // onFocusChanged also fires once on first composition with isFocused = false;
    // forwarding that would close a panel opened via initialActive. Only real
    // focus transitions should change `active`.
    var wasFocused: Boolean by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimRes(R.dimen.autocomplete_bar_height))
            .shadow(
                elevation = dimRes(R.dimen.autocomplete_bar_elevation),
                shape = barCornerRadius
            )
            .background(color = colRes(R.color.autocomplete_surface), shape = barCornerRadius)
            .padding(horizontal = dimRes(R.dimen.autocomplete_bar_inner_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val nowActive: Boolean = !active
                onActiveChange(nowActive)
                if (nowActive) {
                    // Focus the field so expanding via the icon behaves the same as
                    // tapping the field directly — both open and focus together.
                    focusRequester.requestFocus()
                } else {
                    // Clear focus so a later tap re-fires focus-gained and reopens the panel.
                    focusManager.clearFocus()
                }
                onLeadingIconClick()
            },
            enabled = hasSuggestions,
            modifier = Modifier.testTag(AutocompleteTestTags.LEADING_ICON_BUTTON)
        ) {
            // Nothing to expand/collapse below MIN_QUERY_LENGTH chars —
            // a neutral, non-interactive search glyph instead of an
            // arrow that would falsely imply there's a panel to toggle.
            Icon(
                imageVector = when {
                    !hasSuggestions -> Icons.Filled.Search
                    active -> Icons.Filled.KeyboardArrowUp
                    else -> Icons.Filled.KeyboardArrowDown
                },
                contentDescription = when {
                    !hasSuggestions -> null
                    active -> strRes(R.string.autocomplete_leading_icon_collapse)
                    else -> strRes(R.string.autocomplete_leading_icon_expand)
                },
                tint = colRes(R.color.autocomplete_text_secondary)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    text = strRes(R.string.autocomplete_search_placeholder),
                    style = TextStyle(
                        fontSize = spRes(R.dimen.autocomplete_input_text_size),
                        color = colRes(R.color.autocomplete_text_secondary)
                    )
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = spRes(R.dimen.autocomplete_input_text_size),
                    color = colRes(R.color.autocomplete_text_primary)
                ),
                cursorBrush = SolidColor(colRes(R.color.autocomplete_accent)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AutocompleteTestTags.SEARCH_FIELD)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused != wasFocused) {
                            wasFocused = state.isFocused
                            onActiveChange(state.isFocused)
                        }
                    }
            )
        }

        if (text.isNotEmpty()) {
            IconButton(
                onClick = { onTextChange("") },
                modifier = Modifier
                    .size(dimRes(R.dimen.autocomplete_clear_button_size))
                    .testTag(AutocompleteTestTags.CLEAR_BUTTON)
            ) {
                Box(
                    modifier = Modifier
                        .size(dimRes(R.dimen.autocomplete_clear_circle_size))
                        .background(
                            color = colRes(R.color.autocomplete_border),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = strRes(R.string.autocomplete_clear_icon),
                        tint = colRes(R.color.autocomplete_text_secondary),
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_clear_icon_size))
                    )
                }
            }
        } else {
            Box(Modifier.width(dimRes(R.dimen.autocomplete_leading_spacer_width)))
        }
    }
}

@Preview(name = "Empty", showBackground = true, widthDp = 380)
@Composable
private fun FloatingSearchBarEmptyPreview() {
    FloatingSearchBar(
        text = "",
        active = false,
        hasSuggestions = false,
        onTextChange = {},
        onActiveChange = {},
        onLeadingIconClick = {}
    )
}

@Preview(name = "Typing, too short", showBackground = true, widthDp = 380)
@Composable
private fun FloatingSearchBarTypingPreview() {
    FloatingSearchBar(
        text = "ko",
        active = false,
        hasSuggestions = false,
        onTextChange = {},
        onActiveChange = {},
        onLeadingIconClick = {}
    )
}

@Preview(name = "Active with suggestions", showBackground = true, widthDp = 380)
@Composable
private fun FloatingSearchBarActivePreview() {
    FloatingSearchBar(
        text = "kotlin",
        active = true,
        hasSuggestions = true,
        onTextChange = {},
        onActiveChange = {},
        onLeadingIconClick = {}
    )
}
