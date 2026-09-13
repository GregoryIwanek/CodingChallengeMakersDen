package pl.gi.codingchallenge.shared

// The Android target's implementation of commonMain's `expect fun platformName()`.
// Only androidMain can reference Android-specific types; commonMain never does.
actual fun platformName(): String = "Android"
