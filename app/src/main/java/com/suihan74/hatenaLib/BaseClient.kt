package com.suihan74.hatenaLib

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.lang.reflect.Type
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.SocketTimeoutException

open class BaseClient {
    protected val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    protected val clientWithoutCookie = OkHttpClient.Builder().build()

    protected val client =
        clientWithoutCookie.newBuilder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .build()

    // キャッシュ回避
    protected fun cacheAvoidance() : String = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toString()

    protected fun get(url: String, withCookie: Boolean = true) : Response {
        val request = Request.Builder()
            .url(url)
            .build()

        val httpClient =
            if (withCookie) client
            else clientWithoutCookie

        val call = httpClient.newCall(request)
        return call.execute()
    }

    protected fun post(url: String, params: Map<String, String>? = null) : Response {
        val paramsBuilder = FormBody.Builder()
        if (!params.isNullOrEmpty()) {
            for (param in params) {
                paramsBuilder.addEncoded(param.key, param.value)
            }
        }
        return post(url, paramsBuilder.build())
    }

    private fun post(url: String, formBody: FormBody) : Response {
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        val call = client.newCall(request)
        return call.execute()
    }

    private fun send(request: Request) : Response {
        val call = client.newCall(request)
        return call.execute()
    }

    protected fun <T> send(type: Type, request: Request, gsonBuilder: GsonBuilder) : T =
        try {
            send(request).use { response ->
                when {
                    response.isSuccessful -> {
                        val gson = gsonBuilder
                            .serializeNulls()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .create()

                        val json = response.body!!.use { it.string() }
                        gson.fromJson<T>(json, type)
                    }

                    response.code == 404 -> throw NotFoundException()

                    else -> throw RuntimeException("connection failed: error code: ${response.code}")
                }
            }
        }
        catch (e: SocketTimeoutException) {
            client.connectionPool.evictAll()
            throw TimeoutException(e)
        }

    protected inline fun <reified T> send(request: Request, gsonBuilder: GsonBuilder) : T =
        send(T::class.java, request, gsonBuilder)

    protected inline fun <reified T> send(request: Request) : T
            = send(T::class.java, request, GsonBuilder())

    private fun <T> responseTo(type: Type, response: Response, gsonBuilder: GsonBuilder) : T =
        when (val code = response.code) {
            200 -> {
                val json = response.body!!.use { it.string() }
                gsonBuilder
                    .serializeNulls()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(Boolean::class.java, BooleanDeserializer())
                    .create()
                    .fromJson(json, type)
            }

            403 -> throw ForbiddenException()
            404 -> throw NotFoundException()
            else -> throw ConnectionFailureException("connection error: $code")
        }

    private fun <T> responseTo(
        type: Type,
        response: Response,
        dateFormat: String? = null
    ) : T = responseTo(
        type,
        response,
        GsonBuilder().registerTypeAdapter(
            LocalDateTime::class.java,
            TimestampDeserializer(dateFormat)
        )
    )

    /**
     * @throws ConnectionFailureException
     * @throws NotFoundException
     * @throws ForbiddenException
     * @throws TimeoutException
     */
    protected fun <T> getJson(
        type: Type,
        url: String,
        dateFormat: String? = null,
        withCookie: Boolean = true
    ) : T {
        try {
            get(url, withCookie).use { response ->
                return responseTo(type, response, dateFormat)
            }
        }
        catch (e: SocketTimeoutException) {
            client.connectionPool.evictAll()
            throw TimeoutException("${e.message ?: ""}: $url", e)
        }
        catch (e: NotFoundException) {
            throw NotFoundException("404 not found: $url", e)
        }
        catch (e: ForbiddenException) {
            throw e
        }
        catch (e: Throwable) {
            throw ConnectionFailureException("${e.message ?: ""}: $url", e)
        }
    }

    /**
     * @throws ConnectionFailureException
     * @throws NotFoundException
     * @throws ForbiddenException
     * @throws TimeoutException
     */
    protected inline fun <reified T> getJson(
        url: String,
        dateFormat: String? = null,
        withCookie: Boolean = true
    ) = getJson<T>(T::class.java, url, dateFormat, withCookie)
}
