//
//  ContentView.swift
//  iosApp
//
//  Created by Grzegorz Iwanek on 16/09/2026.
//

import SwiftUI
import Shared

struct ContentView: View {
    @State private var fact: String = "Tap to fetch a cat fact"
    private let catFactApi = KoinHelperKt.getCatFactApi()

    var body: some View {
        VStack(spacing: 20) {
            Text(fact).padding()
            Button("Fetch a cat fact") {
                Task { await fetch() }
            }
        }
        .padding()
    }

    private func fetch() async {
        do {
            let result = try await catFactApi.fetchCatFact()
            fact = result.fact
        } catch {
            fact = "Error: \(error.localizedDescription)"
        }
    }
}

#Preview {
    ContentView()
}
