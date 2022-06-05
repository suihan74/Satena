package com.suihan74.satena.models

import android.util.Log
import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.runner.RunWith

@Suppress("NonAsciiCharacters", "SpellCheckingInspection")
@RunWith(AndroidJUnit4ClassRunner::class)
class BrowserDaoTest {
    // Context of the app under test.
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val db =
        Room.inMemoryDatabaseBuilder(
            appContext, AppDatabase::class.java).build()
    private val dao = db.browserDao()

    @Before
    fun データ初期化() {
        runBlocking {
            dao.run {
                clearHistory()
            }
            Log.i("initialized", "データ初期化")
        }
    }

    /*
    @Test
    fun クエリを使った履歴検索() = runBlocking {
        runBlocking {
            dao.run {
                val now = LocalDateTime.now()
                insertHistory(
                    History(
                        HistoryLog(visitedAt = now),
                        HistoryPage(
                            url = Uri.decode("https://www.google.com"),
                            title = "google",
                            faviconUrl = "https://localhost/",
                            lastVisited = now
                        )
                    )
                )
                insertHistory(
                    History(
                        HistoryLog(visitedAt = now),
                        HistoryPage(
                            url = Uri.decode("https://www.yahoo.co.jp"),
                            title = "yahoo",
                            faviconUrl = "https://localhost/",
                            lastVisited = now
                        )
                    )
                )
                insertHistory(
                    History(
                        HistoryLog(visitedAt = now),
                        HistoryPage(
                            url = Uri.decode("https://www.doogle.com"),
                            title = "doodle",
                            faviconUrl = "https://localhost/",
                            lastVisited = now
                        )
                    )
                )
            }
        }

        assertEquals(3, dao.findHistory().size)
        assertEquals(2, dao.findHistory("oogle").size)
        assertEquals(1, dao.findHistory("goo").size)
        assertEquals(1, dao.findHistory("aho").size)
    }
     */
}
