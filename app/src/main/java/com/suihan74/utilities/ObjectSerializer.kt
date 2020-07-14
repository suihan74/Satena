package com.suihan74.utilities

import android.util.Base64
import java.io.*

class ObjectSerializer<T : Serializable>(private val klass: Class<T>) {
    companion object {
        inline operator fun <reified T : Serializable> invoke() = ObjectSerializer(T::class.java)
    }

    fun serialize(obj: T) : String {
        val ostream = ByteArrayOutputStream()
        val oos = ObjectOutputStream(ostream)

        try {
            oos.writeObject(obj)
            oos.flush()
            return Base64.encodeToString(ostream.toByteArray(), Base64.NO_WRAP)
        }
        catch (e: Throwable) {
            throw ClassCastException("failed to serialize from ${klass.name}}")
        }
        finally {
            oos.close()
        }
    }

    fun deserialize(str: String) : T {
        val istream = ByteArrayInputStream(Base64.decode(str, Base64.NO_WRAP))
        val ois = ObjectInputStream(istream)

        try {
            val obj = ois.readObject()
            return klass.cast(obj) ?: throw Exception()
        }
        catch (e: Throwable) {
            throw ClassCastException("failed to deserialize to ${klass.name}}")
        }
        finally {
            ois.close()
        }
    }
}
