package com.nihonreader.app.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.nihonreader.app.R;
import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.models.VocabularyItem;
import com.nihonreader.app.utils.KanjiDictionary;

/**
 * Dialog for displaying details of a Japanese word with KANJIDIC2 information
 */
public class WordDetailsDialog extends Dialog {
    
    private JapaneseWord japaneseWord;
    private VocabularyItem vocabularyItem; // Optional vocabulary item with extra info
    
    public WordDetailsDialog(@NonNull Context context, JapaneseWord japaneseWord) {
        super(context);
        this.japaneseWord = japaneseWord;
    }
    
    public WordDetailsDialog(@NonNull Context context, JapaneseWord japaneseWord, VocabularyItem vocabularyItem) {
        super(context);
        this.japaneseWord = japaneseWord;
        this.vocabularyItem = vocabularyItem;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_word_details);
        
        // Initialize views
        TextView textWord = findViewById(R.id.text_word);
        TextView textReading = findViewById(R.id.text_reading);
        TextView textPos = findViewById(R.id.text_pos);
        TextView textDictionaryForm = findViewById(R.id.text_dictionary_form);
        TextView textDictionaryFormLabel = findViewById(R.id.text_dictionary_form_label);
        TextView textMeaning = findViewById(R.id.text_meaning);
        TextView textStrokeCountLabel = findViewById(R.id.text_stroke_count_label);
        TextView textStrokeCount = findViewById(R.id.text_stroke_count);
        
        // Set word details from Kuromoji parsing
        textWord.setText(japaneseWord.getSurface());
        
        // Set reading if available
        if (vocabularyItem != null && vocabularyItem.getReading() != null && !vocabularyItem.getReading().isEmpty()) {
            textReading.setText(vocabularyItem.getReading());
            textReading.setVisibility(View.VISIBLE);
        } else if (japaneseWord.getReading() != null && !japaneseWord.getReading().isEmpty() && 
            !japaneseWord.getReading().equals(japaneseWord.getSurface())) {
            textReading.setText(japaneseWord.getReading());
            textReading.setVisibility(View.VISIBLE);
        } else {
            textReading.setVisibility(View.GONE);
        }
        
        // Set part of speech
        textPos.setText(japaneseWord.getPartOfSpeech());
        
        // Set dictionary form if different from surface form
        String dictionaryForm = null;
        if (vocabularyItem != null && vocabularyItem.getDictionaryForm() != null && 
            !vocabularyItem.getDictionaryForm().isEmpty() && 
            !vocabularyItem.getDictionaryForm().equals(japaneseWord.getSurface())) {
            dictionaryForm = vocabularyItem.getDictionaryForm();
        } else if (japaneseWord.getBaseForm() != null && !japaneseWord.getBaseForm().isEmpty() && 
            !japaneseWord.getBaseForm().equals(japaneseWord.getSurface())) {
            dictionaryForm = japaneseWord.getBaseForm();
        }
        
        if (dictionaryForm != null) {
            textDictionaryForm.setText(dictionaryForm);
            textDictionaryFormLabel.setVisibility(View.VISIBLE);
            textDictionaryForm.setVisibility(View.VISIBLE);
        } else {
            textDictionaryFormLabel.setVisibility(View.GONE);
            textDictionaryForm.setVisibility(View.GONE);
        }
        
        // Set meaning
        String meaning = "";
        
        // First check vocabularyItem if available
        if (vocabularyItem != null && vocabularyItem.getMeaning() != null && !vocabularyItem.getMeaning().isEmpty()) {
            meaning = vocabularyItem.getMeaning();
        } 
        // Then check if word has meaning from Kuromoji
        else if (japaneseWord.getMeaning() != null && !japaneseWord.getMeaning().isEmpty()) {
            meaning = japaneseWord.getMeaning();
        } 
        // Default message if no meaning is available
        else {
            meaning = "No definition available";
        }
        
        textMeaning.setText(meaning);
        
        // Check if the word contains a single kanji character for stroke count display
        if (isKanji(japaneseWord.getSurface()) && japaneseWord.getSurface().length() == 1) {
            KanjiDictionary kanjiDictionary = KanjiDictionary.getInstance(getContext());
            KanjiDictionary.KanjiEntry kanjiEntry = kanjiDictionary.lookupKanji(japaneseWord.getSurface());
            
            if (kanjiEntry != null && kanjiEntry.getStrokeCount() != null) {
                textStrokeCount.setText(kanjiEntry.getStrokeCount());
                textStrokeCountLabel.setVisibility(View.VISIBLE);
                textStrokeCount.setVisibility(View.VISIBLE);
            } else {
                textStrokeCountLabel.setVisibility(View.GONE);
                textStrokeCount.setVisibility(View.GONE);
            }
        } else {
            textStrokeCountLabel.setVisibility(View.GONE);
            textStrokeCount.setVisibility(View.GONE);
        }
    }
    
    /**
     * Check if a string is a single kanji character
     */
    private boolean isKanji(String text) {
        if (TextUtils.isEmpty(text) || text.length() != 1) {
            return false;
        }
        
        char c = text.charAt(0);
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }
}