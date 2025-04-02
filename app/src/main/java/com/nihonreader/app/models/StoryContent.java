package com.nihonreader.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.nihonreader.app.database.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the content of a story
 */
@Entity(tableName = "story_contents")
@TypeConverters(Converters.class)
public class StoryContent {
    
    @PrimaryKey
    @NonNull
    private String id;
    private String storyId;
    private String text;
    private String audioUri;
    private List<AudioSegment> segments;
    private List<VocabularyItem> vocabulary;

    public StoryContent(@NonNull String id, String storyId, String text, String audioUri) {
        this.id = id;
        this.storyId = storyId;
        this.text = text;
        this.audioUri = audioUri;
        this.segments = new ArrayList<>();
        this.vocabulary = new ArrayList<>();
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAudioUri() {
        return audioUri;
    }

    public void setAudioUri(String audioUri) {
        this.audioUri = audioUri;
    }

    public List<AudioSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<AudioSegment> segments) {
        this.segments = segments;
    }

    public List<VocabularyItem> getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(List<VocabularyItem> vocabulary) {
        this.vocabulary = vocabulary;
    }
}