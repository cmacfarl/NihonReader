package com.nihonreader.app.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dictionary utility for Japanese-English translations
 */
public class JapaneseDictionary {
    private static final String TAG = "JapaneseDictionary";
    private static JapaneseDictionary instance;
    private final Map<String, String> dictionary = new HashMap<>();
    
    private JapaneseDictionary(Context context) {
        loadDictionary(context);
    }
    
    public static synchronized JapaneseDictionary getInstance(Context context) {
        if (instance == null) {
            instance = new JapaneseDictionary(context);
        }
        return instance;
    }
    
    private void loadDictionary(Context context) {
        try {
            // Load dictionary from assets
            InputStream inputStream = context.getAssets().open("dictionary.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            
            String json = new String(buffer, StandardCharsets.UTF_8);
            
            // Parse JSON
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> loadedDictionary = gson.fromJson(json, type);
            
            // Add to dictionary map
            dictionary.putAll(loadedDictionary);
            Log.d(TAG, "Dictionary loaded with " + dictionary.size() + " entries");
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading dictionary", e);
        }
    }
    
    /**
     * Look up a word in the dictionary
     * @param word The word to look up
     * @return The English meaning, or null if not found
     */
    public String lookupWord(String word) {
        return dictionary.get(word);
    }
}