"""Generates docs/architecture/architecture.svg.

Run from the repo root: python3 docs/architecture/tools/gen_architecture_svg.py docs/architecture/architecture.svg
Then check it renders: open the SVG in a browser (or headless Chrome) and look for overlaps.
"""
import sys
from xml.sax.saxutils import escape

W, H = 1600, 1100
NAVY = "#13263A"
INK = "#1F3448"
GREY = "#5B6B7A"
LINE = "#C9D3DC"
TEAL = "#2F8F8C"
TEAL_BG = "#E3F1F0"
ORANGE = "#E8772E"
BG = "#F3F6F8"
CHIP = {"UI": ("#DCEAF7", "#2C5E8C"), "VM": ("#FCE9DA", "#B05A1F"), "IN": (TEAL_BG, TEAL)}

out = []


def t(x, y, s, size=13, fill=INK, weight="400", anchor="start", extra=""):
    out.append(
        f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}" font-weight="{weight}" '
        f'text-anchor="{anchor}" {extra}>{escape(s)}</text>'
    )


def rect(x, y, w, h, fill="#FFFFFF", stroke=LINE, rx=10, sw=1.5, extra=""):
    out.append(
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" '
        f'stroke="{stroke}" stroke-width="{sw}" {extra}/>'
    )


def path(d, stroke, marker=None, dash=None, sw=2.2):
    m = f' marker-end="url(#{marker})"' if marker else ""
    da = f' stroke-dasharray="{dash}"' if dash else ""
    out.append(f'<path d="{d}" fill="none" stroke="{stroke}" stroke-width="{sw}"{da}{m}/>')


def pill(cx, cy, label, color, w=None):
    w = w or int(len(label) * 6.6 + 20)
    rect(cx - w / 2, cy - 11, w, 22, fill="#FFFFFF", stroke=color, rx=11, sw=1.5)
    t(cx, cy + 4.5, label, size=11.5, fill=color, weight="700", anchor="middle")


def card(x, y, w, h, title, icon, sub=None, head=NAVY):
    rect(x, y, w, h, extra='filter="url(#shadow)"')
    out.append(
        f'<path d="M{x},{y + 40} v-30 a10,10 0 0 1 10,-10 h{w - 20} a10,10 0 0 1 10,10 v30 z" '
        f'fill="{head}"/>'
    )
    out.append(
        f'<use href="#{icon}" x="{x + 12}" y="{y + 9}" width="22" height="22" stroke="#FFFFFF"/>'
    )
    t(x + 42, y + 26, title, size=16, fill="#FFFFFF", weight="700")
    if sub:
        t(x + 14, y + 60, sub, size=12, fill=GREY, extra='font-style="italic"')


def lines(x, y, items, gap=20, size=13):
    for i, s in enumerate(items):
        bold = s.startswith("*")
        indent = 14 if s.startswith("  ") else 0
        t(x + indent, y + i * gap, s.lstrip("*").strip(), size=size,
          weight="700" if bold else "400")


def chips(x, y, rows):
    for i, (tag, label) in enumerate(rows):
        bg, fg = CHIP[tag]
        yy = y + i * 26
        rect(x, yy - 14, 30, 19, fill=bg, stroke=bg, rx=5, sw=1)
        t(x + 15, yy, tag, size=10.5, fill=fg, weight="800", anchor="middle")
        t(x + 40, yy, label, size=13)


def sublayer(x, y, w, h, title, note=None):
    rect(x, y, w, h, fill="#FFFFFF", stroke=TEAL, rx=8, sw=1.5)
    rect(x, y, w, 28, fill=TEAL_BG, stroke=TEAL, rx=8, sw=1.5)
    t(x + 12, y + 19, title, size=13.5, fill=TEAL, weight="800")
    if note:
        t(x + w - 12, y + 19, note, size=11, fill=TEAL, anchor="end")


