package com.nihonreader.app.fragments;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.nihonreader.app.R;
import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.models.VocabularyItem;
import com.nihonreader.app.utils.KanjiDictionary;
import com.nihonreader.app.views.JapaneseTextView;

/**
 * A compact popup fragment for displaying word details that appears below the clicked word
 */
public class WordPopupFragment extends DialogFragment {
    
    // Width of the popup window
    private static final int POPUP_WIDTH = 240; // dp
    
    private static final String ARG_WORD = "word";
    private static final String ARG_READING = "reading";
    private static final String ARG_MEANING = "meaning";
    private static final String ARG_POS = "pos";
    private static final String ARG_DICTIONARY_FORM = "dictionaryForm";
    private static final String ARG_STROKE_COUNT = "strokeCount";
    
    // Store the click coordinates
    private static int clickX;
    private static int clickY;
    private static int wordLeft;
    private static int wordTop;
    private static int wordWidth;
    private static int wordHeight;
    private static JapaneseTextView originTextView;
    private static JapaneseWord clickedWord;
    
    public WordPopupFragment() {
        // Required empty public constructor
    }
    
    /**
     * Create a new instance of the popup fragment with the given word details
     */
    public static WordPopupFragment newInstance(JapaneseWord word, VocabularyItem vocabularyItem,
                                               View clickedView, int x, int y) {
        WordPopupFragment fragment = new WordPopupFragment();
        Bundle args = new Bundle();
        
        // Store the click position and view
        clickX = x;
        clickY = y;
        if (clickedView instanceof JapaneseTextView) {
            originTextView = (JapaneseTextView) clickedView;
            clickedWord = word;
            
            // Get the word's position within the text view
            Rect bounds = new Rect();
            CharSequence text = originTextView.getText();
            originTextView.getLayout().getLineBounds(
                    originTextView.getLayout().getLineForOffset(word.getStartIndex()), bounds);
            
            wordLeft = (int) originTextView.getLayout().getPrimaryHorizontal(word.getStartIndex());
            wordTop = bounds.top;
            wordWidth = (int) (originTextView.getLayout().getPrimaryHorizontal(word.getEndIndex()) - wordLeft);
            wordHeight = bounds.height();
        }
        
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
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.WordPopupStyle);
        // Make sure the dialog doesn't have a dim background
        setCancelable(true);
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
        TextView textStrokeCount = view.findViewById(R.id.text_stroke_count);
        
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
                textDictionaryForm.setText("Dictionary form: " + dictionaryForm);
                textDictionaryForm.setVisibility(View.VISIBLE);
            } else {
                textDictionaryForm.setVisibility(View.GONE);
            }
            
            // Set stroke count
            String strokeCount = args.getString(ARG_STROKE_COUNT);
            if (!TextUtils.isEmpty(strokeCount)) {
                textStrokeCount.setText("Strokes: " + strokeCount);
                textStrokeCount.setVisibility(View.VISIBLE);
            } else {
                textStrokeCount.setVisibility(View.GONE);
            }
        }
        
        // Enable closing by touch anywhere on the popup
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true);
        }
        
        // Also add click listener to explicitly dismiss
        view.setOnClickListener(v -> dismiss());
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        // Position the dialog below the clicked word
        Window window = getDialog().getWindow();
        if (window != null) {
            // Calculate size and position
            WindowManager.LayoutParams params = window.getAttributes();
            
            // Set fixed width for the popup
            int widthInPixels = (int) (POPUP_WIDTH * getResources().getDisplayMetrics().density);
            params.width = widthInPixels;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            
            // Calculate position to show below the word
            if (originTextView != null) {
                int[] location = new int[2];
                originTextView.getLocationOnScreen(location);
                
                // Position the popup below the word
                params.gravity = Gravity.TOP | Gravity.START;
                params.x = location[0] + wordLeft;
                params.y = location[1] + wordTop + wordHeight + 8; // 8dp padding
                
                // Check if the popup would go off-screen to the right
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                if (params.x + widthInPixels > screenWidth) {
                    params.x = screenWidth - widthInPixels - 8; // 8dp from edge
                }
                
                // Make the dialog non-modal
                params.dimAmount = 0.0f;
                params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                
                window.setAttributes(params);
            }
        }
    }
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        
        // Remove highlight when the popup is dismissed
        if (originTextView != null && clickedWord != null) {
            originTextView.removeHighlight();
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