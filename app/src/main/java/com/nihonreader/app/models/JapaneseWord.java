package com.nihonreader.app.models;

/**
 * Model class for a parsed Japanese word with morphological information
 */
public class JapaneseWord {
    private String surface; // The surface form as it appears in text
    private String reading; // Furigana reading
    private String baseForm; // Dictionary form for verbs, adjectives
    private String partOfSpeech; // Part of speech (noun, verb, etc.)
    private String meaning; // English meaning (if available)
    private int startIndex; // Start position in the original text
    private int endIndex; // End position in the original text
    private boolean isClickable; // Whether this word should respond to clicks (not particles)

    public JapaneseWord(String surface, String reading, String baseForm, String partOfSpeech, 
                        int startIndex, int endIndex) {
        this.surface = surface;
        this.reading = reading;
        this.baseForm = baseForm;
        this.partOfSpeech = partOfSpeech;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        
        // Determine if this word should be clickable based on part of speech
        this.isClickable = !partOfSpeech.contains("助詞") && // Not a particle
                           !partOfSpeech.contains("助動詞") && // Not an auxiliary verb 
                           !partOfSpeech.contains("記号"); // Not a symbol/punctuation
    }
    
    public String getSurface() {
        return surface;
    }
    
    public String getReading() {
        return reading;
    }
    
    public String getBaseForm() {
        return baseForm;
    }
    
    public String getPartOfSpeech() {
        return partOfSpeech;
    }
    
    public String getMeaning() {
        return meaning;
    }
    
    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public boolean isClickable() {
        return isClickable;
    }
}