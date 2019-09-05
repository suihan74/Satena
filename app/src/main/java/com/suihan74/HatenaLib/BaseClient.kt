package com.suihan74.HatenaLib

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.lang.RuntimeException
import java.lang.reflect.Type
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

open class BaseClient {
    protected val mCookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    protected val mClient = OkHttpClient()
        .newBuilder()
        .connectTimeout(3, TimeUnit.MINUTES)
        .cookieJar(JavaNetCookieJar(mCookieManager))
        .build()


    // キャッシュ回避
    protected fun cacheAvoidance() : String = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toString()

    protected fun get(url: String) : Response {
        val request = Request.Builder()
            .url(url)
            .build()
        val call = mClient.newCall(request)
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
        val call = mClient.newCall(request)
        return call.execute()
    }

    protected fun send(request: Request) : Response {
        val call = mClient.newCall(request)
        return call.execute()
    }

    protected fun <T> send(type: Type, request: Request, gsonBuilder: GsonBuilder) : T {
        val response = send(request)
        if (response.isSuccessful) {
            val gson = gsonBuilder
                .serializeNulls()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
            val json = response.body()!!.string()
            val result = gson.fromJson<T>(json, type)

            response.close()
            return result
        }

        response.close()
        throw RuntimeException("connection error")
    }

    protected fun <T> send(type: Type, request: Request) : T = send(type, request, GsonBuilder())

    protected fun <T> responseTo(type: Type, response: Response, gsonBuilder: GsonBuilder) : T {
        return if (response.isSuccessful) {
            val json = response.body()!!.charStream()

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
            response.close()
            throw RuntimeException("connection error")
        }
    }

    protected fun <T> responseTo(type: Type, response: Response, dateFormat: String? = null) : T =
        responseTo<T>(type, response, GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, TimestampDeserializer(dateFormat)))

    protected fun <T> getJson(type: Type, url: String, gsonBuilder: GsonBuilder) : T {
        val response = get(url)
        return responseTo(type, response, gsonBuilder)
    }

    protected fun <T> getJson(type: Type, url: String, dateFormat: String? = null) : T {
        val response = get(url)
        return responseTo(type, response, dateFormat)
    }
}
