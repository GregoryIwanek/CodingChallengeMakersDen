package pl.gi.codingchallenge.shared.converter

// The "domain" layer of this toy feature: pure, framework-free functions with no
// Android/coroutines/DI dependency, so they compile and behave identically on every
// KMP target without needing an expect/actual split at all.
fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9 / 5 + 32
fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32) * 5 / 9
fun kilometersToMiles(km: Double): Double = km * 0.621371
fun milesToKilometers(miles: Double): Double = miles / 0.621371
