package pl.gi.codingchallenge.data.remote

import pl.gi.codingchallenge.data.remote.dto.GitHubSearchResponse
import pl.gi.codingchallenge.data.remote.dto.RepositoryDto
import pl.gi.codingchallenge.data.remote.dto.UserDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GitHubApi {

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 50,
    ): GitHubSearchResponse<RepositoryDto>

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 50,
    ): GitHubSearchResponse<UserDto>
}
