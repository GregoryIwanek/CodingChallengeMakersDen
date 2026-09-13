package pl.gi.codingchallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import pl.gi.codingchallenge.ui.unitconverter.UnitConverterScreen

/**
 * Debug-only entry point for the :shared KMP toy feature (kmp-interview-prep step 1).
 * Not in the launcher or the real app navigation on purpose - start manually:
 * adb shell am start -n pl.gi.codingchallenge/.UnitConverterActivity
 */
@AndroidEntryPoint
class UnitConverterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UnitConverterScreen()
                }
            }
        }
    }
}