ICONS = """
<symbol id="i-phone" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="3" width="16" height="26" rx="3"/><path d="M12 10h8M12 14h8M12 18h5M15 25h2"/></symbol>
<symbol id="i-layers" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M16 4l12 6-12 6-12-6z"/><path d="M4 16l12 6 12-6M4 22l12 6 12-6"/></symbol>
<symbol id="i-search" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="14" cy="14" r="8"/><path d="M20 20l8 8"/></symbol>
<symbol id="i-doc" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3h11l6 6v20H8z"/><path d="M19 3v6h6M12 16h9M12 21h9"/></symbol>
<symbol id="i-share" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="4" width="10" height="10" rx="2"/><rect x="18" y="18" width="10" height="10" rx="2"/><path d="M9 14v9h9M23 18V9h-9"/></symbol>
<symbol id="i-server" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="5" width="22" height="9" rx="2"/><rect x="5" y="18" width="22" height="9" rx="2"/><path d="M10 9.5h.01M10 22.5h.01M15 9.5h8M15 22.5h8"/></symbol>
<symbol id="i-cloud" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M9 25h15a6 6 0 0 0 .8-11.95A8 8 0 0 0 9.3 12 6.5 6.5 0 0 0 9 25z"/></symbol>
<symbol id="i-sparkle" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M14 4l2.5 7.5L24 14l-7.5 2.5L14 24l-2.5-7.5L4 14l7.5-2.5z"/></symbol>
<symbol id="i-gear" viewBox="0 0 32 32" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="16" cy="16" r="4.5"/><path d="M16 3v4M16 25v4M3 16h4M25 16h4M6.8 6.8l2.8 2.8M22.4 22.4l2.8 2.8M6.8 25.2l2.8-2.8M22.4 9.6l2.8-2.8"/></symbol>
<symbol id="i-lock" viewBox="0 0 32 32" fill="none" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="14" width="20" height="14" rx="3"/><path d="M10 14v-4a6 6 0 0 1 12 0v4"/></symbol>
"""


def marker(mid, color):
    return (
        f'<marker id="{mid}" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" '
        f'markerHeight="6" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" '
        f'fill="{color}"/></marker>'
    )


out.append(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" '
    "font-family=\"'Inter', 'Helvetica Neue', Helvetica, Arial, sans-serif\">"
)
out.append(
    "<title>CodingChallenge architecture: modules, layers, DI wiring and runtime flows</title>"
)
out.append("<defs>")
out.append(
    '<filter id="shadow" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="0" '
    'dy="3" stdDeviation="4" flood-color="#0B1B2B" flood-opacity="0.12"/></filter>'
)
out.append(marker("aNavy", NAVY) + marker("aTeal", TEAL) + marker("aOrange", ORANGE))
out.append(ICONS)
out.append("</defs>")

# Canvas and frame
rect(0, 0, W, H, fill="#FFFFFF", stroke="none", rx=0, sw=0)
rect(20, 20, W - 40, H - 40, fill=BG, stroke=TEAL, rx=18, sw=3)

# Title
t(60, 88, "CODINGCHALLENGE · ARCHITECTURE", size=34, fill=NAVY, weight="800",
  extra='letter-spacing="-0.5"')
rect(60, 102, 120, 5, fill=ORANGE, stroke="none", rx=2.5, sw=0)
t(60, 134, "Gradle modules, the layers inside them, how DI connects them, and what talks to "
  "what at runtime", size=15, fill=GREY)

# Legend
lx, ly = 1020, 58
rect(lx, ly, 530, 96, fill="#FFFFFF", stroke=LINE, rx=10)
legend = [
    (NAVY, None, "Gradle dependency", 0, 0),
    (TEAL, "6 5", "DI: object provided", 1, 0),
    (ORANGE, None, "Runtime call / stream", 0, 1),
    (NAVY, "4 4", "Implements (points inward)", 1, 1),
]
for color, dash, label, col, row in legend:
    x0, y0 = lx + 20 + col * 260, ly + 28 + row * 28
    path(f"M{x0},{y0} h44", color, marker={NAVY: "aNavy", TEAL: "aTeal", ORANGE: "aOrange"}[color],
         dash=dash, sw=2)
    t(x0 + 56, y0 + 4.5, label, size=13)
for i, (tag, meaning) in enumerate([("UI", "Compose UI"), ("VM", "ViewModel"),
                                    ("IN", "Hilt-injected")]):
    bg, fg = CHIP[tag]
    x0 = lx + 20 + i * 168
    rect(x0, ly + 70, 30, 19, fill=bg, stroke=bg, rx=5, sw=1)
    t(x0 + 15, ly + 84, tag, size=10.5, fill=fg, weight="800", anchor="middle")
    t(x0 + 38, ly + 84, meaning, size=12.5, fill=GREY)

