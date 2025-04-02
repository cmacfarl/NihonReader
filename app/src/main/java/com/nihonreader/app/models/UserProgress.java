package com.nihonreader.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.nihonreader.app.database.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing user progress for a story
 */
@Entity(tableName = "user_progress")
@TypeConverters(Converters.class)
public class UserProgress {
    
    @PrimaryKey
    @NonNull
    private String storyId;
    private long lastPosition;
    private long lastAudioPosition;
    private List<String> completedSegments;
    private List<String> savedVocabulary;

    public UserProgress(@NonNull String storyId) {
        this.storyId = storyId;
        this.lastPosition = 0;
        this.lastAudioPosition = 0;
        this.completedSegments = new ArrayList<>();
        this.savedVocabulary = new ArrayList<>();
    }

    @NonNull
    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(@NonNull String storyId) {
        this.storyId = storyId;
    }

    public long getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(long lastPosition) {
        this.lastPosition = lastPosition;
    }

    public long getLastAudioPosition() {
        return lastAudioPosition;
    }

    public void setLastAudioPosition(long lastAudioPosition) {
        this.lastAudioPosition = lastAudioPosition;
    }

    public List<String> getCompletedSegments() {
        return completedSegments;
    }

    public void setCompletedSegments(List<String> completedSegments) {
        this.completedSegments = completedSegments;
    }

    public List<String> getSavedVocabulary() {
        return savedVocabulary;
    }

    public void setSavedVocabulary(List<String> savedVocabulary) {
        this.savedVocabulary = savedVocabulary;
    }
}