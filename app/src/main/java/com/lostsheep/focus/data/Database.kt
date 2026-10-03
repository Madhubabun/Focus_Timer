package com.lostsheep.focus.data

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

/** A finished focus session (completed or ended early), kept for statistics and the Journey. */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val startedAt: Long,
    val endedAt: Long,
    val plannedMs: Long,
    val focusedMs: Long,
    /** [SessionOutcomeKind] name: COMPLETED or ABANDONED. */
    val outcome: String,
    val distractionsBlocked: Int,
) {
    val completed: Boolean get() = outcome == SessionOutcomeKind.COMPLETED.name
}

enum class SessionOutcomeKind { COMPLETED, ABANDONED }

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
)

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Database(
    entities = [FocusSessionEntity::class, BlockedAppEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LostSheepDatabase : RoomDatabase() {
    abstract fun sessions(): FocusSessionDao
    abstract fun blockedApps(): BlockedAppDao

    companion object {
        fun create(context: Context): LostSheepDatabase =
            Room.databaseBuilder(context, LostSheepDatabase::class.java, "lost_sheep.db").build()
    }
}
