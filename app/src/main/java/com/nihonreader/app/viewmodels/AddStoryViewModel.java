package com.nihonreader.app.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nihonreader.app.repository.StoryRepository;
import com.nihonreader.app.utils.JapaneseTextUtils;
import com.nihonreader.app.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * ViewModel for the add story screen
 */
public class AddStoryViewModel extends AndroidViewModel {
    
    private StoryRepository repository;
    
    private MutableLiveData<String> title = new MutableLiveData<>("");
    private MutableLiveData<String> author = new MutableLiveData<>("");
    private MutableLiveData<String> description = new MutableLiveData<>("");
    
    private MutableLiveData<Uri> textFileUri = new MutableLiveData<>();
    private MutableLiveData<Uri> audioFileUri = new MutableLiveData<>();
    
    private MutableLiveData<String> textFileName = new MutableLiveData<>();
    private MutableLiveData<String> audioFileName = new MutableLiveData<>();
    
    private MutableLiveData<String> textContent = new MutableLiveData<>();
    
    private MutableLiveData<Boolean> isImporting = new MutableLiveData<>(false);
    private MutableLiveData<String> importResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> useAiAlignment = new MutableLiveData<>(false);
    private MutableLiveData<String> importStatus = new MutableLiveData<>();
    
    public AddStoryViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
    }
    
    public LiveData<String> getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title.setValue(title);
    }
    
    public LiveData<String> getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author.setValue(author);
    }
    
    public LiveData<String> getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description.setValue(description);
    }
    
    public LiveData<Uri> getTextFileUri() {
        return textFileUri;
    }
    
    public void setTextFileUri(Uri textFileUri) {
        this.textFileUri.setValue(textFileUri);
    }
    
    public LiveData<Uri> getAudioFileUri() {
        return audioFileUri;
    }
    
    public void setAudioFileUri(Uri audioFileUri) {
        this.audioFileUri.setValue(audioFileUri);
    }
    
    public LiveData<String> getTextFileName() {
        return textFileName;
    }
    
    public void setTextFileName(String textFileName) {
        this.textFileName.setValue(textFileName);
    }
    
    public LiveData<String> getAudioFileName() {
        return audioFileName;
    }
    
    public void setAudioFileName(String audioFileName) {
        this.audioFileName.setValue(audioFileName);
    }
    
    public LiveData<String> getTextContent() {
        return textContent;
    }
    
    public void setTextContent(String textContent) {
        this.textContent.setValue(textContent);
    }
    
    public LiveData<Boolean> getIsImporting() {
        return isImporting;
    }
    
    public LiveData<String> getImportResult() {
        return importResult;
    }
    
    public LiveData<Boolean> getUseAiAlignment() {
        return useAiAlignment;
    }
    
    public void setUseAiAlignment(boolean useAi) {
        useAiAlignment.setValue(useAi);
    }
    
    public LiveData<String> getImportStatus() {
        return importStatus;
    }
    
    public void importStory(StoryRepository.ImportStoryCallback callback) {
        if (!validateInputs()) {
            callback.onError("Please fill in all required fields");
            return;
        }

        // Get the text content from the text file
        String textContent = null;
        try {
            textContent = FileUtils.readTextFromUri(getApplication(), textFileUri.getValue());
        } catch (IOException e) {
            callback.onError("Failed to read text file: " + e.getMessage());
            return;
        }

        // Split text into sentences using JapaneseTextUtils
        List<String> sentences = JapaneseTextUtils.splitIntoSentences(textContent);

        // Create a temporary file for the timestamps
        File tempFile = null;
        try {
            tempFile = File.createTempFile("timestamps_", ".txt", getApplication().getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

            // Write timestamps in the LRC format expected by AudioUtils.parseTimingFile
            // Format: [MM:SS.CC] Text
            long currentTime = 0;
            for (String sentence : sentences) {
                // Assume each sentence takes about 3 seconds to read
                int minutes = (int) (currentTime / 60000);
                int seconds = (int) ((currentTime % 60000) / 1000);
                int centis = (int) ((currentTime % 1000) / 10);
                
                // Format: [MM:SS.CC] Text
                writer.write(String.format("[%02d:%02d.%02d] %s\n", minutes, seconds, centis, sentence));
                
                // Increment current time for next sentence
                currentTime += 3000;
            }
            writer.close();

            // Create a Uri from the temporary file
            Uri timingUri = Uri.fromFile(tempFile);

            // Call repository to import the story
            repository.importCustomStory(
                title.getValue(),
                author.getValue(),
                description.getValue(),
                textFileUri.getValue(),
                audioFileUri.getValue(),
                timingUri,
                useAiAlignment.getValue(),
                callback
            );
        } catch (IOException e) {
            callback.onError("Failed to create timestamp file: " + e.getMessage());
            return;
        }
    }
    
    public boolean validateInputs() {
        boolean isTitleValid = title.getValue() != null && !title.getValue().isEmpty();
        boolean isAuthorValid = author.getValue() != null && !author.getValue().isEmpty();
        boolean isTextFileValid = textFileUri.getValue() != null;
        boolean isAudioFileValid = audioFileUri.getValue() != null;

        return isTitleValid && isAuthorValid && isTextFileValid && isAudioFileValid;
    }
}