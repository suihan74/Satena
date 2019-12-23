package com.suihan74.HatenaLib

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.lang.reflect.Type
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

open class BaseClient {
    protected val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    protected val clientWithoutCookie =
        OkHttpClient()

    protected val client : OkHttpClient =
        OkHttpClient().newBuilder()
        .readTimeout(3, TimeUnit.MINUTES)
        .writeTimeout(3, TimeUnit.MINUTES)
        .connectTimeout(3, TimeUnit.MINUTES)
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

    protected fun post(url: String, formBody: FormBody) : Response {
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        val call = client.newCall(request)
        return call.execute()
    }

    protected fun send(request: Request) : Response {
        val call = client.newCall(request)
        return call.execute()
    }

    protected fun <T> send(type: Type, request: Request, gsonBuilder: GsonBuilder) : T {
        val response = send(request)
        if (response.isSuccessful) {
            val gson = gsonBuilder
                .serializeNulls()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
            val json = response.body!!.string()
            val result = gson.fromJson<T>(json, type)

            response.close()
            return result
        }

        response.close()
        throw RuntimeException("connection error")
    }

    protected fun <T> send(type: Type, request: Request) : T =
        send(type, request, GsonBuilder())

    protected inline fun <reified T> send(request: Request, gsonBuilder: GsonBuilder) : T =
        send(T::class.java, request, gsonBuilder)

    protected inline fun <reified T> send(request: Request) : T
            = send(T::class.java, request, GsonBuilder())

    private fun <T> responseTo(type: Type, response: Response, gsonBuilder: GsonBuilder) : T {
        return if (response.isSuccessful) {
            val json = response.body!!.charStream()

            val result = gsonBuilder
                .serializeNulls()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Boolean::class.java, BooleanDeserializer())
                .create()
                .fromJson<T>(json, type)

            response.close()
            result
        }
        else {
            val code = response.code
            response.close()
            throw RuntimeException("connection error: $code")
        }
    }

    private fun <T> responseTo(type: Type, response: Response, dateFormat: String? = null) : T =
        responseTo(type, response, GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, TimestampDeserializer(dateFormat)))

    protected fun <T> getJson(type: Type, url: String, gsonBuilder: GsonBuilder) : T {
        try {
            val response = get(url)
            return responseTo(type, response, gsonBuilder)
        }
        catch (e: Exception) {
            throw RuntimeException("${e.message}: $url")
        }
    }

    protected fun <T> getJson(type: Type, url: String, dateFormat: String? = null, withCookie: Boolean = true) : T {
        try {
            val response = get(url, withCookie)
            return responseTo(type, response, dateFormat)
        }
        catch (e: Exception) {
            throw RuntimeException("${e.message}: $url")
        }
    }

    protected inline fun <reified T> getJson(url: String, gsonBuilder: GsonBuilder) =
        getJson<T>(T::class.java, url, gsonBuilder)

    protected inline fun <reified T> getJson(url: String, dateFormat: String? = null, withCookie: Boolean = true) =
        getJson<T>(T::class.java, url, dateFormat, withCookie)
}
