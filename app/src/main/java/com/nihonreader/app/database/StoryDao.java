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
 * Data Access Object for Story entity
 */
@Dao
public interface StoryDao {
    
    /**
     * Get all stories ordered by title
     */
    @Query("SELECT * FROM stories ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<Story>> getAllStories();
    
    /**
     * Get stories in a specific folder, ordered by title
     */
    @Query("SELECT * FROM stories WHERE folderId = :folderId ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<Story>> getStoriesInFolder(String folderId);
    
    /**
     * Get stories that are not in any folder
     */
    @Query("SELECT * FROM stories WHERE folderId IS NULL OR folderId = '' ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<Story>> getStoriesWithoutFolder();
    
    /**
     * Get a story by ID
     */
    @Query("SELECT * FROM stories WHERE id = :id")
    LiveData<Story> getStoryById(String id);
    
    /**
     * Insert a new story
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Story story);
    
    /**
     * Update an existing story
     */
    @Update
    void update(Story story);
    
    /**
     * Delete a story
     */
    @Delete
    void delete(Story story);
    
    /**
     * Update story last opened timestamp
     */
    @Query("UPDATE stories SET lastOpened = :timestamp WHERE id = :id")
    void updateLastOpened(String id, String timestamp);
    
    /**
     * Move a story to a folder
     */
    @Query("UPDATE stories SET folderId = :folderId WHERE id = :storyId")
    void moveStoryToFolder(String storyId, String folderId);
    
    /**
     * Move all stories from one folder to another
     */
    @Query("UPDATE stories SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    void moveAllStories(String oldFolderId, String newFolderId);
    
    /**
     * Get all stories (non-LiveData version for export)
     */
    @Query("SELECT * FROM stories ORDER BY title COLLATE NOCASE ASC")
    List<Story> getAllStoriesSync();
    
    /**
     * Get total story count
     */
    @Query("SELECT COUNT(*) FROM stories")
    int getStoryCount();
}