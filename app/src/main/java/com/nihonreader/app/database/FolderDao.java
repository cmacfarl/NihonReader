package com.nihonreader.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nihonreader.app.models.Folder;
import com.nihonreader.app.models.Story;

import java.util.List;

/**
 * Data Access Object for the Folder entity
 */
@Dao
public interface FolderDao {
    
    /**
     * Get all folders ordered by position
     */
    @Query("SELECT * FROM folders ORDER BY position ASC")
    LiveData<List<Folder>> getAllFolders();
    
    /**
     * Get a folder by its ID
     */
    @Query("SELECT * FROM folders WHERE id = :id")
    LiveData<Folder> getFolderById(String id);
    
    /**
     * Get the default folder
     */
    @Query("SELECT * FROM folders WHERE isDefaultFolder = 1 LIMIT 1")
    LiveData<Folder> getDefaultFolder();
    
    /**
     * Insert a new folder
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Folder folder);
    
    /**
     * Update an existing folder
     */
    @Update
    void update(Folder folder);
    
    /**
     * Delete a folder
     */
    @Delete
    void delete(Folder folder);
    
    /**
     * Get the highest position value
     */
    @Query("SELECT MAX(position) FROM folders")
    int getMaxPosition();
    
    /**
     * Shift folder positions to make room for a new folder
     */
    @Query("UPDATE folders SET position = position + 1 WHERE position >= :fromPosition")
    void shiftFolderPositionsForInsert(int fromPosition);
    
    /**
     * Shift folder positions after deletion
     */
    @Query("UPDATE folders SET position = position - 1 WHERE position > :deletedPosition")
    void shiftFolderPositionsAfterDelete(int deletedPosition);
    
    /**
     * Reorder folders based on their position values
     */
    @Update
    void reorderFolders(List<Folder> folders);
} 