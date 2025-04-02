package com.nihonreader.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a story in the application
 */
@Entity(tableName = "stories")
public class Story {
    
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String author;
    private String coverImagePath;
    private String description;
    private boolean isCustom;
    private String dateAdded;
    private String lastOpened;

    public Story(@NonNull String id, String title, String author, String description, 
                 boolean isCustom, String dateAdded) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.isCustom = isCustom;
        this.dateAdded = dateAdded;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getLastOpened() {
        return lastOpened;
    }

    public void setLastOpened(String lastOpened) {
        this.lastOpened = lastOpened;
    }
}