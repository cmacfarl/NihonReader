package com.nihonreader.app.activities;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nihonreader.app.R;
import com.nihonreader.app.adapters.TimestampAdapter;
import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.utils.AudioUtils;
import com.nihonreader.app.viewmodels.EditTimestampsViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for editing audio segment timestamps
 */
public class EditTimestampsActivity extends AppCompatActivity implements TimestampAdapter.OnTimestampEditListener {
    
    public static final String EXTRA_STORY_ID = "com.nihonreader.app.EXTRA_STORY_ID";
    
    private EditTimestampsViewModel viewModel;
    private RecyclerView recyclerView;
    private TimestampAdapter adapter;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private TextView textViewCurrentTime;
    private TextView textViewTotalTime;
    private ImageButton buttonPlayPause;
    private ImageButton buttonSkipBackward;
    private ImageButton buttonSkipForward;
    private FloatingActionButton fabSaveTimestamps;
    
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBarRunnable;
    
    private StoryContent storyContent;
    private List<AudioSegment> originalSegments = new ArrayList<>();
    private boolean isModified = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_timestamps);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Setup views
        recyclerView = findViewById(R.id.recycler_view_timestamps);
        progressBar = findViewById(R.id.progress_bar);
        seekBar = findViewById(R.id.seek_bar);
        textViewCurrentTime = findViewById(R.id.text_view_current_time);
        textViewTotalTime = findViewById(R.id.text_view_total_time);
        buttonPlayPause = findViewById(R.id.button_play_pause);
        buttonSkipBackward = findViewById(R.id.button_skip_backward);
        buttonSkipForward = findViewById(R.id.button_skip_forward);
        fabSaveTimestamps = findViewById(R.id.fab_save_timestamps);
        
        // Get story ID from intent
        String storyId = getIntent().getStringExtra(EXTRA_STORY_ID);
        if (storyId == null) {
            Toast.makeText(this, "Error: No story ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Setup view model
        viewModel = new ViewModelProvider(this).get(EditTimestampsViewModel.class);
        viewModel.loadStory(storyId);
        
        // Setup recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // We'll initialize the adapter after MediaPlayer is ready
        
        // Initialize handler for seeking
        handler = new Handler(Looper.getMainLooper());
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                updateSeekBarProgress();
                handler.postDelayed(this, 100);
            }
        };
        
        // Observe story content
        viewModel.getStoryContent().observe(this, content -> {
            if (content != null) {
                storyContent = content;
                
                // Make a deep copy of the segments to avoid modifying the original list
                originalSegments.clear();
                for (AudioSegment segment : content.getSegments()) {
                    originalSegments.add(new AudioSegment(
                            segment.getStart(),
                            segment.getEnd(),
                            segment.getText()
                    ));
                }
                
                // Initialize media player
                initializeMediaPlayer(content.getAudioUri());
            }
        });
        
        // Setup click listeners
        buttonPlayPause.setOnClickListener(v -> togglePlayback());
        buttonSkipBackward.setOnClickListener(v -> skipBackward());
        buttonSkipForward.setOnClickListener(v -> skipForward());
        
        // Setup seek bar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateSeekBarProgress();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopUpdatingSeekBar();
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    startUpdatingSeekBar();
                }
            }
        });
        
        // Save button
        fabSaveTimestamps.setOnClickListener(v -> saveTimestamps());
        
        // Show progress while loading
        progressBar.setVisibility(View.VISIBLE);
    }
    
    private void initializeMediaPlayer(String audioUri) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.setDataSource(audioUri);
            mediaPlayer.prepare();
            
            // Set up seek bar
            seekBar.setMax(mediaPlayer.getDuration());
            textViewTotalTime.setText(AudioUtils.formatTime(mediaPlayer.getDuration()));
            
            // Set up completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                mediaPlayer.seekTo(0);
                updateSeekBarProgress();
                stopUpdatingSeekBar();
            });
            
            // Now we can initialize the adapter
            adapter = new TimestampAdapter(mediaPlayer, this);
            recyclerView.setAdapter(adapter);
            adapter.setSegments(originalSegments);
            
            // Hide progress
            progressBar.setVisibility(View.GONE);
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void togglePlayback() {
        if (mediaPlayer == null) {
            return;
        }
        
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            buttonPlayPause.setImageResource(android.R.drawable.ic_media_play);
            stopUpdatingSeekBar();
        } else {
            mediaPlayer.start();
            buttonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            startUpdatingSeekBar();
        }
    }
    
    private void skipBackward() {
        if (mediaPlayer == null) {
            return;
        }
        
        int newPosition = Math.max(0, mediaPlayer.getCurrentPosition() - 5000);
        mediaPlayer.seekTo(newPosition);
        updateSeekBarProgress();
    }
    
    private void skipForward() {
        if (mediaPlayer == null) {
            return;
        }
        
        int newPosition = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 5000);
        mediaPlayer.seekTo(newPosition);
        updateSeekBarProgress();
    }
    
    private void updateSeekBarProgress() {
        if (mediaPlayer == null) {
            return;
        }
        
        int currentPosition = mediaPlayer.getCurrentPosition();
        seekBar.setProgress(currentPosition);
        textViewCurrentTime.setText(AudioUtils.formatTime(currentPosition));
        
        // Update listeners
        onPlaybackPositionChanged(currentPosition);
    }
    
    private void startUpdatingSeekBar() {
        handler.post(updateSeekBarRunnable);
    }
    
    private void stopUpdatingSeekBar() {
        handler.removeCallbacks(updateSeekBarRunnable);
    }
    
    private void saveTimestamps() {
        if (storyContent != null && adapter != null) {
            List<AudioSegment> editedSegments = adapter.getSegments();
            storyContent.setSegments(editedSegments);
            viewModel.saveStoryContent(storyContent);
            
            Toast.makeText(this, R.string.timestamps_saved, Toast.LENGTH_SHORT).show();
            isModified = false;
            
            // Optionally finish the activity
            finish();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button
            if (isModified) {
                // Show confirmation dialog if there are unsaved changes
                // For simplicity, we'll just finish for now
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            buttonPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
        stopUpdatingSeekBar();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }
    
    // TimestampAdapter.OnTimestampEditListener methods
    
    @Override
    public void onStartTimeSet(int position, long timestamp) {
        isModified = true;
        // No need to update adapter since it's already updated through binding
    }
    
    @Override
    public void onEndTimeSet(int position, long timestamp) {
        isModified = true;
        // No need to update adapter since it's already updated through binding
    }
    
    @Override
    public void onPlaySegment(int position, long startTime, long endTime) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int) startTime);
            updateSeekBarProgress();
            if (!mediaPlayer.isPlaying()) {
                togglePlayback();
            }
        }
    }
    
    @Override
    public void onPlaybackPositionChanged(long position) {
        // Nothing to do here, adapter already has access to MediaPlayer
    }
}