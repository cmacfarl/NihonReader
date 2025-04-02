package com.nihonreader.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.models.UserProgress;
import com.nihonreader.app.models.VocabularyItem;

/**
 * Main database class for the application
 */
@Database(entities = {Story.class, StoryContent.class, UserProgress.class, VocabularyItem.class}, 
          version = 1, 
          exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "nihon_reader_db";
    private static AppDatabase instance;
    
    public abstract StoryDao storyDao();
    public abstract StoryContentDao storyContentDao();
    public abstract UserProgressDao userProgressDao();
    public abstract VocabularyDao vocabularyDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}