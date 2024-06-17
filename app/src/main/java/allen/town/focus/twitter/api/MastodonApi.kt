/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package allen.town.focus.twitter.api

import allen.town.focus.twitter.api.requests.accounts.TimelineAccount
import allen.town.focus.twitter.api.requests.filter.Filter
import allen.town.focus.twitter.api.requests.filter.FilterKeyword
import allen.town.focus.twitter.api.requests.filter.FilterV1
import allen.town.focus.twitter.model.Relationship
import at.connyduck.calladapter.networkresult.NetworkResult
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * for documentation of the Mastodon REST API see https://docs.joinmastodon.org/api/
 */

@JvmSuppressWildcards
interface MastodonApi {

    companion object {
        const val ENDPOINT_AUTHORIZE = "oauth/authorize"
        const val DOMAIN_HEADER = "domain"
        const val PLACEHOLDER_DOMAIN = "dummy.placeholder"
    }

    @GET("api/v1/blocks")
    suspend fun blocks(
        @Query("max_id") maxId: String?
    ): Response<List<TimelineAccount>>

    @GET("api/v1/mutes")
    suspend fun mutes(
        @Query("max_id") maxId: String?
    ): Response<List<TimelineAccount>>

    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/mute")
    suspend fun muteAccount(
        @Path("id") accountId: String,
        @Field("notifications") notifications: Boolean? = null,
        @Field("duration") duration: Int? = null
    ): NetworkResult<Relationship>

    @POST("api/v1/accounts/{id}/unmute")
    suspend fun unmuteAccount(
        @Path("id") accountId: String
    ): NetworkResult<Relationship>

    @POST("api/v1/accounts/{id}/block")
    suspend fun blockAccount(
        @Path("id") accountId: String
    ): NetworkResult<Relationship>

    @POST("api/v1/accounts/{id}/unblock")
    suspend fun unblockAccount(
        @Path("id") accountId: String
    ): NetworkResult<Relationship>

    @GET("api/v1/accounts/relationships")
    suspend fun relationships(
        @Query("id[]") accountIds: List<String>
    ): NetworkResult<List<Relationship>>
    @FormUrlEncoded
    @POST("api/v2/filters")
    suspend fun createFilter(
        @Field("title") title: String,
        @Field("context[]") context: List<String>,
        @Field("filter_action") filterAction: String,
        @Field("expires_in") expiresInSeconds: Int?
    ): NetworkResult<Filter>

    @FormUrlEncoded
    @PUT("api/v2/filters/{id}")
    suspend fun updateFilter(
        @Path("id") id: String,
        @Field("title") title: String? = null,
        @Field("context[]") context: List<String>? = null,
        @Field("filter_action") filterAction: String? = null,
        @Field("expires_in") expiresInSeconds: Int? = null
    ): NetworkResult<Filter>

    @DELETE("api/v2/filters/{id}")
    suspend fun deleteFilter(
        @Path("id") id: String
    ): NetworkResult<ResponseBody>

    @FormUrlEncoded
    @POST("api/v2/filters/{filterId}/keywords")
    suspend fun addFilterKeyword(
        @Path("filterId") filterId: String,
        @Field("keyword") keyword: String,
        @Field("whole_word") wholeWord: Boolean
    ): NetworkResult<FilterKeyword>

    @FormUrlEncoded
    @PUT("api/v2/filters/keywords/{keywordId}")
    suspend fun updateFilterKeyword(
        @Path("keywordId") keywordId: String,
        @Field("keyword") keyword: String,
        @Field("whole_word") wholeWord: Boolean
    ): NetworkResult<FilterKeyword>

    @DELETE("api/v2/filters/keywords/{keywordId}")
    suspend fun deleteFilterKeyword(
        @Path("keywordId") keywordId: String
    ): NetworkResult<ResponseBody>

    @FormUrlEncoded
    @POST("api/v1/filters")
    suspend fun createFilterV1(
        @Field("phrase") phrase: String,
        @Field("context[]") context: List<String>,
        @Field("irreversible") irreversible: Boolean?,
        @Field("whole_word") wholeWord: Boolean?,
        @Field("expires_in") expiresInSeconds: Int?
    ): NetworkResult<FilterV1>

    @FormUrlEncoded
    @PUT("api/v1/filters/{id}")
    suspend fun updateFilterV1(
        @Path("id") id: String,
        @Field("phrase") phrase: String,
        @Field("context[]") context: List<String>,
        @Field("irreversible") irreversible: Boolean?,
        @Field("whole_word") wholeWord: Boolean?,
        @Field("expires_in") expiresInSeconds: Int?
    ): NetworkResult<FilterV1>

    @GET("api/v1/filters")
    suspend fun getFiltersV1(): NetworkResult<List<FilterV1>>

    @GET("api/v2/filters")
    suspend fun getFilters(): NetworkResult<List<Filter>>


    @DELETE("api/v1/filters/{id}")
    suspend fun deleteFilterV1(
        @Path("id") id: String
    ): NetworkResult<ResponseBody>
}


