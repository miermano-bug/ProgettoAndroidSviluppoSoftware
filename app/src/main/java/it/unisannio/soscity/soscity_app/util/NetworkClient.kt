package it.unisannio.soscity.soscity_app.util

import it.unisannio.soscity.soscity_app.data.remote.ApiService
import it.unisannio.soscity.soscity_app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configura il client Retrofit per le chiamate API.
 * Legge BASE_URL da BuildConfig (impostato in app/build.gradle.kts).
 */
object NetworkClient {

    private const val CONNECTION_TIMEOUT = 30L  // secondi
    private const val READ_TIMEOUT = 30L

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)

        // Aggiunge l'interceptor per il logging in debug
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        // Aggiunge l'interceptor per l'header Authorization Bearer
        builder.addInterceptor(AuthInterceptor())

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Crea un'istanza di ApiService.
     * Utilizzata da RealRepository per fare le chiamate HTTP.
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    /**
     * Metodo di convenienza per ottenere direttamente ApiService.
     */
    val apiService: ApiService by lazy {
        createService(ApiService::class.java)
    }
}