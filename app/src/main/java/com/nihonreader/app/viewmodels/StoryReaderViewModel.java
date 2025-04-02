package com.nihonreader.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.models.UserProgress;
import com.nihonreader.app.repository.StoryRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ViewModel for the story reader screen
 */
public class StoryReaderViewModel extends AndroidViewModel {
    
    private StoryRepository repository;
    private LiveData<Story> story;
    private LiveData<StoryContent> storyContent;
    private LiveData<UserProgress> userProgress;
    private String storyId;
    
    private MutableLiveData<Integer> currentSegmentIndex = new MutableLiveData<>(-1);
    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    
    public StoryReaderViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
    }
    
    public void loadStory(String storyId) {
        this.storyId = storyId;
        story = repository.getStoryById(storyId);
        storyContent = repository.getContentForStory(storyId);
        userProgress = repository.getProgressForStory(storyId);
        
        // Update last opened timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .format(new Date());
        repository.updateLastOpened(storyId, timestamp);
    }
    
    public LiveData<Story> getStory() {
        return story;
    }
    
    public LiveData<StoryContent> getStoryContent() {
        return storyContent;
    }
    
    public LiveData<UserProgress> getUserProgress() {
        return userProgress;
    }
    
    public void updateUserProgress(UserProgress progress) {
        repository.update(progress);
    }
    
    public void updateAudioPosition(long position) {
        repository.updateAudioPosition(storyId, position);
    }
    
    public LiveData<Integer> getCurrentSegmentIndex() {
        return currentSegmentIndex;
    }
    
    public void setCurrentSegmentIndex(int index) {
        currentSegmentIndex.setValue(index);
    }
    
    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }
    
    public void setIsPlaying(boolean playing) {
        isPlaying.setValue(playing);
    }
    
    public int findCurrentSegment(StoryContent content, long currentTime) {
        if (content == null || content.getSegments() == null || content.getSegments().isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < content.getSegments().size(); i++) {
            AudioSegment segment = content.getSegments().get(i);
            if (currentTime >= segment.getStart() && currentTime < segment.getEnd()) {
                return i;
            }
        }
        
        return -1;
    }
}