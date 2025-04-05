package com.nihonreader.app.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.models.VocabularyItem;
import com.nihonreader.app.repository.StoryRepository;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for looking up word definitions from the vocabulary database
 */
public class DictionaryLookupService {
    
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private final StoryRepository repository;
    private final Context context;
    
    public interface OnWordDefinitionFoundListener {
        void onDefinitionFound(VocabularyItem vocabularyItem);
        void onDefinitionNotFound();
    }
    
    public DictionaryLookupService(Context context, StoryRepository repository) {
        this.context = context;
        this.repository = repository;
    }
    
    /**
     * Look up a word definition from the vocabulary database and create a new one if it doesn't exist
     */
    public void lookupWord(@NonNull JapaneseWord word, @NonNull OnWordDefinitionFoundListener listener) {
        executor.execute(() -> {
            // First, try to find the word in its surface form
            VocabularyItem vocabularyItem = repository.getVocabularyByWord(word.getSurface());
            
            // If not found and the word has a base form that's different, try that
            if (vocabularyItem == null && 
                    word.getBaseForm() != null && 
                    !word.getBaseForm().isEmpty() && 
                    !word.getBaseForm().equals(word.getSurface())) {
                vocabularyItem = repository.getVocabularyByWord(word.getBaseForm());
            }
            
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
     * Create a new vocabulary item from a Japanese word
     */
    private VocabularyItem createNewVocabularyItem(JapaneseWord word) {
        String id = UUID.randomUUID().toString();
        
        VocabularyItem vocabularyItem;
        if (word.getBaseForm() != null && !word.getBaseForm().isEmpty() && 
                !word.getBaseForm().equals(word.getSurface())) {
            vocabularyItem = new VocabularyItem(
                    id,
                    word.getSurface(),
                    word.getReading(),
                    "No definition available", // Default meaning
                    word.getBaseForm() // Dictionary form for verbs
            );
        } else {
            vocabularyItem = new VocabularyItem(
                    id,
                    word.getSurface(),
                    word.getReading(),
                    "No definition available" // Default meaning
            );
        }
        
        return vocabularyItem;
    }
}