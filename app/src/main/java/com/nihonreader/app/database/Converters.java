package com.nihonreader.app.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.VocabularyItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Type converters for Room database
 */
public class Converters {
    
    private static final Gson gson = new Gson();
    
    @TypeConverter
    public static String fromAudioSegmentList(List<AudioSegment> segments) {
        if (segments == null) {
            return null;
        }
        return gson.toJson(segments);
    }
    
    @TypeConverter
    public static List<AudioSegment> toAudioSegmentList(String segmentsJson) {
        if (segmentsJson == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<AudioSegment>>(){}.getType();
        return gson.fromJson(segmentsJson, listType);
    }
    
    @TypeConverter
    public static String fromVocabularyItemList(List<VocabularyItem> vocabulary) {
        if (vocabulary == null) {
            return null;
        }
        return gson.toJson(vocabulary);
    }
    
    @TypeConverter
    public static List<VocabularyItem> toVocabularyItemList(String vocabularyJson) {
        if (vocabularyJson == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<VocabularyItem>>(){}.getType();
        return gson.fromJson(vocabularyJson, listType);
    }
    
    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
    
    @TypeConverter
    public static List<String> toStringList(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(json, listType);
    }
}