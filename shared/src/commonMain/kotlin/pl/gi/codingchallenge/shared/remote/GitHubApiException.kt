package pl.gi.codingchallenge.shared.remote

// GitHubApi's non-2xx responses get mapped to these instead of being deserialized as if they
// were a successful GitHubSearchResponse - see docs/architecture/github-api-error-handling.md
// for the bug this fixes (a rate-limit response used to surface as a JSON parse failure).
sealed class GitHubApiException(message: String) : Exception(message) {

    class RateLimited(statusCode: Int) :
        GitHubApiException("GitHub API rate limit exceeded (HTTP $statusCode) - try again shortly")

    class Unauthorized : GitHubApiException("GitHub API request was unauthorized")

    class NotFound : GitHubApiException("GitHub API resource not found")

    class ServerError(statusCode: Int) :
        GitHubApiException(
            "GitHub API returned a server error (HTTP $statusCode) - try again shortly"
        )

    class Unknown(statusCode: Int, rawBody: String) :
        GitHubApiException("GitHub API returned HTTP $statusCode: $rawBody")
}
