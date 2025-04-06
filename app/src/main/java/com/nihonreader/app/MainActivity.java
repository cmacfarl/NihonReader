package com.nihonreader.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nihonreader.app.activities.AddStoryActivity;
import com.nihonreader.app.activities.EditTimestampsActivity;
import com.nihonreader.app.activities.StoryReaderActivity;
import com.nihonreader.app.adapters.FolderAdapter;
import com.nihonreader.app.adapters.StoryAdapter;
import com.nihonreader.app.models.Folder;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.utils.FileUtils;
import com.nihonreader.app.viewmodels.FolderViewModel;
import com.nihonreader.app.viewmodels.StoryListViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main activity that displays the list of available stories
 */
public class MainActivity extends AppCompatActivity implements 
        StoryAdapter.OnStoryClickListener, 
        FolderAdapter.OnFolderClickListener {
    
    private StoryListViewModel storyViewModel;
    private FolderViewModel folderViewModel;
    private RecyclerView recyclerViewStories;
    private RecyclerView recyclerViewFolders;
    private StoryAdapter storyAdapter;
    private FolderAdapter folderAdapter;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView currentFolderTitle;
    
    private String currentFolderId = null; // null means "All Stories"
    private List<Story> currentStories = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Setup views
        recyclerViewStories = findViewById(R.id.recycler_view_stories);
        recyclerViewFolders = findViewById(R.id.recycler_view_folders);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
        currentFolderTitle = findViewById(R.id.current_folder_title);
        
        // Setup story recycler view
        recyclerViewStories.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewStories.setHasFixedSize(true);
        
        // Setup folder recycler view
        LinearLayoutManager folderLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerViewFolders.setLayoutManager(folderLayoutManager);
        
        // Setup adapters
        storyAdapter = new StoryAdapter(this);
        recyclerViewStories.setAdapter(storyAdapter);
        
        folderAdapter = new FolderAdapter(this, this);
        recyclerViewFolders.setAdapter(folderAdapter);
        
        // Setup view models
        storyViewModel = new ViewModelProvider(this).get(StoryListViewModel.class);
        folderViewModel = new ViewModelProvider(this).get(FolderViewModel.class);
        
        // Observe folders
        folderViewModel.getAllFolders().observe(this, folders -> {
            if (folders != null) {
                folderAdapter.setFolders(folders);
                updateStoryCounts();
            }
            
            // If we don't have a current folder set, load all stories
            if (currentFolderId == null) {
                loadAllStories();
            }
        });
        
        // Setup drag and drop for story reordering
        setupItemTouchHelper();
        
        // Setup FAB
        FloatingActionButton fab = findViewById(R.id.fab_add_story);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddStoryActivity.class);
            startActivity(intent);
        });
        
        // Create app directories
        FileUtils.createAppDirectories(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_create_folder) {
            showFolderDialog(null);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void setupItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                  @NonNull RecyclerView.ViewHolder viewHolder, 
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(currentStories, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(currentStories, i, i - 1);
                    }
                }
                
                storyAdapter.notifyItemMoved(fromPosition, toPosition);
                
                // Update positions in the database
                updateStoryPositions();
                
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used
            }
        };
        
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewStories);
    }
    
    private void updateStoryPositions() {
        for (int i = 0; i < currentStories.size(); i++) {
            Story story = currentStories.get(i);
            story.setPosition(i);
        }
        
        folderViewModel.reorderStories(currentStories);
    }
    
    private void showFolderDialog(Folder folder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_folder, null);
        EditText editFolderName = view.findViewById(R.id.editFolderName);
        
        if (folder != null) {
            builder.setTitle(R.string.edit_folder);
            editFolderName.setText(folder.getName());
        } else {
            builder.setTitle(R.string.create_folder);
        }
        
        builder.setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String folderName = editFolderName.getText().toString().trim();
                    
                    if (folderName.isEmpty()) {
                        Toast.makeText(this, R.string.folder_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (folder == null) {
                        // Create new folder
                        folderViewModel.createFolder(folderName, 0);
                    } else {
                        // Update existing folder
                        folder.setName(folderName);
                        folderViewModel.updateFolder(folder);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showDeleteFolderConfirmationDialog(Folder folder) {
        if (folder.isDefaultFolder()) {
            Toast.makeText(this, R.string.cannot_delete_default_folder, Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_folder)
                .setMessage(R.string.confirm_delete_folder)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // Delete the folder
                    folderViewModel.deleteFolder(folder);
                    
                    // If we were viewing this folder, go back to all stories
                    if (folder.getId().equals(currentFolderId)) {
                        currentFolderId = null;
                        loadAllStories();
                    }
                    
                    Toast.makeText(MainActivity.this, R.string.folder_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showMoveToFolderDialog(Story story) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.move_to_folder);
        
        folderViewModel.getAllFolders().observe(this, folders -> {
            if (folders == null || folders.isEmpty()) {
                return;
            }
            
            String[] folderNames = new String[folders.size() + 1];
            String[] folderIds = new String[folders.size() + 1];
            
            folderNames[0] = getString(R.string.all_stories);
            folderIds[0] = null;
            
            for (int i = 0; i < folders.size(); i++) {
                folderNames[i + 1] = folders.get(i).getName();
                folderIds[i + 1] = folders.get(i).getId();
            }
            
            int checkedItem = 0;
            String storyFolderId = story.getFolderId();
            
            if (storyFolderId != null) {
                for (int i = 1; i < folderIds.length; i++) {
                    if (storyFolderId.equals(folderIds[i])) {
                        checkedItem = i;
                        break;
                    }
                }
            }
            
            builder.setSingleChoiceItems(folderNames, checkedItem, (dialog, which) -> {
                String targetFolderId = folderIds[which];
                
                if ((storyFolderId == null && targetFolderId != null) || 
                    (storyFolderId != null && !storyFolderId.equals(targetFolderId))) {
                    // Move story to the selected folder
                    folderViewModel.moveStoryToFolder(story.getId(), targetFolderId, 0);
                    
                    // If we're viewing a specific folder, refresh the list
                    if (currentFolderId != null) {
                        loadStoriesInFolder(currentFolderId);
                    } else {
                        loadAllStories();
                    }
                }
                
                dialog.dismiss();
            });
            
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        });
    }
    
    private void loadAllStories() {
        currentFolderId = null;
        currentFolderTitle.setText(R.string.all_stories);
        
        storyViewModel.getAllStories().observe(this, stories -> {
            progressBar.setVisibility(View.GONE);
            
            if (stories != null && !stories.isEmpty()) {
                currentStories = new ArrayList<>(stories);
                storyAdapter.setStories(stories);
                recyclerViewStories.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                currentStories.clear();
                recyclerViewStories.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void loadStoriesInFolder(String folderId) {
        currentFolderId = folderId;
        
        folderViewModel.getFolderById(folderId).observe(this, folder -> {
            if (folder != null) {
                currentFolderTitle.setText(folder.getName());
            }
        });
        
        folderViewModel.getStoriesInFolder(folderId).observe(this, stories -> {
            progressBar.setVisibility(View.GONE);
            
            if (stories != null && !stories.isEmpty()) {
                currentStories = new ArrayList<>(stories);
                storyAdapter.setStories(stories);
                recyclerViewStories.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                currentStories.clear();
                recyclerViewStories.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void updateStoryCounts() {
        folderViewModel.getAllFolders().observe(this, folders -> {
            if (folders == null) return;
            
            // Count stories in each folder
            for (Folder folder : folders) {
                folderViewModel.getStoriesInFolder(folder.getId()).observe(this, stories -> {
                    int storyCount = stories != null ? stories.size() : 0;
                    folderAdapter.updateStoryCounts();
                });
            }
        });
    }
    
    @Override
    public void onStoryClick(Story story) {
        // Update last opened timestamp
        storyViewModel.updateLastOpened(story.getId(), String.valueOf(System.currentTimeMillis()));
        
        // Open story reader
        Intent intent = new Intent(MainActivity.this, StoryReaderActivity.class);
        intent.putExtra(StoryReaderActivity.EXTRA_STORY_ID, story.getId());
        startActivity(intent);
    }
    
    @Override
    public void onEditTimestampsClick(Story story) {
        // Launch the EditTimestampsActivity
        Intent intent = new Intent(MainActivity.this, EditTimestampsActivity.class);
        intent.putExtra(EditTimestampsActivity.EXTRA_STORY_ID, story.getId());
        startActivity(intent);
    }
    
    @Override
    public void onDeleteStoryClick(Story story) {
        showDeleteConfirmationDialog(story);
    }
    
    @Override
    public void onMoveStoryClick(Story story) {
        showMoveToFolderDialog(story);
    }
    
    private void showDeleteConfirmationDialog(Story story) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_story)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // Delete the story
                    storyViewModel.delete(story);
                    Toast.makeText(MainActivity.this, R.string.story_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    @Override
    public void onFolderClick(Folder folder) {
        loadStoriesInFolder(folder.getId());
    }
    
    @Override
    public void onAllStoriesClick() {
        loadAllStories();
    }
    
    @Override
    public void onEditFolderClick(Folder folder) {
        showFolderDialog(folder);
    }
    
    @Override
    public void onDeleteFolderClick(Folder folder) {
        showDeleteFolderConfirmationDialog(folder);
    }
}