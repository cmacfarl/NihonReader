package com.nihonreader.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.nihonreader.app.models.Folder;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.models.UserProgress;
import com.nihonreader.app.models.VocabularyItem;

import java.util.UUID;

/**
 * Main database class for the application
 */
@Database(entities = {Story.class, StoryContent.class, UserProgress.class, VocabularyItem.class, Folder.class}, 
          version = 2, 
          exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "nihon_reader_db";
    private static AppDatabase instance;
    
    public abstract StoryDao storyDao();
    public abstract StoryContentDao storyContentDao();
    public abstract UserProgressDao userProgressDao();
    public abstract VocabularyDao vocabularyDao();
    public abstract FolderDao folderDao();
    
    // Migration from version 1 to 2 (adding folders and ordering)
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the folders table
            database.execSQL("CREATE TABLE IF NOT EXISTS `folders` " +
                    "(`id` TEXT NOT NULL, `name` TEXT, `position` INTEGER NOT NULL, " +
                    "`isDefaultFolder` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            
            // Create a default folder
            String defaultFolderId = UUID.randomUUID().toString();
            database.execSQL("INSERT INTO folders (id, name, position, isDefaultFolder) " +
                    "VALUES ('" + defaultFolderId + "', 'All Stories', 0, 1)");
            
            // Add new columns to stories table
            database.execSQL("ALTER TABLE stories ADD COLUMN folderId TEXT");
            database.execSQL("ALTER TABLE stories ADD COLUMN position INTEGER NOT NULL DEFAULT 0");
            
            // Create an index on folderId to improve query performance
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_stories_folderId` ON `stories` (`folderId`)");
        }
    };
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}