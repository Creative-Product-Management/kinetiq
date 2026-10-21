package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Habit
import com.example.data.model.RoutineStackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY orderIndex ASC, id ASC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY orderIndex ASC, id ASC")
    fun getArchivedHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitByIdFlow(id: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE categoryId = :categoryId AND isArchived = 0 ORDER BY orderIndex ASC")
    fun getHabitsByCategory(categoryId: Long): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM routine_stacks ORDER BY id ASC")
    fun getAllRoutineStacks(): Flow<List<RoutineStackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStack(stack: RoutineStackEntity): Long

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabitsList(): List<Habit>

    @Query("SELECT * FROM routine_stacks ORDER BY id ASC")
    suspend fun getAllStacksList(): List<RoutineStackEntity>

    @Query("DELETE FROM routine_stacks")
    suspend fun clearAllStacks()

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitsCount(): Int
}
