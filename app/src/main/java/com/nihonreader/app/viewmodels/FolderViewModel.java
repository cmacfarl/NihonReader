package com.nihonreader.app.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.nihonreader.app.models.Folder;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.repository.FolderRepository;
import com.nihonreader.app.repository.StoryRepository;

import java.util.List;
import java.util.UUID;

/**
 * ViewModel for folder operations
 */
public class FolderViewModel extends AndroidViewModel {
    
    private FolderRepository folderRepository;
    private StoryRepository storyRepository;
    private LiveData<List<Folder>> allFolders;
    private LiveData<Folder> defaultFolder;
    
    public FolderViewModel(@NonNull Application application) {
        super(application);
        folderRepository = new FolderRepository(application);
        storyRepository = new StoryRepository(application);
        allFolders = folderRepository.getAllFolders();
        defaultFolder = folderRepository.getDefaultFolder();
    }
    
    public LiveData<List<Folder>> getAllFolders() {
        return allFolders;
    }
    
    public LiveData<Folder> getDefaultFolder() {
        return defaultFolder;
    }
    
    public LiveData<Folder> getFolderById(String folderId) {
        return folderRepository.getFolderById(folderId);
    }
    
    public LiveData<List<Story>> getStoriesInFolder(String folderId) {
        return storyRepository.getStoriesInFolder(folderId);
    }
    
    public LiveData<List<Story>> getStoriesWithoutFolder() {
        return storyRepository.getStoriesWithoutFolder();
    }
    
    public void createFolder(String name, int position) {
        Folder folder = new Folder(UUID.randomUUID().toString(), name, position, false);
        folderRepository.insert(folder);
    }
    
    public void updateFolder(Folder folder) {
        folderRepository.update(folder);
    }
    
    public void deleteFolder(Folder folder) {
        folderRepository.delete(folder);
    }
    
    public void moveStoryToFolder(String storyId, String folderId, int position) {
        storyRepository.moveStoryToFolder(storyId, folderId, position);
    }
    
    public void updateStoryPosition(String storyId, int position) {
        storyRepository.updateStoryPosition(storyId, position);
    }
    
    public void reorderStories(List<Story> stories) {
        storyRepository.reorderStories(stories);
    }
} 