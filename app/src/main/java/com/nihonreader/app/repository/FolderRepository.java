package com.nihonreader.app.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.nihonreader.app.database.AppDatabase;
import com.nihonreader.app.database.FolderDao;
import com.nihonreader.app.models.Folder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Folder operations
 */
public class FolderRepository {
    
    private final FolderDao folderDao;
    private final LiveData<List<Folder>> allFolders;
    private final LiveData<Folder> defaultFolder;
    private final ExecutorService executorService;
    
    public FolderRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        folderDao = database.folderDao();
        allFolders = folderDao.getAllFolders();
        defaultFolder = folderDao.getDefaultFolder();
        executorService = Executors.newSingleThreadExecutor();
    }
    
    public LiveData<List<Folder>> getAllFolders() {
        return allFolders;
    }
    
    public LiveData<Folder> getDefaultFolder() {
        return defaultFolder;
    }
    
    public LiveData<Folder> getFolderById(String folderId) {
        return folderDao.getFolderById(folderId);
    }
    
    public void insert(Folder folder) {
        executorService.execute(() -> {
            // Get the max position to determine where to insert
            int maxPosition = folderDao.getMaxPosition();
            
            // If the new folder should be inserted at a specific position
            if (folder.getPosition() < maxPosition) {
                folderDao.shiftFolderPositionsForInsert(folder.getPosition());
            } else {
                // Otherwise, just add it at the end
                folder.setPosition(maxPosition + 1);
            }
            
            folderDao.insert(folder);
        });
    }
    
    public void update(Folder folder) {
        executorService.execute(() -> folderDao.update(folder));
    }
    
    public void delete(Folder folder) {
        executorService.execute(() -> {
            folderDao.delete(folder);
            folderDao.shiftFolderPositionsAfterDelete(folder.getPosition());
        });
    }
    
    public void reorderFolders(List<Folder> folders) {
        executorService.execute(() -> folderDao.reorderFolders(folders));
    }
} 