package com.suihan74.satena

import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryModule
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryRepository
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntryViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    IgnoredEntryModule::class
])
interface AppComponent {
    fun inject(viewModel: IgnoredEntryViewModel)
    fun inject(repository: IgnoredEntryRepository)
}
