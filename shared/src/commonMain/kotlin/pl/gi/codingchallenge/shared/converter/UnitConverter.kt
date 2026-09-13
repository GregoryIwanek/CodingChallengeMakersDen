package pl.gi.codingchallenge.shared.converter

fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9 / 5 + 32
fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32) * 5 / 9
fun kilometersToMiles(km: Double): Double = km * 0.621371
fun milesToKilometers(miles: Double): Double = miles / 0.621371
