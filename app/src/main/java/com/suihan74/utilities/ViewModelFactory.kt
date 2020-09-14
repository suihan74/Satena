package com.suihan74.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass

abstract class ViewModelFactory<ViewModelT : ViewModel>(
    private val creator: () -> ViewModelT,
    private val kClass: KClass<ViewModelT>
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return creator.invoke() as T
    }

    /** ViewModelを作成・取得する */
    fun provide(owner: ViewModelStoreOwner, key: String? = null) : ViewModelT =
        if (key == null) ViewModelProvider(owner, this)[kClass.java]
        else ViewModelProvider(owner, this)[key, kClass.java]
}

/**
 * ViewModel作成時に用いるNewInstanceFactoryを生成する
 */
inline fun <reified ViewModelT : ViewModel> createViewModelFactory(
    noinline creator: ()->ViewModelT
) = object : ViewModelFactory<ViewModelT>(creator, ViewModelT::class) {}

/**
 * ViewModelを作成・取得する
 */
inline fun <reified ViewModelT : ViewModel> provideViewModel(
    owner: ViewModelStoreOwner,
    noinline creator: ()->ViewModelT
) = createViewModelFactory(creator).provide(owner)

/**
 * ViewModelを作成・取得する
 */
inline fun <reified ViewModelT : ViewModel> provideViewModel(
    owner: ViewModelStoreOwner,
    key: String?,
    noinline creator: ()->ViewModelT
) = createViewModelFactory(creator).provide(owner, key)
