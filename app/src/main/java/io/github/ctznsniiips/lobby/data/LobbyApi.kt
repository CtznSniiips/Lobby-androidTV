package io.github.ctznsniiips.lobby.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * The plugin's TV API. Routes are relative to the server base URL (which already includes the "/emby"
 * prefix when an Emby server only answers under it), so every path here starts with "Lobby/".
 */
interface LobbyApi {
    @POST("Lobby/Pair")
    suspend fun startPairing(@Body body: StartPairingBody): StartPairingResponse

    @GET("Lobby/Pair/Status")
    suspend fun pairingStatus(
        @Query("PairingId") pairingId: String,
        @Query("Secret") secret: String,
    ): PairingStatusResponse

    @GET("Lobby/Ping")
    suspend fun ping(): PingResponse

    @GET("Lobby/Home")
    suspend fun home(): HomeResponse

    @GET("Lobby/Search")
    suspend fun search(@Query("query") query: String): HomeResponse

    @POST("Lobby/Play")
    suspend fun play(@Body body: PlayBody): DispatchResult

    /** Seerr details for a title. 404 when Seerr isn't set up on the server. */
    @GET("Lobby/Details")
    suspend fun details(
        @Query("mediaType") mediaType: String,
        @Query("tmdbId") tmdbId: Int,
    ): TitleDetailsResponse

    @GET("Lobby/Requesters")
    suspend fun requesters(): RequestersResponse

    @POST("Lobby/Request")
    suspend fun request(@Body body: SubmitRequestBody): SubmitRequestResponse
}

object LobbyJson {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }
}

object LobbyApiFactory {
    /** Shared client; the token (if any) is attached per request so one client serves pairing and normal use. */
    fun create(server: ServerAddress, token: String?): LobbyApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .apply { if (!token.isNullOrBlank()) header("Authorization", "Lobby $token") }
                    .build()
                chain.proceed(request)
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(server.baseUrl)
            .client(client)
            .addConverterFactory(LobbyJson.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LobbyApi::class.java)
    }
}
