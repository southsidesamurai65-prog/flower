package com.example.flowerid.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val thumbPath: String,
    val title: String,
    val resultJson: String,
    val modelUsed: String,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): HistoryItem?

    @Insert
    suspend fun insert(item: HistoryItem): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [HistoryItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
