//
//  iosAppApp.swift
//  iosApp
//
//  Created by Grzegorz Iwanek on 16/09/2026.
//

import SwiftUI
import Shared

@main
struct iosAppApp: App {
    init() {
        KoinBootstrapKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
