package io.github.aman99dex.relaymesh

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.SecureRandom

@Entity
data class Contact(
    @PrimaryKey val publicKey: String, // base64url Ed25519 key
    val name: String,
    val addedAt: Long,
)

@Dao
interface ContactDao {
    @Query("SELECT * FROM Contact ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: Contact)
}

/** The whole database file is AES-256 encrypted by SQLCipher; its random key is sealed by [KeyVault]. */
@Database(entities = [Contact::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun contacts(): ContactDao

    companion object {
        fun open(context: Context): AppDb {
            System.loadLibrary("sqlcipher")
            val key = KeyVault.secret(File(context.filesDir, "db.key")) { ByteArray(32).also(SecureRandom()::nextBytes) }
            return Room.databaseBuilder(context, AppDb::class.java, "relaymesh.db")
                .openHelperFactory(SupportOpenHelperFactory(key))
                .build()
        }
    }
}
