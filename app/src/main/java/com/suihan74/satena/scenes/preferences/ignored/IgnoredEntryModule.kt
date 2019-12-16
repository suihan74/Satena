package com.suihan74.satena.scenes.preferences.ignored

import com.suihan74.satena.SatenaApplication
import dagger.Module
import dagger.Provides

@Module
class IgnoredEntryModule(private val app: SatenaApplication) {
    @Provides
    fun provideIgnoredEntryDao() =
        app.ignoredEntryDao

    @Provides
    fun provideIgnoredEntryRepository() =
        IgnoredEntryRepository(provideIgnoredEntryDao())
}
