package com.nihonreader.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a folder that can contain stories
 */
@Entity(tableName = "folders")
public class Folder {
    
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private int position;
    private boolean isDefaultFolder;
    
    @Ignore
    private int storyCount; // Used for UI display, not stored in database
    
    public Folder(@NonNull String id, String name, int position, boolean isDefaultFolder) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.isDefaultFolder = isDefaultFolder;
        this.storyCount = 0;
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public boolean isDefaultFolder() {
        return isDefaultFolder;
    }
    
    public void setDefaultFolder(boolean defaultFolder) {
        isDefaultFolder = defaultFolder;
    }
    
    public int getStoryCount() {
        return storyCount;
    }
    
    public void setStoryCount(int storyCount) {
        this.storyCount = storyCount;
    }
} 