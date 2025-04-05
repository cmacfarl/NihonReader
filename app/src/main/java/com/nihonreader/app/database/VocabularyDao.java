package com.nihonreader.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nihonreader.app.models.VocabularyItem;

import java.util.List;

/**
 * Data Access Object for VocabularyItem entities
 */
@Dao
public interface VocabularyDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VocabularyItem vocabularyItem);
    
    @Update
    void update(VocabularyItem vocabularyItem);
    
    @Delete
    void delete(VocabularyItem vocabularyItem);
    
    @Query("SELECT * FROM vocabulary WHERE id = :id")
    LiveData<VocabularyItem> getVocabularyById(String id);
    
    @Query("SELECT * FROM vocabulary ORDER BY word ASC")
    LiveData<List<VocabularyItem>> getAllVocabulary();
    
    @Query("SELECT * FROM vocabulary WHERE word = :word LIMIT 1")
    VocabularyItem getVocabularyByWord(String word);
}