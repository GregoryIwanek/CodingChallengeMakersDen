# `/new-feature` Examples

A guide for you, not for Claude: `SKILL.md` doesn't link here, so Claude never loads this file
and the examples can't sway its answers.

## Arguments

Everything after `/new-feature` becomes `$ARGUMENTS`:

- `/new-feature result-count header above search results`
- `/new-feature result-count header, :app only, spec in docs/ai/harness-test-feature.md`
- `/new-feature show the repo's language as a chip in each result row`

## Example: affected tests (step 3)

From the result-count header: `GitHubAutocompleteBarTest.successState_capsSuggestionPanelHeight_evenWithManyResults`
asserts the panel is exactly 500dp with 50 results. A header above a list that keeps its own
500dp cap breaks it, and androidTests don't run locally, so only reading it up front catches it.

## Example answers

The result-count header as shipped in PR #16 ([harness-test-feature.md](../../../docs/ai/harness-test-feature.md#outcome)).

| # | Question | Example answer |
| --- | --- | --- |
| 1 | What exactly does the user see, and in which UI states? | A "12 results" header fixed above the list, in the `Success` state only |
| 2 | What are the edge cases? | `Empty` is its own state, so `Success` always has ≥ 1 item; 1 → "1 result"; 50 (the cap) → "50+ results" |
| 3 | Which data does it show, and where does it come from? | Items shown in the list, not GitHub's `total_count` |
| 4 | Which module: `:app` only, or `:shared`? | `:app` only; the cap is passed in as `maxResults` instead of exposing `:shared`'s constant |
| 5 | Which files and existing patterns should it follow? | `SuggestionPanel.kt`; `dimensionResource` + existing `autocomplete_row_horizontal_padding`; `AutocompleteTestTags.kt` |
| 6 | New strings? | `<plurals name="autocomplete_result_count">` plus a `<string>` for "%1$d+ results" |
| 7 | Accessibility: anything beyond plain, in-order text? | `heading()` plus a polite live region |
| 8 | Which tests prove it? | `formatResultCount` text via `paparazzi.context.resources`; cap boundary unit test; 1-result and 50+ goldens; androidTest assertions |
| 9 | Does it change existing Paparazzi goldens? | Yes, `..._success.png` changes |
| 10 | Commit tag and branch name? | `[FEAT]`; `feat_result_count_header` |
| 11 | What's out of scope? | GitHub `total_count`, per-type breakdown, iOS header, animation |
