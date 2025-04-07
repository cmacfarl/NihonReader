package com.nihonreader.app.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JSONExportImportUtils {
    private static final String TAG = "JSONExportImportUtils";
    private static final String METADATA_FILENAME = "stories_metadata.json";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Exports all stories and their content to a ZIP file
     * @param context Application context
     * @param stories List of stories to export
     * @param storyContents Map of story ID to story content
     * @param outputUri The URI to write the ZIP file to
     * @return true if export was successful, false otherwise
     */
    public static boolean exportToZip(Context context, List<Story> stories, Map<String, StoryContent> storyContents, Uri outputUri) {
        try {
            // Create a temporary directory to store files for zipping
            File tempDir = new File(context.getCacheDir(), "export_temp_" + System.currentTimeMillis());
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Create a JSON file with metadata
            JsonObject root = new JsonObject();
            root.addProperty("version", 1); 
            root.addProperty("exportDate", System.currentTimeMillis());
            
            JsonArray storiesArray = new JsonArray();
            Map<String, String> audioFilesMap = new HashMap<>(); // Original path -> Export path
            
            // Process each story
            for (Story story : stories) {
                JsonObject storyObject = new JsonObject();
                
                // Add all story properties
                storyObject.addProperty("id", story.getId());
                storyObject.addProperty("title", story.getTitle());
                storyObject.addProperty("author", story.getAuthor());
                storyObject.addProperty("description", story.getDescription());
                storyObject.addProperty("isCustom", story.isCustom());
                storyObject.addProperty("dateAdded", story.getDateAdded());
                storyObject.addProperty("lastOpened", story.getLastOpened());
                storyObject.addProperty("folderId", story.getFolderId());
                storyObject.addProperty("position", story.getPosition());
                
                // Add story content if available
                StoryContent content = storyContents.get(story.getId());
                if (content != null) {
                    JsonObject contentObject = new JsonObject();
                    contentObject.addProperty("id", content.getId());
                    contentObject.addProperty("storyId", content.getStoryId());
                    contentObject.addProperty("text", content.getText());
                    
                    // Handle audio file
                    String audioPath = content.getAudioUri();
                    if (audioPath != null && !audioPath.isEmpty()) {
                        File audioFile = new File(audioPath);
                        if (audioFile.exists()) {
                            // Generate a unique filename for the audio in the ZIP
                            String audioFileName = "audio/" + story.getId() + "_" + audioFile.getName();
                            contentObject.addProperty("audioFileName", audioFileName);
                            
                            // Add to the audio files map for later processing
                            audioFilesMap.put(audioPath, audioFileName);
                        }
                    }
                    
                    // Add segments if available
                    if (content.getSegments() != null && !content.getSegments().isEmpty()) {
                        JsonArray segmentsArray = new JsonArray();
                        for (AudioSegment segment : content.getSegments()) {
                            JsonObject segmentObject = new JsonObject();
                            segmentObject.addProperty("start", segment.getStart());
                            segmentObject.addProperty("end", segment.getEnd());
                            segmentObject.addProperty("text", segment.getText());
                            segmentsArray.add(segmentObject);
                        }
                        contentObject.add("segments", segmentsArray);
                    }
                    
                    storyObject.add("content", contentObject);
                }
                
                storiesArray.add(storyObject);
            }
            
            root.add("stories", storiesArray);
            
            // Write JSON metadata to a temporary file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(root);
            
            File metadataFile = new File(tempDir, METADATA_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(metadataFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            
            // Create audio directory in temp dir
            File audioDir = new File(tempDir, "audio");
            audioDir.mkdirs();
            
            // Copy all audio files to the temp directory
            for (Map.Entry<String, String> entry : audioFilesMap.entrySet()) {
                File sourceFile = new File(entry.getKey());
                File destFile = new File(tempDir, entry.getValue());
                
                // Ensure parent directory exists
                destFile.getParentFile().mkdirs();
                
                // Copy the file
                copyFile(sourceFile, destFile);
            }
            
            // Create the ZIP file
            OutputStream outputStream = context.getContentResolver().openOutputStream(outputUri);
            if (outputStream == null) {
                return false;
            }
            
            try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
                // Add all files in the temp directory to the ZIP
                addDirToZip(tempDir, tempDir, zipOut);
            }
            
            // Clean up temporary files
            deleteDir(tempDir);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting stories to ZIP", e);
            return false;
        }
    }
    
    /**
     * Add a directory and its contents to a ZIP file
     * @param rootDir The root directory for relative paths
     * @param sourceDir The directory to add
     * @param zipOut The ZIP output stream
     * @throws IOException If an I/O error occurs
     */
    private static void addDirToZip(File rootDir, File sourceDir, ZipOutputStream zipOut) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        byte[] buffer = new byte[BUFFER_SIZE];
        
        for (File file : files) {
            if (file.isDirectory()) {
                addDirToZip(rootDir, file, zipOut);
            } else {
                // Calculate the relative path for the ZIP entry
                String relativePath = file.getAbsolutePath().substring(rootDir.getAbsolutePath().length() + 1);
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipOut.putNextEntry(zipEntry);
                
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }
                }
                
                zipOut.closeEntry();
            }
        }
    }

    /**
     * Class representing the result of a JSON import operation
     */
    public static class ImportResult {
        public int totalStories;
        public int importedStories;
        public int skippedStories;
        public int failedStories;
        public List<String> errors = new ArrayList<>();
        public List<Story> stories = new ArrayList<>();
        public List<StoryContent> contents = new ArrayList<>();
    }

    /**
     * Imports stories from a ZIP file
     * @param context Application context
     * @param inputUri The URI of the ZIP file to import
     * @return ImportResult with information about the import operation
     * @throws IOException If an error occurs reading the file
     */
    public static ImportResult importFromZip(Context context, Uri inputUri) throws IOException {
        ImportResult result = new ImportResult();
        
        // Create a temporary directory to extract the ZIP contents
        File tempDir = new File(context.getCacheDir(), "import_temp_" + System.currentTimeMillis());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        try {
            // Extract the ZIP file
            extractZip(context, inputUri, tempDir);
            
            // Read the metadata file
            File metadataFile = new File(tempDir, METADATA_FILENAME);
            if (!metadataFile.exists()) {
                result.errors.add("Metadata file not found in the import package");
                return result;
            }
            
            // Parse the JSON metadata
            Gson gson = new Gson();
            JsonObject root;
            try (FileInputStream fis = new FileInputStream(metadataFile);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                root = gson.fromJson(reader, JsonObject.class);
            }
            
            // Check version for compatibility
            int version = root.has("version") ? root.get("version").getAsInt() : 1;
            Log.d(TAG, "Importing JSON version: " + version);
            
            // Prepare directories
            File appStoriesDir = new File(context.getFilesDir(), "stories");
            File appAudioDir = new File(context.getFilesDir(), "audio");
            
            if (!appStoriesDir.exists()) {
                appStoriesDir.mkdirs();
            }
            
            if (!appAudioDir.exists()) {
                appAudioDir.mkdirs();
            }
            
            // Parse stories
            JsonArray storiesArray = root.getAsJsonArray("stories");
            result.totalStories = storiesArray.size();
            
            List<Story> stories = new ArrayList<>();
            Map<String, StoryContent> contents = new HashMap<>();
            Map<String, String> audioFileMapping = new HashMap<>(); // Original ZIP path -> New file path
            
            // First pass: parse all story data
            for (int i = 0; i < storiesArray.size(); i++) {
                try {
                    JsonObject storyObject = storiesArray.get(i).getAsJsonObject();
                    
                    // Generate new unique IDs
                    String originalId = storyObject.get("id").getAsString();
                    String newId = "imported_" + UUID.randomUUID().toString();
                    
                    // Create Story object
                    Story story = new Story(
                            newId,
                            storyObject.get("title").getAsString(),
                            storyObject.has("author") ? storyObject.get("author").getAsString() : "",
                            storyObject.has("description") ? storyObject.get("description").getAsString() : "",
                            storyObject.has("isCustom") && storyObject.get("isCustom").getAsBoolean(),
                            storyObject.has("dateAdded") ? storyObject.get("dateAdded").getAsString() : String.valueOf(System.currentTimeMillis())
                    );
                    
                    // Set optional properties
                    if (storyObject.has("folderId") && !storyObject.get("folderId").isJsonNull()) {
                        story.setFolderId(storyObject.get("folderId").getAsString());
                    }
                    
                    if (storyObject.has("position")) {
                        story.setPosition(storyObject.get("position").getAsInt());
                    }
                    
                    if (storyObject.has("lastOpened")) {
                        story.setLastOpened(storyObject.get("lastOpened").getAsString());
                    }
                    
                    // Process story content
                    if (storyObject.has("content")) {
                        JsonObject contentObject = storyObject.getAsJsonObject("content");
                        String newContentId = "content_" + newId;
                        
                        // Create StoryContent object
                        StoryContent content = new StoryContent(
                                newContentId,
                                newId,
                                contentObject.has("text") ? contentObject.get("text").getAsString() : "",
                                null // We'll set the audio URI later
                        );
                        
                        // Handle audio file
                        if (contentObject.has("audioFileName")) {
                            String zipAudioPath = contentObject.get("audioFileName").getAsString();
                            File sourceAudioFile = new File(tempDir, zipAudioPath);
                            
                            if (sourceAudioFile.exists()) {
                                // Create a new audio file in the app's directory
                                String newAudioFileName = "audio_" + newId + ".mp3";
                                File destAudioFile = new File(appAudioDir, newAudioFileName);
                                
                                // Add mapping for later processing
                                audioFileMapping.put(sourceAudioFile.getAbsolutePath(), destAudioFile.getAbsolutePath());
                                
                                // Set the audio URI in the content object
                                content.setAudioUri(destAudioFile.getAbsolutePath());
                            } else {
                                Log.w(TAG, "Audio file not found in ZIP: " + zipAudioPath);
                            }
                        }
                        
                        // Handle segments
                        if (contentObject.has("segments")) {
                            JsonArray segmentsArray = contentObject.getAsJsonArray("segments");
                            List<AudioSegment> segments = new ArrayList<>();
                            
                            for (int j = 0; j < segmentsArray.size(); j++) {
                                JsonObject segmentObject = segmentsArray.get(j).getAsJsonObject();
                                AudioSegment segment = new AudioSegment(
                                        segmentObject.get("start").getAsLong(),
                                        segmentObject.get("end").getAsLong(),
                                        segmentObject.get("text").getAsString()
                                );
                                segments.add(segment);
                            }
                            
                            content.setSegments(segments);
                        }
                        
                        contents.put(newId, content);
                    }
                    
                    stories.add(story);
                    result.importedStories++;
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing story", e);
                    result.failedStories++;
                    result.errors.add("Error parsing story: " + e.getMessage());
                }
            }
            
            // Copy all audio files to their final locations
            for (Map.Entry<String, String> entry : audioFileMapping.entrySet()) {
                File sourceFile = new File(entry.getKey());
                File destFile = new File(entry.getValue());
                try {
                    copyFile(sourceFile, destFile);
                } catch (IOException e) {
                    Log.e(TAG, "Error copying audio file", e);
                    result.errors.add("Error copying audio file: " + e.getMessage());
                }
            }
            
            // Add stories and contents to the result
            result.stories = stories;
            result.contents = new ArrayList<>(contents.values());
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error importing from ZIP", e);
            result.errors.add("Error importing: " + e.getMessage());
            return result;
        } finally {
            // Clean up temporary files
            deleteDir(tempDir);
        }
    }
    
    /**
     * Extract a ZIP file to a directory
     * @param context Application context
     * @param inputUri The URI of the ZIP file
     * @param outputDir The directory to extract to
     * @throws IOException If an I/O error occurs
     */
    private static void extractZip(Context context, Uri inputUri, File outputDir) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
             ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(inputStream))) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            ZipEntry entry;
            
            while ((entry = zipIn.getNextEntry()) != null) {
                File outputFile = new File(outputDir, entry.getName());
                
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                    continue;
                }
                
                // Create parent directories if needed
                outputFile.getParentFile().mkdirs();
                
                // Extract the file
                try (FileOutputStream fos = new FileOutputStream(outputFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    int bytesRead;
                    while ((bytesRead = zipIn.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
                
                zipIn.closeEntry();
            }
        }
    }

    /**
     * Helper method to copy a file
     * @param sourceFile Source file
     * @param destFile Destination file
     * @throws IOException If an I/O error occurs
     */
    private static void copyFile(File sourceFile, File destFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(destFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * Recursively delete a directory and its contents
     * @param dir The directory to delete
     * @return true if the directory was deleted successfully
     */
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
} 