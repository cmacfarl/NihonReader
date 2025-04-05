package com.nihonreader.app.utils;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.nihonreader.app.models.JapaneseWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing Japanese text using Kuromoji
 */
public class JapaneseTextParser {
    
    private static Tokenizer tokenizer;
    
    /**
     * Get a singleton instance of the Kuromoji tokenizer
     */
    private static synchronized Tokenizer getTokenizer() {
        if (tokenizer == null) {
            tokenizer = new Tokenizer();
        }
        return tokenizer;
    }
    
    /**
     * Parse a Japanese text into a list of JapaneseWord objects
     * 
     * @param text The Japanese text to parse
     * @return A list of JapaneseWord objects
     */
    public static List<JapaneseWord> parseText(String text) {
        List<JapaneseWord> words = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return words;
        }
        
        // Tokenize the text using Kuromoji
        List<Token> tokens = getTokenizer().tokenize(text);
        
        int currentPosition = 0;
        for (Token token : tokens) {
            String surface = token.getSurface();
            
            // Find the actual position in the original text to handle potential whitespace
            while (currentPosition < text.length() && !text.substring(currentPosition).startsWith(surface)) {
                currentPosition++;
            }
            
            if (currentPosition < text.length()) {
                int startIndex = currentPosition;
                int endIndex = startIndex + surface.length();
                
                JapaneseWord word = new JapaneseWord(
                        surface,
                        token.getReading(),
                        token.getBaseForm(),
                        token.getPartOfSpeechLevel1(),
                        startIndex,
                        endIndex
                );
                
                words.add(word);
                currentPosition = endIndex;
            }
        }
        
        return words;
    }
    
    /**
     * Get a list of JapaneseWord objects that include a specific character position
     * 
     * @param words The list of parsed words
     * @param position The character position in the original text
     * @return The word at the position, or null if no word is found
     */
    public static JapaneseWord getWordAtPosition(List<JapaneseWord> words, int position) {
        for (JapaneseWord word : words) {
            if (position >= word.getStartIndex() && position < word.getEndIndex()) {
                return word;
            }
        }
        return null;
    }
}