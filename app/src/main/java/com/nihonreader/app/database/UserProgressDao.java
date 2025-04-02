package com.nihonreader.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nihonreader.app.models.UserProgress;

/**
 * Data Access Object for UserProgress entities
 */
@Dao
public interface UserProgressDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProgress userProgress);
    
    @Update
    void update(UserProgress userProgress);
    
    @Delete
    void delete(UserProgress userProgress);
    
    @Query("DELETE FROM user_progress WHERE storyId = :storyId")
    void deleteByStoryId(String storyId);
    
    @Query("SELECT * FROM user_progress WHERE storyId = :storyId")
    LiveData<UserProgress> getProgressForStory(String storyId);
    
    @Query("UPDATE user_progress SET lastAudioPosition = :position WHERE storyId = :storyId")
    void updateAudioPosition(String storyId, long position);
}