package com.nihonreader.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Provides access to KANJIDIC2 data for kanji lookup
 */
public class KanjiDictionary {
    private static final String TAG = "KanjiDictionary";
    
    // The URL for KANJIDIC2 (gzipped XML file)
    private static final String KANJIDIC_URL = "https://www.edrdg.org/kanjidic/kanjidic2.xml.gz";
    
    // Dictionary file name
    private static final String KANJIDIC_FILE = "kanjidic2.xml";
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "KanjiDictPrefs";
    private static final String KEY_DICT_DOWNLOADED = "dictionary_downloaded";
    
    // Singleton instance
    private static KanjiDictionary instance;
    
    private Context context;
    private Map<String, KanjiEntry> kanjiMap = new HashMap<>();
    private boolean isLoaded = false;
    private boolean isDownloading = false;
    private List<DictionaryLoadListener> loadListeners = new ArrayList<>();
    
    /**
     * Class representing a single kanji entry from KANJIDIC2
     */
    public static class KanjiEntry {
        private String kanji;
        private List<String> readings = new ArrayList<>();
        private List<String> meanings = new ArrayList<>();
        private String strokeCount;
        
        public KanjiEntry(String kanji) {
            this.kanji = kanji;
        }
        
        public void addReading(String reading) {
            readings.add(reading);
        }
        
        public void addMeaning(String meaning) {
            meanings.add(meaning);
        }
        
        public String getKanji() {
            return kanji;
        }
        
        public List<String> getReadings() {
            return readings;
        }
        
        public List<String> getMeanings() {
            return meanings;
        }
        
        public String getStrokeCount() {
            return strokeCount;
        }
        
        public void setStrokeCount(String strokeCount) {
            this.strokeCount = strokeCount;
        }
        
