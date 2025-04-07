package com.nihonreader.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nihonreader.app.models.StoryContent;

/**
 * Data Access Object for StoryContent entities
 */
@Dao
public interface StoryContentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StoryContent storyContent);
    
    @Update
    void update(StoryContent storyContent);
    
    @Delete
    void delete(StoryContent storyContent);
    
    @Query("DELETE FROM story_contents WHERE storyId = :storyId")
    void deleteByStoryId(String storyId);
    
    @Query("SELECT * FROM story_contents WHERE storyId = :storyId")
    LiveData<StoryContent> getContentForStory(String storyId);
    
    @Query("SELECT * FROM story_contents WHERE storyId = :storyId")
    StoryContent getContentForStorySync(String storyId);
}