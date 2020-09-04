@file:Suppress("unused")

package com.suihan74.utilities

typealias OnSuccess<ResultT> = (ResultT)->Unit

typealias OnError = (Throwable)->Unit

typealias OnFinally = ()->Unit