        /**
         * Get a comma-separated string of all readings
         */
        public String getReadingsString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < readings.size(); i++) {
                sb.append(readings.get(i));
                if (i < readings.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
        
        /**
         * Get a comma-separated string of all meanings
         */
        public String getMeaningsString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < meanings.size(); i++) {
                sb.append(meanings.get(i));
                if (i < meanings.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }
    
    /**
     * Interface for listening to dictionary load events
     */
    public interface DictionaryLoadListener {
        void onDictionaryLoaded();
        void onDictionaryLoadFailed(String error);
        void onDictionaryLoadProgress(int progress);
    }
    
    private KanjiDictionary(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized KanjiDictionary getInstance(Context context) {
        if (instance == null) {
            instance = new KanjiDictionary(context);
        }
        return instance;
    }
    
    /**
     * Add a listener for dictionary load events
     */
    public void addLoadListener(DictionaryLoadListener listener) {
        loadListeners.add(listener);
        
        // If dictionary is already loaded, notify the listener
        if (isLoaded) {
            listener.onDictionaryLoaded();
        }
    }
    
    /**
     * Remove a listener
     */
    public void removeLoadListener(DictionaryLoadListener listener) {
        loadListeners.remove(listener);
    }
    
    /**
     * Check if the dictionary exists and is loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Start loading the dictionary if not already loaded
     */
    public void loadDictionary() {
        if (isLoaded || isDownloading) {
            return;
        }
        
        // Check if dictionary file exists
        File dictFile = new File(context.getFilesDir(), KANJIDIC_FILE);
        
        if (dictFile.exists()) {
            // Dictionary file exists, parse it
            parseDictionaryFile(dictFile);
        } else {
            // Dictionary file doesn't exist, download it
            downloadDictionary();
        }
    }
    
    /**
     * Download the KANJIDIC2 file
     */
    private void downloadDictionary() {
        isDownloading = true;
        
        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    // Create a temporary file for the downloaded dictionary
                    File tempFile = new File(context.getCacheDir(), "kanjidic2.xml.gz");
                    
                    // Download the gzipped file
                    URL url = new URL(KANJIDIC_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    
                    // Get the file size
                    int fileSize = connection.getContentLength();
                    
                    try (InputStream inputStream = connection.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        int totalBytesRead = 0;
                        
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            
                            // Update progress
                            if (fileSize > 0) {
                                int progress = (int) ((totalBytesRead / (float) fileSize) * 100);
                                publishProgress(progress);
                            }
                        }
                    }
                    
                    // Extract the gzipped file
                    File dictFile = new File(context.getFilesDir(), KANJIDIC_FILE);
                    try (GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(tempFile));
                         FileOutputStream outputStream = new FileOutputStream(dictFile)) {
                        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        
                        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Delete the temporary file
                    tempFile.delete();
                    
                    // Mark as downloaded in SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putBoolean(KEY_DICT_DOWNLOADED, true).apply();
                    
                    // Parse the dictionary
                    return parseDictionaryFile(dictFile);
                    
                } catch (IOException e) {
                    Log.e(TAG, "Error downloading dictionary", e);
                    return false;
                }
            }
            
            @Override
            protected void onProgressUpdate(Integer... values) {
                int progress = values[0];
                for (DictionaryLoadListener listener : loadListeners) {
                    listener.onDictionaryLoadProgress(progress);
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                isDownloading = false;
                
                if (success) {
                    isLoaded = true;
                    for (DictionaryLoadListener listener : loadListeners) {
                        listener.onDictionaryLoaded();
                    }
                } else {
                    for (DictionaryLoadListener listener : loadListeners) {
                        listener.onDictionaryLoadFailed("Failed to download or parse dictionary");
                    }
                }
            }
        }.execute();
    }
    
    /**
     * Parse the KANJIDIC2 XML file
     */
    private boolean parseDictionaryFile(File dictFile) {
        try {
            FileInputStream inputStream = new FileInputStream(dictFile);
            
            // Since KANJIDIC2 is quite large, we'll parse just when needed
            // For now, just check that the file is valid
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, "UTF-8");
            
            // Check for the root element
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("kanjidic2")) {
                inputStream.close();
                
                // Mark as loaded
                isLoaded = true;
                for (DictionaryLoadListener listener : loadListeners) {
                    listener.onDictionaryLoaded();
                }
                return true;
            } else {
                inputStream.close();
                Log.e(TAG, "Invalid KANJIDIC2 file");
                for (DictionaryLoadListener listener : loadListeners) {
                    listener.onDictionaryLoadFailed("Invalid dictionary file");
                }
                return false;
            }
            
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Error parsing dictionary file", e);
            for (DictionaryLoadListener listener : loadListeners) {
                listener.onDictionaryLoadFailed("Error parsing dictionary: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Look up information for a specific kanji
     */
    public KanjiEntry lookupKanji(String kanji) {
        if (!isLoaded) {
            return null;
        }
        
        // If already in cache, return it
        if (kanjiMap.containsKey(kanji)) {
            return kanjiMap.get(kanji);
        }
        
        // Otherwise, parse the file to find the kanji
        try {
            File dictFile = new File(context.getFilesDir(), KANJIDIC_FILE);
            FileInputStream inputStream = new FileInputStream(dictFile);
            
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, "UTF-8");
            
            KanjiEntry currentEntry = null;
            String currentElement = null;
            boolean inReadingElement = false;
            String readingType = null;
            boolean inMeaningElement = false;
            
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name;
                
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        
                        if (name.equals("character")) {
                            currentEntry = null;
                            inReadingElement = false;
                            inMeaningElement = false;
                        } else if (name.equals("literal") && currentEntry == null) {
                            currentElement = "literal";
                        } else if (name.equals("stroke_count")) {
                            currentElement = "stroke_count";
                        } else if (name.equals("reading")) {
                            inReadingElement = true;
                            readingType = parser.getAttributeValue(null, "r_type");
                        } else if (name.equals("meaning") && parser.getAttributeCount() == 0) {
                            inMeaningElement = true;
                        }
                        break;
                        
                    case XmlPullParser.TEXT:
                        if (currentElement != null && currentElement.equals("literal")) {
                            if (parser.getText().equals(kanji)) {
                                // Found our kanji
                                currentEntry = new KanjiEntry(kanji);
                            }
                        } else if (currentEntry != null && currentElement != null && currentElement.equals("stroke_count")) {
                            currentEntry.setStrokeCount(parser.getText());
                        } else if (currentEntry != null && inReadingElement) {
                            if ("ja_on".equals(readingType) || "ja_kun".equals(readingType)) {
                                currentEntry.addReading(parser.getText());
                            }
                        } else if (currentEntry != null && inMeaningElement) {
                            currentEntry.addMeaning(parser.getText());
                        }
                        break;
                        
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        
                        if (name.equals("character")) {
                            if (currentEntry != null && currentEntry.getKanji().equals(kanji)) {
                                // We found and fully parsed our target kanji
                                kanjiMap.put(kanji, currentEntry);
                                inputStream.close();
                                return currentEntry;
                            }
                        } else if (name.equals("literal") || name.equals("stroke_count")) {
                            currentElement = null;
                        } else if (name.equals("reading")) {
                            inReadingElement = false;
                        } else if (name.equals("meaning")) {
                            inMeaningElement = false;
                        }
                        break;
                }
                
                eventType = parser.next();
            }
            
            inputStream.close();
            
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Error looking up kanji", e);
        }
        
        return null;
    }
}