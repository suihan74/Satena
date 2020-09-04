package com.suihan74.utilities

typealias OnSuccess<ResultT> = (ResultT)->Unit

typealias OnError = (Throwable)->Unit

typealias OnFinally<ResultT> = (OnFinallyArgument<ResultT>)->Unit

class OnFinallyArgument<T>(
    val result: T?,
    val exception: Throwable?
)
