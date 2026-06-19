package it.unisannio.soscity.soscity_app.util

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val request =
            chain.request()
                .newBuilder()

        val username =
            SessionManager.username

        val password =
            SessionManager.password

        if (
            SessionManager.isLoggedIn &&
            username != null &&
            password != null
        ) {

            val credential =
                Credentials.basic(
                    username,
                    password
                )

            request.header(
                "Authorization",
                credential
            )
        }

        request.header(
            "Content-Type",
            "application/json"
        )

        return chain.proceed(
            request.build()
        )
    }
}