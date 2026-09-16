# Diagramming Tools for Layers & System Connections

**Status:** reference, not a decision — a comparison of tools for visualizing this repo's
module/layer structure and dependency wiring, plus one worked example. Revisit if the team
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

## Worked example

**[GitHub Autocomplete Architecture](https://claude.ai/artifact/TGHqdZFQJz2ZnB8CpLDSW6)** — a
layer + dependency diagram built with the Artifact method (see below), showing how a search
request crosses the `:app`/`:shared` module boundary: the domain-facing interfaces declared in
`:shared` invert the dependency, `:app`'s Hilt graph supplies the Retrofit network source,
`:shared`'s Koin graph supplies the SQLDelight cache, and one bridge module connects the two DI
containers where they share the cache singleton.

This is the same mechanism `docs/architecture/github-search-caching-decision.md` describes in
prose — the diagram exists because a cold reader can see the module boundary, the two DI
containers, and where the bridge crosses it faster than assembling that from the write-up alone.

## The Artifact method

What produced the worked example above: a hand-authored HTML page with inline `<svg>` (native
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
  prose (like the caching decision doc), reach for **Mermaid** first — lowest friction, versioned
  as text, no external dependency.
- For a genuinely polished, onboarding-facing diagram worth linking from the README or a PR, the
  **Artifact method** used above is a reasonable choice — see the worked example.
- **Structurizr** is worth adopting only if the team wants formal C4-style diagrams enforced
  across multiple docs, not for a single one-off.
- **draw.io** is the fallback when a diagram needs manual layout control Mermaid can't express
  (irregular shapes, precise positioning) and a link-only artifact isn't the right fit.
