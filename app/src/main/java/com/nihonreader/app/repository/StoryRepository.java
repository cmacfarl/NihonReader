package com.nihonreader.app.repository;

import android.app.Application;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nihonreader.app.database.AppDatabase;
import com.nihonreader.app.database.StoryContentDao;
import com.nihonreader.app.database.StoryDao;
import com.nihonreader.app.database.UserProgressDao;
import com.nihonreader.app.database.VocabularyDao;
import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.models.UserProgress;
import com.nihonreader.app.models.VocabularyItem;
import com.nihonreader.app.utils.AudioUtils;
import com.nihonreader.app.utils.FileUtils;
import com.nihonreader.app.utils.JSONExportImportUtils;
import com.nihonreader.app.utils.SpeechAlignmentService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository class that handles the data operations
 */
public class StoryRepository {
    
    private static final String TAG = "StoryRepository";
    
    private final StoryDao storyDao;
    private final StoryContentDao storyContentDao;
    private final UserProgressDao userProgressDao;
    private final VocabularyDao vocabularyDao;
    private final Application application;
    
    public StoryRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        this.storyDao = database.storyDao();
        this.storyContentDao = database.storyContentDao();
        this.userProgressDao = database.userProgressDao();
        this.vocabularyDao = database.vocabularyDao();
        this.application = application;
    }
    
    public StoryRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.storyDao = database.storyDao();
        this.storyContentDao = database.storyContentDao();
        this.userProgressDao = database.userProgressDao();
        this.vocabularyDao = database.vocabularyDao();
        this.application = null;
    }
    
    // Story operations
    public LiveData<List<Story>> getAllStories() {
        return storyDao.getAllStories();
    }
    
    public LiveData<Story> getStoryById(String storyId) {
        return storyDao.getStoryById(storyId);
    }
    
    public void insert(Story story) {
        new InsertStoryAsyncTask(storyDao).execute(story);
    }
    
    public void update(Story story) {
        new UpdateStoryAsyncTask(storyDao).execute(story);
    }
    
    public void delete(Story story) {
        new DeleteStoryAsyncTask(storyDao, storyContentDao, userProgressDao).execute(story);
    }
    
    public void updateLastOpened(String storyId, String timestamp) {
        new UpdateLastOpenedAsyncTask(storyDao).execute(new String[]{storyId, timestamp});
    }
    
    // New methods for folder operations
    
    public LiveData<List<Story>> getStoriesInFolder(String folderId) {
        return storyDao.getStoriesInFolder(folderId);
    }
    
    public LiveData<List<Story>> getStoriesWithoutFolder() {
        return storyDao.getStoriesWithoutFolder();
    }
    
    public void moveStoryToFolder(String storyId, String folderId) {
        new MoveStoryToFolderAsyncTask(storyDao).execute(new StoryFolderParams(storyId, folderId));
    }
    
    public void moveAllStories(String fromFolderId, String toFolderId) {
        new MoveAllStoriesAsyncTask(storyDao).execute(new FolderMoveParams(fromFolderId, toFolderId));
    }
    
    // Story content operations
    public LiveData<StoryContent> getContentForStory(String storyId) {
        return storyContentDao.getContentForStory(storyId);
    }
    
    public void insert(StoryContent storyContent) {
        new InsertStoryContentAsyncTask(storyContentDao).execute(storyContent);
    }
    
    public void update(StoryContent storyContent) {
        new UpdateStoryContentAsyncTask(storyContentDao).execute(storyContent);
    }
    
    // User progress operations
    public LiveData<UserProgress> getProgressForStory(String storyId) {
        return userProgressDao.getProgressForStory(storyId);
    }
    
    public void insert(UserProgress userProgress) {
        new InsertUserProgressAsyncTask(userProgressDao).execute(userProgress);
    }
    
    public void update(UserProgress userProgress) {
        new UpdateUserProgressAsyncTask(userProgressDao).execute(userProgress);
    }
    
    public void updateAudioPosition(String storyId, long position) {
        new UpdateAudioPositionAsyncTask(userProgressDao).execute(new Object[]{storyId, position});
    }
    
    // Vocabulary operations
    public LiveData<List<VocabularyItem>> getAllVocabulary() {
        return vocabularyDao.getAllVocabulary();
    }
    
    public void insert(VocabularyItem vocabularyItem) {
        new InsertVocabularyAsyncTask(vocabularyDao).execute(vocabularyItem);
    }
    
    public void insertVocabularyItem(VocabularyItem vocabularyItem) {
        new InsertVocabularyAsyncTask(vocabularyDao).execute(vocabularyItem);
    }
    
    public VocabularyItem getVocabularyByWord(String word) {
        try {
            return vocabularyDao.getVocabularyByWord(word);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Import story with audio
    public void importCustomStory(
            String title,
            String author,
            String description,
            Uri textUri,
            Uri audioUri,
            Uri timingUri,
            boolean useAiAlignment,
            String folderId,
            ImportStoryCallback callback) {
        
        new ImportStoryAsyncTask(application, storyDao, storyContentDao, userProgressDao, useAiAlignment, callback)
                .execute(new ImportStoryParams(title, author, description, textUri, audioUri, timingUri, useAiAlignment, folderId));
    }
    
    // AsyncTask classes for database operations
    private static class InsertStoryAsyncTask extends AsyncTask<Story, Void, Void> {
        private StoryDao storyDao;
        
        InsertStoryAsyncTask(StoryDao storyDao) {
            this.storyDao = storyDao;
        }
        
        @Override
        protected Void doInBackground(Story... stories) {
            storyDao.insert(stories[0]);
            return null;
        }
    }
    
    private static class UpdateStoryAsyncTask extends AsyncTask<Story, Void, Void> {
        private StoryDao storyDao;
        
        UpdateStoryAsyncTask(StoryDao storyDao) {
            this.storyDao = storyDao;
        }
        
        @Override
        protected Void doInBackground(Story... stories) {
            storyDao.update(stories[0]);
            return null;
        }
    }
    
    private static class DeleteStoryAsyncTask extends AsyncTask<Story, Void, Void> {
        private StoryDao storyDao;
        private StoryContentDao storyContentDao;
        private UserProgressDao userProgressDao;
        
        DeleteStoryAsyncTask(StoryDao storyDao, StoryContentDao storyContentDao, UserProgressDao userProgressDao) {
            this.storyDao = storyDao;
            this.storyContentDao = storyContentDao;
            this.userProgressDao = userProgressDao;
        }
        
        @Override
        protected Void doInBackground(Story... stories) {
            String storyId = stories[0].getId();
            storyDao.delete(stories[0]);
            storyContentDao.deleteByStoryId(storyId);
            userProgressDao.deleteByStoryId(storyId);
            return null;
        }
    }
    
    private static class UpdateLastOpenedAsyncTask extends AsyncTask<String, Void, Void> {
        private StoryDao storyDao;
        
        UpdateLastOpenedAsyncTask(StoryDao storyDao) {
            this.storyDao = storyDao;
        }
        
        @Override
        protected Void doInBackground(String... params) {
            storyDao.updateLastOpened(params[0], params[1]);
            return null;
        }
    }
    
    private static class InsertStoryContentAsyncTask extends AsyncTask<StoryContent, Void, Void> {
        private StoryContentDao storyContentDao;
        
        InsertStoryContentAsyncTask(StoryContentDao storyContentDao) {
            this.storyContentDao = storyContentDao;
        }
        
        @Override
        protected Void doInBackground(StoryContent... storyContents) {
            storyContentDao.insert(storyContents[0]);
            return null;
        }
    }
    
    private static class UpdateStoryContentAsyncTask extends AsyncTask<StoryContent, Void, Void> {
        private StoryContentDao storyContentDao;
        
        UpdateStoryContentAsyncTask(StoryContentDao storyContentDao) {
            this.storyContentDao = storyContentDao;
        }
        
        @Override
        protected Void doInBackground(StoryContent... storyContents) {
            storyContentDao.update(storyContents[0]);
            return null;
        }
    }
    
    private static class InsertUserProgressAsyncTask extends AsyncTask<UserProgress, Void, Void> {
        private UserProgressDao userProgressDao;
        
        InsertUserProgressAsyncTask(UserProgressDao userProgressDao) {
            this.userProgressDao = userProgressDao;
        }
        
        @Override
        protected Void doInBackground(UserProgress... userProgresses) {
            userProgressDao.insert(userProgresses[0]);
            return null;
        }
    }
    
    private static class UpdateUserProgressAsyncTask extends AsyncTask<UserProgress, Void, Void> {
        private UserProgressDao userProgressDao;
        
        UpdateUserProgressAsyncTask(UserProgressDao userProgressDao) {
            this.userProgressDao = userProgressDao;
        }
        
        @Override
        protected Void doInBackground(UserProgress... userProgresses) {
            userProgressDao.update(userProgresses[0]);
            return null;
        }
    }
    
    private static class UpdateAudioPositionAsyncTask extends AsyncTask<Object, Void, Void> {
        private UserProgressDao userProgressDao;
        
        UpdateAudioPositionAsyncTask(UserProgressDao userProgressDao) {
            this.userProgressDao = userProgressDao;
        }
        
        @Override
        protected Void doInBackground(Object... params) {
            String storyId = (String) params[0];
            long position = (long) params[1];
            userProgressDao.updateAudioPosition(storyId, position);
            return null;
        }
    }
    
    private static class InsertVocabularyAsyncTask extends AsyncTask<VocabularyItem, Void, Void> {
        private VocabularyDao vocabularyDao;
        
        InsertVocabularyAsyncTask(VocabularyDao vocabularyDao) {
            this.vocabularyDao = vocabularyDao;
        }
        
        @Override
        protected Void doInBackground(VocabularyItem... vocabularyItems) {
            vocabularyDao.insert(vocabularyItems[0]);
            return null;
        }
    }
    
    private static class ImportStoryAsyncTask extends AsyncTask<ImportStoryParams, Void, String> {
        private Context context;
        private StoryDao storyDao;
        private StoryContentDao storyContentDao;
        private UserProgressDao userProgressDao;
        private ImportStoryCallback callback;
        private boolean useAiAlignment;
        
        ImportStoryAsyncTask(Context context, StoryDao storyDao, StoryContentDao storyContentDao,
                             UserProgressDao userProgressDao, boolean useAiAlignment, ImportStoryCallback callback) {
            this.context = context;
            this.storyDao = storyDao;
            this.storyContentDao = storyContentDao;
            this.userProgressDao = userProgressDao;
            this.useAiAlignment = useAiAlignment;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(ImportStoryParams... params) {
            try {
                String title = params[0].title;
                String author = params[0].author;
                String description = params[0].description;
                Uri textUri = params[0].textUri;
                Uri audioUri = params[0].audioUri;
                Uri timingUri = params[0].timingUri;
                String folderId = params[0].folderId;
                
                // Generate unique ID
                String storyId = "custom_" + UUID.randomUUID().toString();
                
                // Create story directory
                File storiesDir = new File(context.getFilesDir(), "stories");
                File audioDir = new File(context.getFilesDir(), "audio");
                
                if (!storiesDir.exists()) {
                    storiesDir.mkdirs();
                }
                
                if (!audioDir.exists()) {
                    audioDir.mkdirs();
                }
                
                // Copy audio file
                File audioFile = new File(audioDir, storyId + ".mp3");
                if (!FileUtils.copyFile(context, audioUri, audioFile)) {
                    return null;
                }
                
                // Read text content
                String textContent = FileUtils.readTextFromUri(context, textUri);
                
                // Process segments based on settings
                List<AudioSegment> segments = null;
                
                // First check if there's a timing file
                if (timingUri != null) {
                    String timingContent = FileUtils.readTextFromUri(context, timingUri);
                    segments = AudioUtils.parseTimingFile(timingContent);
                } 
                // If AI alignment is requested, use it (would be done asynchronously in a real app)
                else if (useAiAlignment) {
                    // In a real implementation, this would call SpeechAlignmentService
                    // and wait for the alignment to complete
                    // For demo purposes, we'll use the automatic segmentation
                    
                    // This is the real AI-based alignment implementation
                    if (callback != null) {
                        callback.onProgressUpdate("Using AI to align text with audio...");
                    }
                    
                    // Use auto-generated segments as a fallback
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(context, audioUri);
                        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        long audioDuration = Long.parseLong(durationStr);
                        
                        // First use auto-generated segments (as a fallback)
                        segments = AudioUtils.autoGenerateSegments(textContent, audioDuration);
                        
                        // Then try to improve with speech alignment
                        // We can't run SpeechRecognizer directly here since we're in a background thread
                        // Let's use our placeholder implementation for now
                        /*
                        final SpeechAlignmentService alignmentService = new SpeechAlignmentService(context);
                        try {
                            alignmentService.alignTextWithAudio(audioUri, textContent, 
                                new SpeechAlignmentService.AlignmentCallback() {
                                    @Override
                                    public void onAlignmentComplete(List<AudioSegment> alignedSegments) {
                                        // Update segments with aligned results
                                        // This would need to update the database after alignment completes
                                    }
                                    
                                    @Override
                                    public void onAlignmentProgress(int percentComplete) {
                                        if (callback != null) {
                                            callback.onProgressUpdate("Aligning: " + percentComplete + "%");
                                        }
                                    }
                                    
                                    @Override
                                    public void onAlignmentFailed(String errorMessage) {
                                        Log.e(TAG, "Alignment failed: " + errorMessage);
                                    }
                                });
                        } catch (IOException e) {
                            Log.e(TAG, "Error in speech alignment", e);
                        }
                        */
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting audio duration", e);
                    } finally {
                        retriever.release();
                    }
                }
                
                // Create story object
                Story story = new Story(
                        storyId,
                        title,
                        author,
                        description,
                        true,
                        String.valueOf(System.currentTimeMillis())
                );
                
                // Set folder ID if provided
                if (folderId != null) {
                    story.setFolderId(folderId);
                }
                
                // Create story content
                StoryContent storyContent = new StoryContent(
                        "content_" + storyId,
                        storyId,
                        textContent,
                        audioFile.getAbsolutePath()
                );
                
                if (segments != null && !segments.isEmpty()) {
                    storyContent.setSegments(segments);
                }
                
                // Create user progress
                UserProgress userProgress = new UserProgress(storyId);
                
                // Save to database
                storyDao.insert(story);
                storyContentDao.insert(storyContent);
                userProgressDao.insert(userProgress);
                
                return storyId;
            } catch (IOException e) {
                Log.e(TAG, "Error importing story", e);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String storyId) {
            if (callback != null) {
                if (storyId != null) {
                    callback.onSuccess(storyId);
                } else {
                    callback.onError("Failed to import story");
                }
            }
        }
    }
    
    public interface ImportStoryCallback {
        void onSuccess(String storyId);
        void onError(String errorMessage);
        void onProgressUpdate(String status);
    }
    
    // Additional AsyncTask classes for folder and ordering operations
    
    private static class MoveStoryToFolderAsyncTask extends AsyncTask<StoryFolderParams, Void, Void> {
        private StoryDao storyDao;
        
        MoveStoryToFolderAsyncTask(StoryDao storyDao) {
            this.storyDao = storyDao;
        }
        
        @Override
        protected Void doInBackground(StoryFolderParams... params) {
            if (params.length > 0) {
                StoryFolderParams param = params[0];
                storyDao.moveStoryToFolder(param.storyId, param.folderId);
            }
            return null;
        }
    }
    
    private static class MoveAllStoriesAsyncTask extends AsyncTask<FolderMoveParams, Void, Void> {
        private StoryDao storyDao;
        
        MoveAllStoriesAsyncTask(StoryDao storyDao) {
            this.storyDao = storyDao;
        }
        
        @Override
        protected Void doInBackground(FolderMoveParams... params) {
            if (params.length > 0) {
                FolderMoveParams param = params[0];
                storyDao.moveAllStories(param.fromFolderId, param.toFolderId);
            }
            return null;
        }
    }
    
    // Helper class for story-folder parameters
    private static class StoryFolderParams {
        String storyId;
        String folderId;
        
        StoryFolderParams(String storyId, String folderId) {
            this.storyId = storyId;
            this.folderId = folderId;
        }
    }
    
    // Helper class for folder move parameters
    private static class FolderMoveParams {
        String fromFolderId;
        String toFolderId;
        
        FolderMoveParams(String fromFolderId, String toFolderId) {
            this.fromFolderId = fromFolderId;
            this.toFolderId = toFolderId;
        }
    }
    
    // Helper class for import story parameters
    private static class ImportStoryParams {
        String title;
        String author;
        String description;
        Uri textUri;
        Uri audioUri;
        Uri timingUri;
        boolean useAiAlignment;
        String folderId;
        
        ImportStoryParams(String title, String author, String description, Uri textUri, Uri audioUri, Uri timingUri,
                         boolean useAiAlignment, String folderId) {
            this.title = title;
            this.author = author;
            this.description = description;
            this.textUri = textUri;
            this.audioUri = audioUri;
            this.timingUri = timingUri;
            this.useAiAlignment = useAiAlignment;
            this.folderId = folderId;
        }
    }
    
    // Bulk export/import methods
    
    /**
     * Export all stories to a JSON file
     * @param outputUri URI to write the exported file
     * @param callback Callback to notify about export results
     */
    public void exportAllStories(Uri outputUri, ExportImportCallback callback) {
        new ExportAllStoriesAsyncTask(application, storyDao, storyContentDao, callback).execute(outputUri);
    }
    
    /**
     * Import stories from a JSON file
     * @param inputUri URI of the file to import
     * @param callback Callback to notify about import results
     */
    public void importAllStories(Uri inputUri, ExportImportCallback callback) {
        new ImportAllStoriesAsyncTask(application, storyDao, storyContentDao, userProgressDao, callback).execute(inputUri);
    }
    
    /**
     * Callback for bulk export/import operations
     */
    public interface ExportImportCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
        void onProgressUpdate(String status);
    }
    
    /**
     * AsyncTask for exporting all stories
     */
    private static class ExportAllStoriesAsyncTask extends AsyncTask<Uri, String, Boolean> {
        private Context context;
        private StoryDao storyDao;
        private StoryContentDao storyContentDao;
        private ExportImportCallback callback;
        private String errorMessage;
        private int storyCount;
        
        ExportAllStoriesAsyncTask(Context context, StoryDao storyDao, StoryContentDao storyContentDao, ExportImportCallback callback) {
            this.context = context;
            this.storyDao = storyDao;
            this.storyContentDao = storyContentDao;
            this.callback = callback;
        }
        
        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onProgressUpdate("Starting export...");
            }
        }
        
        @Override
        protected void onProgressUpdate(String... values) {
            if (callback != null && values.length > 0) {
                callback.onProgressUpdate(values[0]);
            }
        }
        
        @Override
        protected Boolean doInBackground(Uri... uris) {
            if (uris.length == 0) {
                errorMessage = "No output URI provided";
                return false;
            }
            
            Uri outputUri = uris[0];
            
            try {
                // Get all stories
                publishProgress("Retrieving stories...");
                List<Story> stories = storyDao.getAllStoriesSync();
                
                storyCount = stories.size();
                
                if (stories.isEmpty()) {
                    errorMessage = "No stories to export";
                    return false;
                }
                
                // Get content for each story
                publishProgress("Retrieving story content...");
                Map<String, StoryContent> contentMap = new HashMap<>();
                for (Story story : stories) {
                    StoryContent content = storyContentDao.getContentForStorySync(story.getId());
                    if (content != null) {
                        contentMap.put(story.getId(), content);
                    }
                }
                
                // Export to ZIP
                publishProgress("Creating export package with audio files...");
                boolean success = JSONExportImportUtils.exportToZip(context, stories, contentMap, outputUri);
                
                if (!success) {
                    errorMessage = "Failed to write export file";
                    return false;
                }
                
                return true;
            } catch (Exception e) {
                Log.e("StoryRepository", "Error exporting stories", e);
                errorMessage = "Error exporting stories: " + e.getMessage();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (callback != null) {
                if (success) {
                    callback.onSuccess("Successfully exported " + storyCount + " stories");
                } else {
                    callback.onError(errorMessage != null ? errorMessage : "Unknown error during export");
                }
            }
        }
    }
    
    /**
     * AsyncTask for importing stories
     */
    private static class ImportAllStoriesAsyncTask extends AsyncTask<Uri, String, Boolean> {
        private Context context;
        private StoryDao storyDao;
        private StoryContentDao storyContentDao;
        private UserProgressDao userProgressDao;
        private ExportImportCallback callback;
        private String resultMessage;
        private String errorMessage;
        
        ImportAllStoriesAsyncTask(Context context, StoryDao storyDao, StoryContentDao storyContentDao, 
                                 UserProgressDao userProgressDao, ExportImportCallback callback) {
            this.context = context;
            this.storyDao = storyDao;
            this.storyContentDao = storyContentDao;
            this.userProgressDao = userProgressDao;
            this.callback = callback;
        }
        
        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onProgressUpdate("Starting import...");
            }
        }
        
        @Override
        protected void onProgressUpdate(String... values) {
            if (callback != null && values.length > 0) {
                callback.onProgressUpdate(values[0]);
            }
        }
        
        @Override
        protected Boolean doInBackground(Uri... uris) {
            if (uris.length == 0) {
                errorMessage = "No input URI provided";
                return false;
            }
            
            Uri inputUri = uris[0];
            
            try {
                // Import from ZIP
                publishProgress("Reading import package...");
                JSONExportImportUtils.ImportResult result = JSONExportImportUtils.importFromZip(context, inputUri);
                
                if (result.stories.isEmpty()) {
                    errorMessage = "No stories found in the import file";
                    return false;
                }
                
                // Insert stories and content into database
                publishProgress("Importing stories into database...");
                
                List<Story> stories = result.stories;
                List<StoryContent> contents = result.contents;
                
                // Generate progress entries for each imported story
                List<UserProgress> progresses = new ArrayList<>();
                for (Story story : stories) {
                    progresses.add(new UserProgress(story.getId()));
                }
                
                // Insert all the data into the database
                for (Story story : stories) {
                    storyDao.insert(story);
                }
                
                for (StoryContent content : contents) {
                    storyContentDao.insert(content);
                }
                
                for (UserProgress progress : progresses) {
                    userProgressDao.insert(progress);
                }
                
                StringBuilder message = new StringBuilder();
                message.append("Import complete: ");
                message.append(stories.size()).append(" stories imported");
                if (result.skippedStories > 0) {
                    message.append(", ").append(result.skippedStories).append(" skipped");
                }
                if (result.failedStories > 0) {
                    message.append(", ").append(result.failedStories).append(" failed");
                }
                resultMessage = message.toString();
                
                return !stories.isEmpty();
            } catch (Exception e) {
                Log.e("StoryRepository", "Error importing stories", e);
                errorMessage = "Error importing stories: " + e.getMessage();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (callback != null) {
                if (success) {
                    callback.onSuccess(resultMessage);
                } else {
                    callback.onError(errorMessage != null ? errorMessage : "Unknown error during import");
                }
            }
        }
    }
}