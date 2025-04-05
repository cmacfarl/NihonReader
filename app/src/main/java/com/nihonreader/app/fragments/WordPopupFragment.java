package com.nihonreader.app.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.nihonreader.app.R;
import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.models.VocabularyItem;
import com.nihonreader.app.utils.KanjiDictionary;

/**
 * A SatoriReader-style popup fragment for displaying word details
 */
public class WordPopupFragment extends DialogFragment {
    
    private static final String ARG_WORD = "word";
    private static final String ARG_READING = "reading";
    private static final String ARG_MEANING = "meaning";
    private static final String ARG_POS = "pos";
    private static final String ARG_DICTIONARY_FORM = "dictionaryForm";
    private static final String ARG_STROKE_COUNT = "strokeCount";
    
    private JapaneseWord japaneseWord;
    private VocabularyItem vocabularyItem;
    
    public WordPopupFragment() {
        // Required empty public constructor
    }
    
    /**
     * Create a new instance of the popup fragment with the given word details
     */
    public static WordPopupFragment newInstance(JapaneseWord word, VocabularyItem vocabularyItem) {
        WordPopupFragment fragment = new WordPopupFragment();
        Bundle args = new Bundle();
        
        // Add word info from JapaneseWord
        args.putString(ARG_WORD, word.getSurface());
        args.putString(ARG_READING, word.getReading());
        args.putString(ARG_POS, word.getPartOfSpeech());
        
        // Add additional info from VocabularyItem if available
        if (vocabularyItem != null) {
            if (!TextUtils.isEmpty(vocabularyItem.getReading())) {
                args.putString(ARG_READING, vocabularyItem.getReading());
            }
            args.putString(ARG_MEANING, vocabularyItem.getMeaning());
            
            if (!TextUtils.isEmpty(vocabularyItem.getDictionaryForm()) && 
                    !vocabularyItem.getDictionaryForm().equals(word.getSurface())) {
                args.putString(ARG_DICTIONARY_FORM, vocabularyItem.getDictionaryForm());
            } else if (!TextUtils.isEmpty(word.getBaseForm()) && 
                    !word.getBaseForm().equals(word.getSurface())) {
                args.putString(ARG_DICTIONARY_FORM, word.getBaseForm());
            }
        }
        
        // For single kanji, get stroke count
        if (isSingleKanji(word.getSurface())) {
            KanjiDictionary dictionary = KanjiDictionary.getInstance(null);
            KanjiDictionary.KanjiEntry entry = dictionary.lookupKanji(word.getSurface());
            if (entry != null && entry.getStrokeCount() != null) {
                args.putString(ARG_STROKE_COUNT, entry.getStrokeCount());
            }
        }
        
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.WordPopupStyle);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_word_details, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get views
        TextView textWord = view.findViewById(R.id.text_word);
        TextView textReading = view.findViewById(R.id.text_reading);
        TextView textDefinition = view.findViewById(R.id.text_definition);
        TextView textPos = view.findViewById(R.id.text_pos);
        TextView textDictionaryForm = view.findViewById(R.id.text_dictionary_form);
        LinearLayout dictionaryFormLayout = view.findViewById(R.id.dictionary_form_layout);
        TextView textStrokeCount = view.findViewById(R.id.text_stroke_count);
        LinearLayout strokeCountLayout = view.findViewById(R.id.stroke_count_layout);
        
        // Get arguments
        Bundle args = getArguments();
        if (args != null) {
            // Set word
            String word = args.getString(ARG_WORD);
            if (!TextUtils.isEmpty(word)) {
                textWord.setText(word);
            }
            
            // Set reading
            String reading = args.getString(ARG_READING);
            if (!TextUtils.isEmpty(reading)) {
                textReading.setText(reading);
                textReading.setVisibility(View.VISIBLE);
            } else {
                textReading.setVisibility(View.GONE);
            }
            
            // Set meaning
            String meaning = args.getString(ARG_MEANING, "No definition available");
            textDefinition.setText(meaning);
            
            // Set part of speech
            String pos = args.getString(ARG_POS);
            if (!TextUtils.isEmpty(pos)) {
                textPos.setText(pos);
            }
            
            // Set dictionary form
            String dictionaryForm = args.getString(ARG_DICTIONARY_FORM);
            if (!TextUtils.isEmpty(dictionaryForm)) {
                textDictionaryForm.setText(dictionaryForm);
                dictionaryFormLayout.setVisibility(View.VISIBLE);
            } else {
                dictionaryFormLayout.setVisibility(View.GONE);
            }
            
            // Set stroke count
            String strokeCount = args.getString(ARG_STROKE_COUNT);
            if (!TextUtils.isEmpty(strokeCount)) {
                textStrokeCount.setText(strokeCount);
                strokeCountLayout.setVisibility(View.VISIBLE);
            } else {
                strokeCountLayout.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Check if a string is a single kanji character
     */
    private static boolean isSingleKanji(String text) {
        if (TextUtils.isEmpty(text) || text.length() != 1) {
            return false;
        }
        
        char c = text.charAt(0);
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }
}