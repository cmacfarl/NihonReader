package com.nihonreader.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nihonreader.app.models.Story;

import java.util.List;

/**
 * Data Access Object for Story entities
 */
@Dao
public interface StoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Story story);
    
    @Update
    void update(Story story);
    
    @Delete
    void delete(Story story);
    
    @Query("DELETE FROM stories WHERE id = :storyId")
    void deleteById(String storyId);
    
    @Query("SELECT * FROM stories WHERE id = :id")
    LiveData<Story> getStoryById(String id);
    
    @Query("SELECT * FROM stories ORDER BY dateAdded DESC")
    LiveData<List<Story>> getAllStories();
    
    @Query("SELECT * FROM stories WHERE isCustom = 1 ORDER BY dateAdded DESC")
    LiveData<List<Story>> getCustomStories();
    
    @Query("UPDATE stories SET lastOpened = :timestamp WHERE id = :storyId")
    void updateLastOpened(String storyId, String timestamp);
}