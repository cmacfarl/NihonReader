package com.nihonreader.app.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a vocabulary item
 */
@Entity(tableName = "vocabulary")
public class VocabularyItem {
    
    @PrimaryKey
    @NonNull
    private String id;
    private String word;
    private String reading;
    private String meaning;
    private String notes;
    private String dictionaryForm; // Base form of a verb or adjective

    @Ignore
    public VocabularyItem(@NonNull String id, String word, String reading, String meaning) {
        this.id = id;
        this.word = word;
        this.reading = reading;
        this.meaning = meaning;
    }

    public VocabularyItem(@NonNull String id, String word, String reading, String meaning, String dictionaryForm) {
        this.id = id;
        this.word = word;
        this.reading = reading;
        this.meaning = meaning;
        this.dictionaryForm = dictionaryForm;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getDictionaryForm() {
        return dictionaryForm;
    }
    
    public void setDictionaryForm(String dictionaryForm) {
        this.dictionaryForm = dictionaryForm;
    }
}