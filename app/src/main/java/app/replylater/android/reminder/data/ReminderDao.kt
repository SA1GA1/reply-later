package app.replylater.android.reminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY remindAtEpochMillis ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query(
        "SELECT * FROM reminders " +
            "WHERE answeredAtEpochMillis IS NULL " +
            "ORDER BY remindAtEpochMillis ASC",
    )
    suspend fun unfinished(): List<ReminderEntity>

    @Query(
        "UPDATE reminders SET answeredAtEpochMillis = :answeredAtEpochMillis, " +
            "updatedAtEpochMillis = :updatedAtEpochMillis WHERE id = :id",
    )
    suspend fun markAnswered(
        id: String,
        answeredAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    )

    @Query(
        "UPDATE reminders SET remindAtEpochMillis = :remindAtEpochMillis, " +
            "answeredAtEpochMillis = NULL, updatedAtEpochMillis = :updatedAtEpochMillis " +
            "WHERE id = :id",
    )
    suspend fun reschedule(
        id: String,
        remindAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    )

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
