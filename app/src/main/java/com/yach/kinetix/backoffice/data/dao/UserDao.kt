package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.backoffice.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: UUID): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username AND isActive = 1 LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY fullName ASC")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}
