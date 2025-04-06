package com.nihonreader.app.adapters;

import android.media.MediaPlayer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nihonreader.app.R;
import com.nihonreader.app.utils.AudioUtils;
import com.nihonreader.app.models.AudioSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying and editing timestamps
 */
public class TimestampAdapter extends RecyclerView.Adapter<TimestampAdapter.TimestampViewHolder> {
    
    private List<AudioSegment> segments = new ArrayList<>();
    private OnTimestampEditListener listener;
    private MediaPlayer mediaPlayer;
    private int currentPosition = -1;
    
    public interface OnTimestampEditListener {
        void onStartTimeSet(int position, long timestamp);
        void onEndTimeSet(int position, long timestamp);
        void onPlaySegment(int position, long startTime, long endTime);
        void onPlaybackPositionChanged(long position);
        void onBeginTimeCaptureForSegment(int position);
        void onMergeSegments(int position);
    }
    
    public TimestampAdapter(MediaPlayer mediaPlayer, OnTimestampEditListener listener) {
        this.mediaPlayer = mediaPlayer;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TimestampViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timestamp, parent, false);
        return new TimestampViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TimestampViewHolder holder, int position) {
        AudioSegment segment = segments.get(position);
        
        // Store segment and position for use in listeners
        holder.segment = segment;
        holder.position = position;
        
        // Set segment text
        holder.textSegment.setText(segment.getText());
        
        // Set start time in mm:ss format
        holder.editStartTime.setText(AudioUtils.formatTime(segment.getStart()));
        
        // Set end time in mm:ss format
        holder.editEndTime.setText(AudioUtils.formatTime(segment.getEnd()));
        
        // Set up text watchers to update values when edited
        setupStartTimeWatcher(holder);
        setupEndTimeWatcher(holder);
        
        // Play segment button - now used to start time capture
        holder.buttonPlaySegment.setOnClickListener(v -> {
            if (listener != null) {
                // This now signals the start of time capture for this segment
                listener.onBeginTimeCaptureForSegment(position);
                
                // Change button text to indicate it's in capture mode
                holder.buttonPlaySegment.setText(R.string.capturing);
                
                // Start playback from the segment's current start time
                if (holder.segment != null) {
                    listener.onPlaySegment(position, holder.segment.getStart(), holder.segment.getEnd());
                }
            }
        });
        
        // Set start time button
        holder.buttonSetStart.setOnClickListener(v -> {
            if (mediaPlayer != null && listener != null) {
                long currentPosition = mediaPlayer.getCurrentPosition();
                holder.editStartTime.setText(AudioUtils.formatTime(currentPosition));
                // Update segment directly
                holder.segment.setStart(currentPosition);
                listener.onStartTimeSet(position, currentPosition);
            }
        });
        
        // Set end time button
        holder.buttonSetEnd.setOnClickListener(v -> {
            if (mediaPlayer != null && listener != null) {
                long currentPosition = mediaPlayer.getCurrentPosition();
                holder.editEndTime.setText(AudioUtils.formatTime(currentPosition));
                // Update segment directly
                holder.segment.setEnd(currentPosition);
                listener.onEndTimeSet(position, currentPosition);
                
                // Auto-update the start time of the next segment if available
                int nextPosition = position + 1;
                if (nextPosition < segments.size()) {
                    AudioSegment nextSegment = segments.get(nextPosition);
                    nextSegment.setStart(currentPosition);
                    // Post the notifyItemChanged call to the main thread to avoid RecyclerView exception
                    v.post(() -> notifyItemChanged(nextPosition));
                }
            }
        });
        
        // Merge with previous segment button
        if (position == 0) {
            // Hide merge button for the first segment
            holder.buttonMerge.setVisibility(View.INVISIBLE);
        } else {
            holder.buttonMerge.setVisibility(View.VISIBLE);
            holder.buttonMerge.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMergeSegments(position);
                }
            });
        }
    }
    
    private void setupStartTimeWatcher(TimestampViewHolder holder) {
        // Remove existing TextWatcher if any to avoid duplicate listeners
        if (holder.startTimeWatcher != null) {
            holder.editStartTime.removeTextChangedListener(holder.startTimeWatcher);
        }
        
        // Create new TextWatcher
        holder.startTimeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && listener != null) {
                    try {
                        long time = AudioUtils.parseTimeString(s.toString());
                        // Update the segment directly
                        holder.segment.setStart(time);
                        listener.onStartTimeSet(holder.position, time);
                    } catch (NumberFormatException e) {
                        // Ignore invalid number format
                    }
                }
            }
        };
        
        // Add the TextWatcher
        holder.editStartTime.addTextChangedListener(holder.startTimeWatcher);
    }
    
    private void setupEndTimeWatcher(TimestampViewHolder holder) {
        // Remove existing TextWatcher if any to avoid duplicate listeners
        if (holder.endTimeWatcher != null) {
            holder.editEndTime.removeTextChangedListener(holder.endTimeWatcher);
        }
        
        // Create new TextWatcher
        holder.endTimeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 && listener != null) {
                    try {
                        long time = AudioUtils.parseTimeString(s.toString());
                        // Update current segment's end time directly
                        holder.segment.setEnd(time);
                        listener.onEndTimeSet(holder.position, time);
                        
                        // Auto-update the start time of the next segment if available
                        int nextPosition = holder.position + 1;
                        if (nextPosition < segments.size()) {
                            AudioSegment nextSegment = segments.get(nextPosition);
                            nextSegment.setStart(time);
                            // Post the notifyItemChanged call to the main thread to avoid RecyclerView exception
                            holder.itemView.post(() -> notifyItemChanged(nextPosition));
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid number format
                    }
                }
            }
        };
        
        // Add the TextWatcher
        holder.editEndTime.addTextChangedListener(holder.endTimeWatcher);
    }
    
    @Override
    public int getItemCount() {
        return segments.size();
    }
    
    public void setSegments(List<AudioSegment> segments) {
        this.segments = segments;
        notifyDataSetChanged();
    }
    
    public List<AudioSegment> getSegments() {
        return segments;
    }
    
    public void updateSegmentStartTime(int position, long timestamp) {
        if (position >= 0 && position < segments.size()) {
            AudioSegment segment = segments.get(position);
            segment.setStart(timestamp);
            notifyItemChanged(position);
        }
    }
    
    public void updateSegmentEndTime(int position, long timestamp) {
        if (position >= 0 && position < segments.size()) {
            AudioSegment segment = segments.get(position);
            segment.setEnd(timestamp);
            notifyItemChanged(position);
        }
    }
    
    /**
     * Merge a segment with the one above it (merges position with position-1)
     * @param position The position of the segment to merge with the previous one
     */
    public void mergeWithPreviousSegment(int position) {
        if (position > 0 && position < segments.size()) {
            AudioSegment currentSegment = segments.get(position);
            AudioSegment previousSegment = segments.get(position - 1);
            
            // Merge text: previousSegment text + currentSegment text
            previousSegment.setText(previousSegment.getText() + " " + currentSegment.getText());
            
            // Keep previousSegment's start time, use currentSegment's end time
            previousSegment.setEnd(currentSegment.getEnd());
            
            // Remove the current segment
            segments.remove(position);
            
            // Notify adapter about the changes
            notifyItemChanged(position - 1);
            notifyItemRemoved(position);
        }
    }
    
    /**
     * ViewHolder for timestamp items
     */
    public class TimestampViewHolder extends RecyclerView.ViewHolder {
        public TextView textSegment;
        public EditText editStartTime;
        public EditText editEndTime;
        ImageButton buttonSetStart;
        ImageButton buttonSetEnd;
        ImageButton buttonMerge;
        public Button buttonPlaySegment;
        int position;
        TextWatcher startTimeWatcher;
        TextWatcher endTimeWatcher;
        public AudioSegment segment; // Store reference to the segment this ViewHolder represents
        
        TimestampViewHolder(@NonNull View itemView) {
            super(itemView);
            textSegment = itemView.findViewById(R.id.text_segment);
            editStartTime = itemView.findViewById(R.id.edit_start_time);
            editEndTime = itemView.findViewById(R.id.edit_end_time);
            buttonSetStart = itemView.findViewById(R.id.button_set_start);
            buttonSetEnd = itemView.findViewById(R.id.button_set_end);
            buttonMerge = itemView.findViewById(R.id.button_merge);
            buttonPlaySegment = itemView.findViewById(R.id.button_play_segment);
        }
    }
}