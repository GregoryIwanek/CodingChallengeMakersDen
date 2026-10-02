# `/new-feature` Examples

A guide for you, not for Claude: `SKILL.md` doesn't link here, so Claude never loads this file
and the examples can't sway its answers.

## Arguments

Everything after `/new-feature` becomes `$ARGUMENTS`:

- `/new-feature result-count header above search results`
- `/new-feature result-count header, :app only, spec in docs/ai/harness-test-feature.md`
- `/new-feature show the repo's language as a chip in each result row`

## Example answers

From the result-count header test feature ([harness-test-feature.md](../../../docs/ai/harness-test-feature.md)).

| # | Question | Example answer |
| --- | --- | --- |
| 1 | What exactly does the user see, and in which UI states? | A "12 results" header above the list, in the `Success` state only |
| 2 | What are the edge cases? | `Empty` is its own state, so `Success` always has ≥ 1 item; 1 → "1 result" |
| 3 | Which data does it show, and where does it come from? | Items shown in the list, not GitHub's `total_count` |
| 4 | Which module: `:app` only, or `:shared`? | `:app` only |
| 5 | Which files and existing patterns should it follow? | `SuggestionPanel.kt`; `formatStars` style; `dimRes` + `dimens.xml`; `AutocompleteTestTags.kt` |
| 6 | New strings? | A new `<plurals name="result_count">` in `strings.xml` |
| 7 | Accessibility: anything beyond plain, in-order text? | No, a plain `Text` read in order is enough |
| 8 | Which tests prove it? | Unit test for `formatResultCount`; re-recorded Paparazzi success golden |
| 9 | Does it change existing Paparazzi goldens? | Yes, `..._success.png` changes |
| 10 | Commit tag and branch name? | `[FEAT]`; `feat_result_count_header` |
| 11 | What's out of scope? | No ViewModel change, no `:shared`, no animation |
