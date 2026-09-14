package pl.gi.codingchallenge.shared

// expect/actual demo: this is the "platform abstraction" layer, proof-of-mechanics
// only. commonMain declares the contract with no body; every compile target
// (androidMain below, iosMain later) must supply exactly one `actual`
// implementation or that target's compilation fails.
expect fun platformName(): String
