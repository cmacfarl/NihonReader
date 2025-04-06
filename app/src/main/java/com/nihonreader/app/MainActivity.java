package com.nihonreader.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nihonreader.app.activities.AddStoryActivity;
import com.nihonreader.app.activities.EditTimestampsActivity;
import com.nihonreader.app.activities.StoryReaderActivity;
import com.nihonreader.app.adapters.StoryAdapter;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.utils.FileUtils;
import com.nihonreader.app.viewmodels.StoryListViewModel;

/**
 * Main activity that displays the list of available stories
 */
public class MainActivity extends AppCompatActivity implements StoryAdapter.OnStoryClickListener {
    
    private StoryListViewModel viewModel;
    private RecyclerView recyclerView;
    private StoryAdapter adapter;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Setup views
        recyclerView = findViewById(R.id.recycler_view_stories);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress_bar);
        
        // Setup recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        
        // Setup adapter
        adapter = new StoryAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Setup view model
        viewModel = new ViewModelProvider(this).get(StoryListViewModel.class);
        viewModel.getAllStories().observe(this, stories -> {
            progressBar.setVisibility(View.GONE);
            
            if (stories != null && !stories.isEmpty()) {
                adapter.setStories(stories);
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
        
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
    public void onStoryClick(Story story) {
        // Update last opened timestamp
        viewModel.updateLastOpened(story.getId(), String.valueOf(System.currentTimeMillis()));
        
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
    
    private void showDeleteConfirmationDialog(Story story) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_story)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // Delete the story
                    viewModel.delete(story);
                    Toast.makeText(MainActivity.this, R.string.story_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}