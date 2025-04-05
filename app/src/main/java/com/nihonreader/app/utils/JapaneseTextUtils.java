package com.nihonreader.app.utils;

import android.util.Log;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JapaneseTextUtils {
    private static final String TAG = "JapaneseTextUtils";
    private static final Tokenizer tokenizer = new Tokenizer();
    
    // Pattern for identifying sentence endings in mixed text
    private static final Pattern SENTENCE_END_PATTERN = 
        Pattern.compile("([。.．！!？?…]+\\s*|」\\s*|[。.．！!？?…]+」\\s*|\\n+)");
    
    /**
     * Splits Japanese text into sentences based on sentence-ending particles and punctuation
     * @param text The Japanese text to split
     * @return List of sentences
     */
    public static List<String> splitIntoSentences(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> sentences = new ArrayList<>();
        
        try {
            Log.d(TAG, "Tokenizing text: " + text);
            
            // First try using kuromoji for proper Japanese segmentation
            List<Token> tokens = tokenizer.tokenize(text);
            
            StringBuilder currentSentence = new StringBuilder();
            boolean foundAnySentenceEnd = false;
            
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                currentSentence.append(token.getSurface());
                
                // Check for sentence endings
                String pos = token.getPartOfSpeechLevel1();
                String surface = token.getSurface();
                
                boolean isEndOfSentence = false;
                
                // Check for sentence-ending punctuation (Japanese and standard)
                if ((pos.equals("記号") || pos.equals("助詞")) && 
                    (surface.equals("。") || surface.equals("！") || surface.equals("？") || 
                     surface.equals("…") || surface.equals("」") || 
                     surface.equals(".") || surface.equals("!") || surface.equals("?"))) {
                    isEndOfSentence = true;
                    foundAnySentenceEnd = true;
                }
                
                // Check for sentence-ending verbs
                if (pos.equals("動詞") && i < tokens.size() - 1) {
                    Token nextToken = tokens.get(i + 1);
                    if ((nextToken.getPartOfSpeechLevel1().equals("記号") || 
                         nextToken.getPartOfSpeechLevel1().equals("助詞")) && 
                        (nextToken.getSurface().equals("。") || nextToken.getSurface().equals("！") || 
                         nextToken.getSurface().equals("？") || nextToken.getSurface().equals("…") || 
                         nextToken.getSurface().equals("」") ||
                         nextToken.getSurface().equals(".") || nextToken.getSurface().equals("!") || 
                         nextToken.getSurface().equals("?"))) {
                        isEndOfSentence = true;
                        foundAnySentenceEnd = true;
                    }
                }
                
                // Check for newlines as potential sentence boundaries
                if (surface.contains("\n")) {
                    isEndOfSentence = true;
                    foundAnySentenceEnd = true;
                }
                
                if (isEndOfSentence) {
                    String sentence = currentSentence.toString().trim();
                    if (!sentence.isEmpty()) {
                        sentences.add(sentence);
                        Log.d(TAG, "Added sentence: " + sentence);
                    }
                    currentSentence = new StringBuilder();
                }
            }
            
            // Add any remaining text as the last sentence
            if (currentSentence.length() > 0) {
                String sentence = currentSentence.toString().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                    Log.d(TAG, "Added final sentence: " + sentence);
                }
            }
            
            // If no sentence endings were found with kuromoji, try fallback method
            if (!foundAnySentenceEnd && sentences.size() <= 1) {
                Log.d(TAG, "No sentence endings found with kuromoji, using fallback method");
                sentences = fallbackSentenceSplitting(text);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in splitIntoSentences: " + e.getMessage());
            // If kuromoji fails, fall back to simpler method
            sentences = fallbackSentenceSplitting(text);
        }
        
        // Return original text as a single sentence if all else fails
        if (sentences.isEmpty()) {
            sentences.add(text);
            Log.d(TAG, "Using full text as single sentence: " + text);
        }
        
        Log.d(TAG, "Total sentences found: " + sentences.size());
        return sentences;
    }
    
    /**
     * Fallback method for splitting text into sentences using regex patterns
     * This is used when kuromoji doesn't detect any sentence endings
     */
    private static List<String> fallbackSentenceSplitting(String text) {
        List<String> sentences = new ArrayList<>();
        
        // First try splitting by Japanese and regular punctuation
        String[] segments = SENTENCE_END_PATTERN.split(text);
        
        // If that didn't work well, try simpler pattern for western text
        if (segments.length <= 1) {
            segments = text.split("(?<=[.!?])\\s+");
        }
        
        // If we still don't have multiple sentences, try splitting by newlines
        if (segments.length <= 1 && text.contains("\n")) {
            segments = text.split("\n+");
        }
        
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
                Log.d(TAG, "Added fallback sentence: " + trimmed);
            }
        }
        
        return sentences;
    }
} 