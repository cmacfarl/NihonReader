package com.nihonreader.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.nihonreader.app.models.Story;
import com.nihonreader.app.repository.StoryRepository;

import java.util.List;

/**
 * ViewModel for the story list screen
 */
public class StoryListViewModel extends AndroidViewModel {
    
    private StoryRepository repository;
    private LiveData<List<Story>> allStories;
    
    public StoryListViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
        allStories = repository.getAllStories();
    }
    
    public LiveData<List<Story>> getAllStories() {
        return allStories;
    }
    
    public void delete(Story story) {
        repository.delete(story);
    }
    
    public void updateLastOpened(String storyId, String timestamp) {
        repository.updateLastOpened(storyId, timestamp);
    }
}