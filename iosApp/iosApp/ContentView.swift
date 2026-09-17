//
//  ContentView.swift
//  iosApp
//
//  Created by Grzegorz Iwanek on 16/09/2026.
//

import Foundation
import SwiftUI
import Shared

// Mirrors SearchAutocompleteUseCase's constants (shared/.../domain/usecase) -
// duplicated here, not shared, because Kotlin/Native's Flow export has no
// AsyncSequence/`for await` support and no MutableStateFlow constructor exported
// (verified by inspecting the generated Shared.h before writing this file, per
// this guide's "verify, don't guess" discipline). This view re-implements the
// debounce/min-length policy natively over the plain `search` suspend fun instead
// of driving the shared Flow pipeline - the same division of responsibility
// Android's ViewModel/use-case split already has, just both halves are per-platform
// here instead of one shared half.
private let minQueryLength = 3
private let debounceMillis = 350
private let perTypeLimit: Int32 = 50

private enum AutocompleteUiState {
    case idle // < minQueryLength chars
    case loading
    case success([SearchResultItem])
    case empty
    case error(String)
}

struct ContentView: View {
    @State private var queryText: String = ""
    @State private var uiState: AutocompleteUiState = .idle
    @State private var searchTask: Task<Void, Never>?

    private let repository = KoinHelperKt.getGitHubSearchRepository()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                TextField("Search GitHub repos and users", text: $queryText)
                    .textFieldStyle(.roundedBorder)
                    .autocorrectionDisabled()
                    .padding()
                    .onChange(of: queryText) { _, newValue in
                        scheduleSearch(for: newValue)
                    }

                stateView
            }
            .navigationTitle("GitHub Search")
        }
    }

    @ViewBuilder
    private var stateView: some View {
        switch uiState {
        case .idle:
            Spacer()
        case .loading:
            ProgressView()
            Spacer()
        case .empty:
            Text("No results").foregroundStyle(.secondary)
            Spacer()
        case .error(let message):
            Text("Error: \(message)").foregroundStyle(.red).padding()
            Spacer()
        case .success(let items):
            List(items, id: \.uniqueKey) { resultRow($0) }
                .listStyle(.plain)
        }
    }

    @ViewBuilder
    private func resultRow(_ item: SearchResultItem) -> some View {
        if let repo = item as? SearchResultItemRepoResult {
            VStack(alignment: .leading, spacing: 4) {
                Text(repo.fullName).font(.headline)
                if let description = repo.description_ {
                    Text(description).font(.subheadline).foregroundStyle(.secondary)
                }
                Text("\u{2605} \(repo.stars)").font(.caption).foregroundStyle(.secondary)
            }
        } else if let user = item as? SearchResultItemUserResult {
            Text(user.login).font(.headline)
        }
    }

    private func scheduleSearch(for text: String) {
        searchTask?.cancel()

        // Trimmed before the length check and before it reaches the repository/cache key -
        // a whitespace-only or padded query otherwise passes the length gate as-is and
        // fragments the cache ("kotlin" vs "kotlin " being different cache rows). Mirrors
        // the same fix in SearchAutocompleteUseCase.kt on the Android side - this view
        // bypasses that use case entirely (see the comment at the top of this file), so it
        // needs its own copy of the fix, not just a shared one.
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= minQueryLength else {
            uiState = .idle
            return
        }

        searchTask = Task {
            do {
                try await Task.sleep(for: .milliseconds(debounceMillis))
            } catch {
                return // cancelled during debounce, same as the button never being pressed
            }
            guard !Task.isCancelled else { return }

            uiState = .loading
            do {
                let results = try await repository.search(query: trimmed, perTypeLimit: perTypeLimit)
                guard !Task.isCancelled else { return }
                uiState = results.isEmpty ? .empty : .success(results)
            } catch {
                guard !Task.isCancelled else { return }
                uiState = .error(error.localizedDescription)
            }
        }
    }
}

#Preview {
    ContentView()
}