# Zone labels and trust boundary
t(60, 182, "ON DEVICE", size=12, fill=TEAL, weight="800", extra='letter-spacing="1.5"')
t(1180, 182, "SERVER · INTERNET", size=12, fill=TEAL, weight="800", extra='letter-spacing="1.5"')
path("M1150,165 V1005", ORANGE, dash="8 7", sw=2)

# Row 1: platform UI modules
card(60, 200, 240, 200, "iosApp", "i-phone", "SwiftUI · built by Xcode, not Gradle")
lines(74, 290, ["ContentView: search list",
                "Debounce + min-length in Swift",
                "Calls GitHubSearchRepository",
                "  from Koin via KoinHelper"])

card(330, 200, 270, 200, ":app", "i-layers", "Thin shell · Hilt · nav graph")
lines(344, 290, ["AppNavHost: demo → detail",
                 "DemoScreen: 3 tabs",
                 "CodingChallengeApp: startKoin",
                 "  + PROXY_BASE_URL property",
                 "*SharedKoinBridgeModule",
                 "src/debug: cleartext to 10.0.2.2"], gap=19)

card(630, 200, 240, 200, ":feature:autocomplete", "i-search", "Reusable search bar")
chips(644, 292, [("UI", "GitHubAutocompleteBar"), ("UI", "SuggestionPanel"),
                 ("VM", "AutocompleteViewModel"), ("IN", "SearchAutocompleteUseCase")])

card(890, 200, 240, 200, ":feature:detail", "i-doc", "Detail screen + Explain card")
chips(904, 292, [("UI", "ResultDetailScreen"), ("UI", "ExplainCard"),
                 ("VM", "ExplainRepoViewModel"), ("IN", "LlmClient")])

# Row 2: :shared
rect(60, 450, 1070, 440, fill="#FFFFFF", stroke=NAVY, rx=12, sw=2, extra='filter="url(#shadow)"')
out.append('<path d="M60,492 v-30 a12,12 0 0 1 12,-12 h1046 a12,12 0 0 1 12,12 v30 z" '
           f'fill="{NAVY}"/>')
out.append(f'<use href="#i-share" x="74" y="459" width="24" height="24" stroke="#FFFFFF"/>')
t(108, 478, ":shared · Kotlin Multiplatform", size=17, fill="#FFFFFF", weight="700")
t(1114, 477, "commonMain compiles for Android and iOS", size=12.5, fill="#BFD3E3", anchor="end")

rect(80, 508, 1030, 52, fill=TEAL_BG, stroke=TEAL, rx=8, sw=1.5)
out.append(f'<use href="#i-gear" x="92" y="520" width="26" height="26" stroke="{TEAL}"/>')
t(128, 529, "di/ · SharedModule (Koin) builds everything below", size=13.5, fill=TEAL,
  weight="800")
t(128, 548, "Platform modules add the Ktor engine (OkHttp / Darwin) and the SQLDelight driver",
  size=12.5, fill=INK)

sublayer(80, 590, 250, 280, "remote/", "GitHub REST")
lines(94, 646, ["GitHubApi (Ktor client)",
                "Typed GitHubApiException",
                "GitHubSearchNetworkSourceImpl",
                "  users + repos in parallel",
                "ResultMappers · dto/"], gap=22)

sublayer(370, 590, 430, 280, "domain/", "pure Kotlin · no I/O")
lines(386, 646, ["*model/",
                 "  SearchResultItem · SearchOutcome · QueryRequest",
                 "*usecase/  SearchAutocompleteUseCase",
                 "  debounce · min-length · flatMapLatest",
                 "*repository/  GitHubSearchRepository (interface)",
                 "  CachingGitHubSearchRepository: network-first",
                 "  NetworkSource + Cache interfaces",
                 "*ResultMerger: merge · sort · cap at 50"], gap=24, size=12.5)

sublayer(840, 590, 270, 120, "cache/", "local")
lines(854, 646, ["SearchResultCache",
                 "SQLDelight · 50 newest queries",
                 "Fallback when network fails"], gap=21)

