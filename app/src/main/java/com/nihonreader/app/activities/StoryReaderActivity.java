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

import com.nihonreader.app.R;
import com.nihonreader.app.adapters.TextSegmentAdapter;
import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.Story;
import com.nihonreader.app.models.StoryContent;
import com.nihonreader.app.models.UserProgress;
import com.nihonreader.app.utils.AudioUtils;
import com.nihonreader.app.viewmodels.StoryReaderViewModel;

import java.io.IOException;
import java.util.List;

/**
 * Activity for reading stories with synchronized audio
 */
public class StoryReaderActivity extends AppCompatActivity {
    
    public static final String EXTRA_STORY_ID = "com.nihonreader.app.EXTRA_STORY_ID";
    
    private StoryReaderViewModel viewModel;
    private RecyclerView recyclerView;
    private TextSegmentAdapter adapter;
    private ProgressBar progressBar;
    private SeekBar seekBar;
    private TextView textViewCurrentTime;
    private TextView textViewTotalTime;
    private ImageButton buttonPlayPause;
    private ImageButton buttonSkipBackward;
    private ImageButton buttonSkipForward;
    
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private Runnable updateSeekBarRunnable;
    
    private Story story;
    private StoryContent storyContent;
    private UserProgress userProgress;
    private List<AudioSegment> segments;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_reader);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Setup views
        recyclerView = findViewById(R.id.recycler_view_text);
        progressBar = findViewById(R.id.progress_bar);
        seekBar = findViewById(R.id.seek_bar);
        textViewCurrentTime = findViewById(R.id.text_view_current_time);
        textViewTotalTime = findViewById(R.id.text_view_total_time);
        buttonPlayPause = findViewById(R.id.button_play_pause);
        buttonSkipBackward = findViewById(R.id.button_skip_backward);
        buttonSkipForward = findViewById(R.id.button_skip_forward);
        
        // Setup recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TextSegmentAdapter(this);
        recyclerView.setAdapter(adapter);
        
        // Get story ID from intent
        String storyId = getIntent().getStringExtra(EXTRA_STORY_ID);
        if (storyId == null) {
            Toast.makeText(this, "Error: No story ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Setup view model
        viewModel = new ViewModelProvider(this).get(StoryReaderViewModel.class);
        viewModel.loadStory(storyId);
        
        // Observe story
        viewModel.getStory().observe(this, story -> {
            if (story != null) {
                this.story = story;
                getSupportActionBar().setTitle(story.getTitle());
            }
        });
        
        // Observe story content
        viewModel.getStoryContent().observe(this, content -> {
            if (content != null) {
                storyContent = content;
                segments = content.getSegments();
                adapter.setSegments(segments);
                
                // Initialize media player
                initializeMediaPlayer(content.getAudioUri());
            }
        });
        
        // Observe user progress
        viewModel.getUserProgress().observe(this, progress -> {
            if (progress != null) {
                userProgress = progress;
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    // Restore last position
                    mediaPlayer.seekTo((int) progress.getLastAudioPosition());
                    updateSeekBarProgress();
                }
            }
        });
        
        // Observe current segment index
        viewModel.getCurrentSegmentIndex().observe(this, index -> {
            if (index >= 0 && segments != null && index < segments.size()) {
                adapter.setSelectedPosition(index);
                recyclerView.smoothScrollToPosition(index);
            }
        });
        
        // Observe playback state
        viewModel.getIsPlaying().observe(this, isPlaying -> {
            buttonPlayPause.setImageResource(isPlaying ? 
                    android.R.drawable.ic_media_pause : 
                    android.R.drawable.ic_media_play);
            
            if (isPlaying) {
                startUpdatingSeekBar();
            } else {
                stopUpdatingSeekBar();
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
        
        // Initialize handler for seeking
        handler = new Handler(Looper.getMainLooper());
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                updateSeekBarProgress();
                handler.postDelayed(this, 100);
            }
        };
        
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
                viewModel.setIsPlaying(false);
                mediaPlayer.seekTo(0);
                updateSeekBarProgress();
            });
            
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
            viewModel.setIsPlaying(false);
        } else {
            mediaPlayer.start();
            viewModel.setIsPlaying(true);
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
        
        // Update current segment
        int segmentIndex = viewModel.findCurrentSegment(storyContent, currentPosition);
        if (segmentIndex >= 0) {
            viewModel.setCurrentSegmentIndex(segmentIndex);
        }
        
        // Save progress
        if (userProgress != null) {
            viewModel.updateAudioPosition(currentPosition);
        }
    }
    
    private void startUpdatingSeekBar() {
        handler.post(updateSeekBarRunnable);
    }
    
    private void stopUpdatingSeekBar() {
        handler.removeCallbacks(updateSeekBarRunnable);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
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
            viewModel.setIsPlaying(false);
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
}