package pl.gi.codingchallenge.shared.domain.repository

import pl.gi.codingchallenge.shared.domain.model.MAX_RESULTS
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

interface GitHubSearchRepository {
    // @Throws is load-bearing on iOS, not documentation (step 8's CatFactApi finding):
    // without it, an uncaught exception here doesn't reach Swift's catch at all - it
    // aborts the whole process instead. This is the one GitHubSearchRepository method
    // Swift calls directly (KoinHelper.getGitHubSearchRepository()), so it needs the
    // annotation even though :app's Hilt-resolved callers never see it.
    @Throws(Exception::class)
    suspend fun search(query: String, perTypeLimit: Int = MAX_RESULTS): List<SearchResultItem>
}
