package com.nihonreader.app.views;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.utils.JapaneseTextParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom TextView that supports Japanese word parsing and click handling with highlighting
 */
public class JapaneseTextView extends AppCompatTextView {
    
    private String originalText;
    private List<JapaneseWord> parsedWords = new ArrayList<>();
    private OnWordClickListener onWordClickListener;
    private SpannableStringBuilder spannableBuilder;
    private JapaneseWord highlightedWord;
    private ForegroundColorSpan currentColorSpan;
    private UnderlineSpan currentUnderlineSpan;
    
    // Colors for highlighting
    private int highlightColor;
    
    public interface OnWordClickListener {
        void onWordClicked(JapaneseWord word);
    }
    
    public JapaneseTextView(@NonNull Context context) {
        super(context);
        init();
    }
    
    public JapaneseTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public JapaneseTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Enable click handling on text
        setMovementMethod(LinkMovementMethod.getInstance());
        // Get highlight color from resources
        highlightColor = getContext().getResources().getColor(com.nihonreader.app.R.color.wordHighlightBlue);
    }
    
    public void setOnWordClickListener(OnWordClickListener listener) {
        this.onWordClickListener = listener;
    }
    
    /**
     * Set the Japanese text and parse it into clickable words
     */
    public void setJapaneseText(String text) {
        this.originalText = text;
        
        // Parse the text using Kuromoji
        this.parsedWords = JapaneseTextParser.parseText(text);
        
        // Apply clickable spans to the text
        applyClickableSpans();
    }
    
    private void applyClickableSpans() {
        if (originalText == null || originalText.isEmpty() || parsedWords.isEmpty()) {
            setText("");
            return;
        }
        
        spannableBuilder = new SpannableStringBuilder(originalText);
        
        for (JapaneseWord word : parsedWords) {
            if (word.isClickable()) {
                final JapaneseWord clickedWord = word;
                
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        highlightWord(clickedWord);
                        if (onWordClickListener != null) {
                            onWordClickListener.onWordClicked(clickedWord);
                        }
                    }
                    
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        // Don't show the default underline
                        ds.setUnderlineText(false);
                    }
                };
                
                spannableBuilder.setSpan(
                        clickableSpan,
                        word.getStartIndex(),
                        word.getEndIndex(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        
        setText(spannableBuilder);
    }
    
    /**
     * Highlight a word by applying a light blue color and underline (SatoriReader style)
     */
    public void highlightWord(JapaneseWord word) {
        // Remove previous highlight if any
        removeHighlight();
        
        // Add new highlight
        highlightedWord = word;
        currentColorSpan = new ForegroundColorSpan(highlightColor);
        currentUnderlineSpan = new UnderlineSpan();
        
        spannableBuilder.setSpan(
                currentColorSpan,
                word.getStartIndex(),
                word.getEndIndex(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        spannableBuilder.setSpan(
                currentUnderlineSpan,
                word.getStartIndex(),
                word.getEndIndex(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        setText(spannableBuilder);
    }
    
    /**
     * Remove the current highlight
     */
    public void removeHighlight() {
        if (spannableBuilder != null && highlightedWord != null) {
            if (currentColorSpan != null) {
                spannableBuilder.removeSpan(currentColorSpan);
            }
            if (currentUnderlineSpan != null) {
                spannableBuilder.removeSpan(currentUnderlineSpan);
            }
            setText(spannableBuilder);
            highlightedWord = null;
            currentColorSpan = null;
            currentUnderlineSpan = null;
        }
    }
    
    /**
     * Get the list of parsed words
     */
    public List<JapaneseWord> getParsedWords() {
        return parsedWords;
    }
}