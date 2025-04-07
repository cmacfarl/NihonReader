package com.nihonreader.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nihonreader.app.activities.AddStoryActivity;
import com.nihonreader.app.activities.EditTimestampsActivity;
import com.nihonreader.app.activities.StoryReaderActivity;
import com.nihonreader.app.adapters.FolderSpinnerAdapter;
import com.nihonreader.app.adapters.StoryAdapter;
import com.nihonreader.app.models.Folder;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.repository.StoryRepository;
import com.nihonreader.app.utils.FileUtils;
import com.nihonreader.app.viewmodels.FolderViewModel;
import com.nihonreader.app.viewmodels.StoryListViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main activity that displays the list of available stories
 */
public class MainActivity extends AppCompatActivity implements 
        StoryAdapter.OnStoryClickListener {
    
    private StoryListViewModel storyViewModel;
    private FolderViewModel folderViewModel;
    private RecyclerView recyclerViewStories;
    private Spinner spinnerFolders;
    private StoryAdapter storyAdapter;
    private FolderSpinnerAdapter folderSpinnerAdapter;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    
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
        spinnerFolders = findViewById(R.id.spinner_folders);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
        
        // Setup story recycler view
        recyclerViewStories.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewStories.setHasFixedSize(true);
        
        // Setup adapters
        storyAdapter = new StoryAdapter(this);
        recyclerViewStories.setAdapter(storyAdapter);
        
        // Setup view models
        storyViewModel = new ViewModelProvider(this).get(StoryListViewModel.class);
        folderViewModel = new ViewModelProvider(this).get(FolderViewModel.class);
        
        // Observe folders
        folderViewModel.getAllFolders().observe(this, folders -> {
            if (folders != null) {
                // Create a list with "All Stories" option
                List<Folder> allFolders = new ArrayList<>();
                allFolders.add(new Folder("", getString(R.string.all_stories), -1, true));
                allFolders.addAll(folders);
                
                // Update spinner adapter
                folderSpinnerAdapter = new FolderSpinnerAdapter(this, allFolders);
                spinnerFolders.setAdapter(folderSpinnerAdapter);
                
                // Set selection based on current folder
                if (currentFolderId == null) {
                    spinnerFolders.setSelection(0);
                } else {
                    for (int i = 1; i < allFolders.size(); i++) {
                        if (allFolders.get(i).getId().equals(currentFolderId)) {
                            spinnerFolders.setSelection(i);
                            break;
                        }
                    }
                }
                
                updateStoryCounts();
            }
            
            // If we don't have a current folder set, load all stories
            if (currentFolderId == null) {
                loadAllStories();
            }
        });
        
        // Setup spinner selection listener
        spinnerFolders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // "All Stories" selected
                    loadAllStories();
                } else {
                    // Folder selected
                    Folder selectedFolder = (Folder) parent.getItemAtPosition(position);
                    loadStoriesInFolder(selectedFolder.getId());
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not used
            }
        });
        
        // Setup FAB
        FloatingActionButton fab = findViewById(R.id.fab_add_story);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddStoryActivity.class);
            // Pass the currently selected folder ID if not "All Stories"
            if (currentFolderId != null) {
                intent.putExtra(AddStoryActivity.EXTRA_FOLDER_ID, currentFolderId);
            }
            startActivity(intent);
        });
        
        // Create app directories
        FileUtils.createAppDirectories(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        // Set menu item text color to white
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(item.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.white)), 
                            0, spanString.length(), 0);
            item.setTitle(spanString);
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_create_folder) {
            showFolderDialog(null);
            return true;
        } else if (id == R.id.action_edit_folder) {
            // Get the currently selected folder
            int position = spinnerFolders.getSelectedItemPosition();
            if (position > 0) { // Skip "All Stories" option (position 0)
                Folder selectedFolder = (Folder) spinnerFolders.getItemAtPosition(position);
                showFolderDialog(selectedFolder);
            } else {
                Toast.makeText(this, R.string.cannot_edit_all_stories_folder, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_export_stories) {
            exportStories();
            return true;
        } else if (id == R.id.action_import_stories) {
            importStories();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
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
    
    @Override
    public void onMoveStoryClick(Story story) {
        // Create an AlertDialog with a list of folders
        folderViewModel.getAllFolders().observe(this, folders -> {
            if (folders == null || folders.isEmpty()) {
                showCreateFolderDialog(null, story);
                return;
            }
            
            List<String> folderNames = new ArrayList<>();
            List<String> folderIds = new ArrayList<>();
            
            for (Folder folder : folders) {
                folderNames.add(folder.getName());
                folderIds.add(folder.getId());
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.move_to_folder));
            
            // Add "No Folder" option
            folderNames.add(0, getString(R.string.no_folder));
            folderIds.add(0, "");
            
            // Add "Create New Folder" option
            folderNames.add(getString(R.string.create_new_folder));
            folderIds.add("");
            
            builder.setItems(folderNames.toArray(new String[0]), (dialog, which) -> {
                if (which == folderNames.size() - 1) {
                    // "Create New Folder" selected
                    showCreateFolderDialog(null, story);
                } else {
                    String selectedFolderId = folderIds.get(which);
                    // Move story to selected folder
                    folderViewModel.moveStoryToFolder(story.getId(), selectedFolderId);
                }
            });
            
            builder.show();
        });
    }
    
    private void loadAllStories() {
        currentFolderId = null;
        
        storyViewModel.getAllStories().observe(this, stories -> {
            progressBar.setVisibility(View.GONE);
            
            if (stories != null && !stories.isEmpty()) {
                currentStories = new ArrayList<>(stories);
                storyAdapter.setStories(stories);
                recyclerViewStories.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                
                // Update "All Stories" count
                if (folderSpinnerAdapter != null && folderSpinnerAdapter.getCount() > 0) {
                    Folder allStoriesFolder = folderSpinnerAdapter.getItem(0);
                    if (allStoriesFolder != null) {
                        allStoriesFolder.setStoryCount(stories.size());
                        folderSpinnerAdapter.notifyDataSetChanged();
                    }
                }
            } else {
                currentStories.clear();
                recyclerViewStories.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                
                // Update "All Stories" count to 0
                if (folderSpinnerAdapter != null && folderSpinnerAdapter.getCount() > 0) {
                    Folder allStoriesFolder = folderSpinnerAdapter.getItem(0);
                    if (allStoriesFolder != null) {
                        allStoriesFolder.setStoryCount(0);
                        folderSpinnerAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }
    
    private void loadStoriesInFolder(String folderId) {
        currentFolderId = folderId;
        
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
                    folder.setStoryCount(storyCount);
                    if (folderSpinnerAdapter != null) {
                        folderSpinnerAdapter.notifyDataSetChanged();
                    }
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
    
    private void showCreateFolderDialog(Folder parentFolder, Story storyToMove) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_folder, null);
        EditText editFolderName = view.findViewById(R.id.editFolderName);
        
        builder.setTitle(R.string.create_folder);
        builder.setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String folderName = editFolderName.getText().toString().trim();
                    
                    if (folderName.isEmpty()) {
                        Toast.makeText(this, R.string.folder_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Create new folder
                    String newFolderId = UUID.randomUUID().toString();
                    folderViewModel.createFolder(folderName, 0);
                    
                    // Move story to the new folder if provided
                    if (storyToMove != null) {
                        // We need to wait a moment for the folder to be created and get its ID
                        folderViewModel.getAllFolders().observe(this, folders -> {
                            if (folders != null) {
                                for (Folder folder : folders) {
                                    if (folder.getName().equals(folderName)) {
                                        folderViewModel.moveStoryToFolder(storyToMove.getId(), folder.getId());
                                        break;
                                    }
                                }
                            }
                        });
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
    
    private static final int REQUEST_EXPORT_STORIES = 1001;
    private static final int REQUEST_IMPORT_STORIES = 1002;
    
    /**
     * Initiates the process to export all stories
     */
    private void exportStories() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "nihon_reader_stories_export.zip");
        startActivityForResult(intent, REQUEST_EXPORT_STORIES);
    }
    
    /**
     * Initiates the process to import stories
     */
    private void importStories() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, REQUEST_IMPORT_STORIES);
    }
    
    /**
     * Shows a progress dialog with the given message
     */
    private ProgressDialog showProgressDialog(String message) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            
            if (requestCode == REQUEST_EXPORT_STORIES) {
                // Show progress dialog
                ProgressDialog progressDialog = showProgressDialog(getString(R.string.exporting));
                
                // Create repository instance
                StoryRepository repository = new StoryRepository(getApplication());
                
                // Export stories
                repository.exportAllStories(uri, new StoryRepository.ExportImportCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            // Dismiss progress dialog
                            progressDialog.dismiss();
                            
                            // Show success toast
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            // Dismiss progress dialog
                            progressDialog.dismiss();
                            
                            // Show error toast
                            Toast.makeText(MainActivity.this, 
                                         getString(R.string.export_error) + ": " + errorMessage, 
                                         Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    @Override
                    public void onProgressUpdate(String status) {
                        runOnUiThread(() -> {
                            // Update progress dialog message
                            progressDialog.setMessage(status);
                        });
                    }
                });
            } else if (requestCode == REQUEST_IMPORT_STORIES) {
                // Show progress dialog
                ProgressDialog progressDialog = showProgressDialog(getString(R.string.importing));
                
                // Create repository instance
                StoryRepository repository = new StoryRepository(getApplication());
                
                // Import stories
                repository.importAllStories(uri, new StoryRepository.ExportImportCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            // Dismiss progress dialog
                            progressDialog.dismiss();
                            
                            // Show success toast
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                            
                            // Reload stories
                            if (currentFolderId == null) {
                                loadAllStories();
                            } else {
                                loadStoriesInFolder(currentFolderId);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            // Dismiss progress dialog
                            progressDialog.dismiss();
                            
                            // Show error toast
                            Toast.makeText(MainActivity.this, 
                                         getString(R.string.import_error) + ": " + errorMessage, 
                                         Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    @Override
                    public void onProgressUpdate(String status) {
                        runOnUiThread(() -> {
                            // Update progress dialog message
                            progressDialog.setMessage(status);
                        });
                    }
                });
            }
        }
    }
}