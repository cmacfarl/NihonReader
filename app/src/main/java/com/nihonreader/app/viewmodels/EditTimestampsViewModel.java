package com.nihonreader.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.repository.StoryRepository;

/**
 * ViewModel for EditTimestampsActivity
 */
public class EditTimestampsViewModel extends AndroidViewModel {
    
    private StoryRepository repository;
    private LiveData<Story> story;
    private LiveData<StoryContent> storyContent;
    private MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    
    public EditTimestampsViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
    }
    
    public void loadStory(String storyId) {
        story = repository.getStoryById(storyId);
        storyContent = repository.getContentForStory(storyId);
    }
    
    public LiveData<Story> getStory() {
        return story;
    }
    
    public LiveData<StoryContent> getStoryContent() {
        return storyContent;
    }
    
    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }
    
    public void saveStoryContent(StoryContent content) {
        isSaving.setValue(true);
        repository.update(content);
        isSaving.setValue(false);
    }
}