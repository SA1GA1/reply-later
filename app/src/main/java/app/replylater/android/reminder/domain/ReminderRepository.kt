package app.replylater.android.reminder.domain

import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    suspend fun upsert(reminder: Reminder)

    suspend fun get(id: String): Reminder?

    fun observeAll(): Flow<List<Reminder>>

    suspend fun unfinished(): List<Reminder>

    suspend fun markAnswered(id: String, answeredAtEpochMillis: Long)

    suspend fun reschedule(
        id: String,
        remindAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    )

    suspend fun delete(id: String)

    suspend fun deleteAll()
}
