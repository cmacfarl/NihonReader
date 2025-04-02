package com.nihonreader.app.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nihonreader.app.repository.StoryRepository;

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
    private MutableLiveData<Uri> timingFileUri = new MutableLiveData<>();
    
    private MutableLiveData<String> textFileName = new MutableLiveData<>();
    private MutableLiveData<String> audioFileName = new MutableLiveData<>();
    private MutableLiveData<String> timingFileName = new MutableLiveData<>();
    
    private MutableLiveData<String> textContent = new MutableLiveData<>();
    
    private MutableLiveData<Boolean> isImporting = new MutableLiveData<>(false);
    private MutableLiveData<String> importResult = new MutableLiveData<>();
    
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
    
    public LiveData<Uri> getTimingFileUri() {
        return timingFileUri;
    }
    
    public void setTimingFileUri(Uri timingFileUri) {
        this.timingFileUri.setValue(timingFileUri);
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
    
    public LiveData<String> getTimingFileName() {
        return timingFileName;
    }
    
    public void setTimingFileName(String timingFileName) {
        this.timingFileName.setValue(timingFileName);
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
    
    public void importStory() {
        if (isImporting.getValue() != null && isImporting.getValue()) {
            return;
        }
        
        isImporting.setValue(true);
        
        repository.importCustomStory(
                title.getValue(),
                author.getValue(),
                description.getValue(),
                textFileUri.getValue(),
                audioFileUri.getValue(),
                timingFileUri.getValue(),
                new StoryRepository.ImportStoryCallback() {
                    @Override
                    public void onSuccess(String storyId) {
                        isImporting.postValue(false);
                        importResult.postValue("success");
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        isImporting.postValue(false);
                        importResult.postValue("error: " + errorMessage);
                    }
                }
        );
    }
    
    public boolean validateInputs() {
        return title.getValue() != null && !title.getValue().isEmpty() &&
               author.getValue() != null && !author.getValue().isEmpty() &&
               textFileUri.getValue() != null &&
               audioFileUri.getValue() != null;
    }
}