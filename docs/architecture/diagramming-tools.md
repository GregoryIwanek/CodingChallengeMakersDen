# Diagramming Tools for Layers & System Connections

**Status:** reference, not a decision — a comparison of tools for visualizing this repo's
module/layer structure and dependency wiring, plus two worked examples. Revisit if the team
settles on one and wants it enforced.

---

## The options

| | **Mermaid** | **Structurizr (C4)** | **draw.io / diagrams.net** | **Artifact (hand-authored SVG)** |
|---|---|---|---|---|
| Format | Text (fenced code block) | Text (DSL) | Binary/XML, GUI-edited | Hand-authored HTML + inline SVG |
| Renders on GitHub | Yes, natively | No — needs Structurizr Lite or the hosted service | No (image export only) | No — lives at a published link |
| Versioned as a diff | Yes | Yes | No (binary-ish XML, noisy diffs) | Yes (it's source) |
| Visual polish | Plain, functional | Clean, purpose-built for layers/containers/components | Highest manual control | Fully custom — matches this doc's own theme, dark/light aware |
| Setup cost | None — just write the fence | Self-host Structurizr Lite, or a hosted account | None — free web app or plugin | None — this session's Artifact tool |
| Best for | Quick diagrams that live next to the code they describe | Formal C4-style architecture docs (context/container/component) | One-off polished diagrams, manual layout control | A designed, interactive-capable diagram meant to be linked and shared |

## Worked examples

**[GitHub Autocomplete Architecture](https://claude.ai/artifact/TGHqdZFQJz2ZnB8CpLDSW6)** (v3) — a
runtime flow diagram built with the Artifact method (see below). It shows how a search request and
a tapped result travel through `:app`, `:feature:autocomplete`, `:feature:detail`, `:shared` and
`iosApp`: the two features never point at each other (`onItemClick` goes back to `:app`'s
`AppNavHost`, which calls `navigateToDetail`), the `AutocompleteViewModel` gets its use case from
`:app`'s Koin→Hilt bridge, `:shared`'s interfaces keep the Ktor network source and SQLDelight
cache behind one repository, and iOS calls that repository directly, bypassing the use case.

**Module graph in [modularization.md](modularization.md#this-repo)** — a Mermaid `flowchart` of the
`implementation(project(…))` dependencies and the convention plugins each module applies. It
renders on GitHub and changes in the same PR as the build files, so it can't drift unnoticed. The
two complement each other: Mermaid for *what depends on what* (versioned), the Artifact for *how a
request flows at runtime* (linked).

**The Artifact has gone stale twice.** v1 put the network source in `:app` as "Retrofit"; a Ktor
migration moved it into `:shared` and nothing caught the drift until a docs pass (`docs/backlog.md`
AT-8, in Resolved). v2 then still showed the autocomplete inside `:app` after the feature-module
extraction; v3 was redrawn alongside the in-repo Mermaid graph. Same tradeoff the recommendation
below names for this method — **if you keep a diagram like this, re-verify it whenever modules
change; a link doesn't age itself for you.**

## The Artifact method

What produced the Artifact worked example above: a hand-authored HTML page with inline `<svg>` (native
shapes — `rect`, `path`, `text`, `line` — no charting library), published as a Claude Artifact.
Practical notes for reusing this:

- **No install, no account** — it's produced directly in a Claude Code conversation and published
  to a link.
- **Themed, not templated** — light/dark aware, uses this project's own palette choices rather
  than a diagramming tool's default theme, and reads well at both phone and desktop widths.
- **Best for a diagram meant to be shared as a link** (e.g. linked from a PR description or this
  doc) rather than one that needs to render inline on GitHub — unlike Mermaid, it doesn't render
  in the markdown file itself, only at its published URL.
- **Higher effort per diagram** than a Mermaid fence — reasonable for a diagram worth getting
  exactly right (like an onboarding-facing architecture map), not for a quick sketch.
- **Not version-controlled with the code** — the published page lives outside this git repo, so
  treat it as a linked reference (like the GitHub screenshots in the README), not as the source of
  truth for the architecture itself. The source of truth remains the code and the prose decision
  docs in this directory.

## Recommendation

No single tool fits every case here:

- For a diagram that should render inline in a markdown doc and get maintained alongside the
  prose (like the caching decision doc, or the module graph in `modularization.md`), reach for
  **Mermaid** first — lowest friction, versioned as text, no external dependency.
- For a genuinely polished, onboarding-facing diagram worth linking from the README or a PR, the
  **Artifact method** used above is a reasonable choice — see the worked examples. Just budget for
  the maintenance cost the row above already names: it went stale twice (see the worked examples'
  note) precisely because nothing in git flags it when the architecture moves. Pair it with a
  Mermaid graph in the repo for the part that must stay exact.
- **Structurizr** is worth adopting only if the team wants formal C4-style diagrams enforced
  across multiple docs, not for a single one-off.
- **draw.io** is the fallback when a diagram needs manual layout control Mermaid can't express
  (irregular shapes, precise positioning) and a link-only artifact isn't the right fit.
