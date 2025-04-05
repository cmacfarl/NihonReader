package com.nihonreader.app.utils;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.models.VocabularyItem;
import com.nihonreader.app.repository.StoryRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for looking up word definitions from the vocabulary database and KANJIDIC2
 */
public class DictionaryLookupService {
    
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private final StoryRepository repository;
    private final Context context;
    private final KanjiDictionary kanjiDictionary;
    private final Tokenizer tokenizer = new Tokenizer();
    
    public interface OnWordDefinitionFoundListener {
        void onDefinitionFound(VocabularyItem vocabularyItem);
        void onDefinitionNotFound();
    }
    
    public DictionaryLookupService(Context context, StoryRepository repository) {
        this.context = context;
        this.repository = repository;
        this.kanjiDictionary = KanjiDictionary.getInstance(context);
        
        // Ensure the kanji dictionary is loaded
        kanjiDictionary.loadDictionary();
    }
    
    /**
     * Look up a word definition from the vocabulary database and KANJIDIC2, 
     * always using the dictionary form (base form) when available
     */
    public void lookupWord(@NonNull JapaneseWord word, @NonNull OnWordDefinitionFoundListener listener) {
        executor.execute(() -> {
            // Always prioritize the base form (dictionary form) if available
            String lookupWord = word.getSurface();
            if (word.getBaseForm() != null && !word.getBaseForm().isEmpty()) {
                lookupWord = word.getBaseForm();
            }
            
            // Look up the word in the database
            VocabularyItem vocabularyItem = repository.getVocabularyByWord(lookupWord);
            
            // If still not found, create a new vocabulary item
            if (vocabularyItem == null) {
                vocabularyItem = createNewVocabularyItem(word);
                
                // Save the new vocabulary item to the database
                repository.insertVocabularyItem(vocabularyItem);
            }
            
            // Pass the result back to the listener
            final VocabularyItem finalItem = vocabularyItem;
            if (finalItem != null) {
                listener.onDefinitionFound(finalItem);
            } else {
                listener.onDefinitionNotFound();
            }
        });
    }
    
    /**
     * Create a new vocabulary item from a Japanese word, using KANJIDIC2 for kanji information
     */
    private VocabularyItem createNewVocabularyItem(JapaneseWord word) {
        String id = UUID.randomUUID().toString();
        
        // Default meaning if not found in dictionary
        String meaning = "No definition available";
        String reading = word.getReading();
        
        // Determine which form to use for kanji lookup
        String formForLookup = word.getSurface();
        
        // If the word has a base form, use that as the preferred lookup form
        if (word.getBaseForm() != null && !word.getBaseForm().isEmpty()) {
            formForLookup = word.getBaseForm();
        }
        
        // Check if the word contains any kanji that can be looked up
        if (containsKanji(formForLookup)) {
            StringBuilder meaningBuilder = new StringBuilder();
            
            // Look up each kanji in the word
            for (char c : formForLookup.toCharArray()) {
                String kanjiChar = String.valueOf(c);
                
                // Skip non-kanji characters
                if (!isKanji(kanjiChar)) {
                    continue;
                }
                
                KanjiDictionary.KanjiEntry kanjiEntry = kanjiDictionary.lookupKanji(kanjiChar);
                if (kanjiEntry != null) {
                    if (!TextUtils.isEmpty(meaningBuilder.toString())) {
                        meaningBuilder.append("; ");
                    }
                    meaningBuilder.append(kanjiChar).append(": ").append(kanjiEntry.getMeaningsString());
                }
            }
            
            if (meaningBuilder.length() > 0) {
                meaning = meaningBuilder.toString();
            }
            
            // Also, try to get a better reading if available
            if (TextUtils.isEmpty(reading)) {
                // Prefer to get reading from the dictionary form
                if (word.getBaseForm() != null && !word.getBaseForm().isEmpty()) {
                    reading = lookupReading(word.getBaseForm());
                }
                // Fallback to surface form if necessary
                if (TextUtils.isEmpty(reading)) {
                    reading = lookupReading(word.getSurface());
                }
            }
        }
        
        VocabularyItem vocabularyItem;
        if (word.getBaseForm() != null && !word.getBaseForm().isEmpty() && 
                !word.getBaseForm().equals(word.getSurface())) {
            vocabularyItem = new VocabularyItem(
                    id,
                    word.getSurface(),
                    reading,
                    meaning,
                    word.getBaseForm() // Dictionary form for verbs
            );
        } else {
            vocabularyItem = new VocabularyItem(
                    id,
                    word.getSurface(),
                    reading,
                    meaning
            );
            vocabularyItem.setDictionaryForm(word.getSurface());
        }
        
        return vocabularyItem;
    }
    
    /**
     * Use Kuromoji to get a reading for the word
     */
    private String lookupReading(String word) {
        if (TextUtils.isEmpty(word)) {
            return "";
        }
        
        List<Token> tokens = tokenizer.tokenize(word);
        if (!tokens.isEmpty()) {
            return tokens.get(0).getReading();
        }
        
        return "";
    }
    
    /**
     * Check if a string contains any kanji characters
     */
    private boolean containsKanji(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (isKanji(String.valueOf(c))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a character is a kanji
     */
    private boolean isKanji(String character) {
        if (TextUtils.isEmpty(character) || character.length() > 1) {
            return false;
        }
        
        char c = character.charAt(0);
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }
}