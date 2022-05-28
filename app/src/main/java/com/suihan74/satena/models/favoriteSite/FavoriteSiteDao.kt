package com.suihan74.satena.models.favoriteSite

import androidx.room.*
import com.suihan74.satena.models.browser.FaviconInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteSite: FavoriteSite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favoriteSites: List<FavoriteSite>)

    // --- //

    @Update
    suspend fun update(vararg favoriteSites: FavoriteSite)

    @Update
    suspend fun update(favoriteSites: List<FavoriteSite>)

    // --- //

    @Transaction
    @Query("""select * from favorite_site""")
    suspend fun allFavoriteSites() : List<FavoriteSiteAndFavicon>

    @Transaction
    @Query("""select * from favorite_site""")
    fun allFavoriteSitesFlow() : Flow<List<FavoriteSiteAndFavicon>>

    // --- //

    @Query("select exists (select * from favorite_site where url = :url)")
    suspend fun exists(url: String) : Boolean

    @Transaction
    @Query("select * from favorite_site where url = :url")
    suspend fun findFavoriteSite(url: String) : FavoriteSiteAndFavicon?

    @Query("select * from favorite_site where faviconInfoId = 0")
    suspend fun findItemsFaviconInfoNotSet() : List<FavoriteSite>

    // --- //

    @Delete
    suspend fun delete(favoriteSite: FavoriteSite)

    @Query("delete from favorite_site where url = :url")
    suspend fun delete(url: String)

    // ------ //

    @Query("select * from browser_favicon_info where domain = :domain")
    suspend fun findFaviconInfo(domain: String) : FaviconInfo?

}
