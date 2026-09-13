package pl.gi.codingchallenge.ui.unitconverter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gi.codingchallenge.shared.history.ConversionRecord
import pl.gi.codingchallenge.shared.history.ConversionType

@Composable
fun UnitConverterScreen(viewModel: UnitConverterViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UnitConverterContent(
        uiState = uiState,
        onInputChanged = viewModel::onInputChanged,
        onTypeSelected = viewModel::onTypeSelected,
        onConvertClicked = viewModel::onConvertClicked,
    )
}

@Composable
private fun UnitConverterContent(
    uiState: UnitConverterUiState,
    onInputChanged: (String) -> Unit,
    onTypeSelected: (ConversionType) -> Unit,
    onConvertClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Running on: ${uiState.platformName}", style = MaterialTheme.typography.labelLarge)

        ConversionTypeDropdown(selected = uiState.selectedType, onTypeSelected = onTypeSelected)

        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = onInputChanged,
            label = { Text("Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.result?.toString().orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Result") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = onConvertClicked) {
            Text("Convert")
        }

        HorizontalDivider()
        Text("History", style = MaterialTheme.typography.titleSmall)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(uiState.history.asReversed()) { record -> HistoryRow(record) }
        }
    }
}

@Composable
private fun ConversionTypeDropdown(
    selected: ConversionType,
    onTypeSelected: (ConversionType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ConversionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(record: ConversionRecord) {
    Text("${record.type.label}:  ${record.input} → ${record.output}")
}