sublayer(840, 730, 270, 140, "llm/", "standalone")
lines(854, 786, ["LlmClient + LlmChunk",
                 "LlmClientImpl: Ktor SSE,",
                 "  channelFlow (cancel = close)",
                 "LlmChunkMapping (pure)"], gap=21)

# Implements arrows, pointing inward into domain
path("M330,730 H366", NAVY, "aNavy", dash="4 4", sw=2)
path("M840,650 H804", NAVY, "aNavy", dash="4 4", sw=2)

# Row 2 right: server side
card(1180, 200, 370, 270, ":proxy", "i-server", "JVM Ktor server · 127.0.0.1:8080")
lines(1194, 290, ["*Holds ANTHROPIC_API_KEY (env only)",
                  "ExplainRoutes: GET /explain → SSE frames",
                  "ReadmeSource: GitHub /readme",
                  "RepoExplainer: Anthropic SDK, streaming",
                  "Guards: 10 calls/min · README ≤ 40k",
                  "No Gradle edge: reached over HTTP"], gap=22)

card(1180, 520, 220, 100, "Anthropic API", "i-sparkle", "Claude · streamed deltas")
t(1194, 600, "Called only by :proxy", size=13)

card(1180, 790, 370, 100, "GitHub REST API", "i-cloud", "Unauthenticated · 10 searches/min")
t(1194, 870, "search/users · search/repositories · /readme", size=13)

# Gradle dependency arrows
path("M600,300 H626", NAVY, "aNavy")                       # :app -> autocomplete
path("M575,200 V178 H1010 V196", NAVY, "aNavy")            # :app -> detail (over the top)
path("M400,400 V446", NAVY, "aNavy")                       # :app -> :shared
path("M750,400 V446", NAVY, "aNavy")                       # autocomplete -> :shared
path("M1010,400 V446", NAVY, "aNavy")                      # detail -> :shared
path("M100,400 V446", NAVY, "aNavy")                       # iosApp -> :shared
t(108, 417, "Shared.framework", size=11.5, fill=NAVY, weight="700")

# DI arrows: Koin graph handed out
path("M540,508 V404", TEAL, "aTeal", dash="6 5")
pill(540, 430, "Koin → Hilt bridge", TEAL)
path("M360,508 V428 H250 V404", TEAL, "aTeal", dash="6 5")
pill(305, 428, "initKoin()", TEAL)

# Runtime flows
path("M1110,770 H1440 V474", ORANGE, "aOrange")            # llm -> :proxy (SSE)
pill(1290, 770, "GET /explain ⇄ SSE", ORANGE)
path("M1290,470 V516", ORANGE, "aOrange")                  # :proxy -> Anthropic
path("M1500,470 V786", ORANGE, "aOrange")                  # :proxy -> GitHub readme
pill(1500, 690, "README", ORANGE)
path("M205,870 V930 H1300 V894", ORANGE, "aOrange")        # remote -> GitHub search
pill(700, 930, "HTTPS search requests", ORANGE)

# Trust boundary tag
rect(1075, 948, 150, 30, fill=NAVY, stroke="none", rx=15, sw=0)
out.append(f'<use href="#i-lock" x="1088" y="954" width="18" height="18" stroke="#FFFFFF"/>')
t(1160, 968, "DEVICE | SERVER", size=12, fill="#FFFFFF", weight="800", anchor="middle")

# build-logic strip
rect(60, 958, 990, 44, fill="#FFFFFF", stroke=LINE, rx=10)
out.append(f'<use href="#i-gear" x="74" y="967" width="26" height="26" stroke="{GREY}"/>')
t(110, 985, "build-logic/convention", size=13.5, fill=NAVY, weight="800")
t(282, 985, "android.library · compose · paparazzi · hilt  →  both features apply all four; "
  ":app applies hilt", size=13, fill=INK)

# Footer notes
t(60, 1040, "The two features never depend on each other: :app's AppNavHost passes "
  "autocomplete's onItemClick to navigateToDetail.", size=13, fill=GREY)
t(60, 1062, "The API key never crosses the boundary: the app only ever receives summary text "
  "over SSE. Release builds have no proxy URL, so the Explain card is hidden.",
  size=13, fill=GREY)

out.append("</svg>")
with open(sys.argv[1], "w") as f:
    f.write("\n".join(out) + "\n")
