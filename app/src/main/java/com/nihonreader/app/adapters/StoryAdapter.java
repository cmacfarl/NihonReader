package com.nihonreader.app.adapters;

import android.app.Dialog;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.nihonreader.app.R;
import com.nihonreader.app.models.Story;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying stories in a RecyclerView
 */
public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {
    
    private List<Story> stories = new ArrayList<>();
    private OnStoryClickListener listener;
    
    public StoryAdapter(OnStoryClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the compact layout for improved list density
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story_compact, parent, false);
        return new StoryViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story currentStory = stories.get(position);
        holder.textViewTitle.setText(currentStory.getTitle());
        
        // Store the story in the holder for use in the long click handler
        holder.story = currentStory;
        
        // Set up the menu button click listener
        holder.buttonMenu.setOnClickListener(v -> {
            showPopupMenu(v, position);
        });
    }
    
    /**
     * Show a popup menu for the story options
     * @param view The button that was clicked
     * @param position The position of the story in the list
     */
    private void showPopupMenu(View view, int position) {
        Story story = stories.get(position);
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.inflate(R.menu.menu_story_options);
        
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_edit_timestamps:
                    if (listener != null) {
                        listener.onEditTimestampsClick(story);
                    }
                    return true;
                case R.id.action_move_to_folder:
                    if (listener != null) {
                        listener.onMoveStoryClick(story);
                    }
                    return true;
                case R.id.action_delete:
                    if (listener != null) {
                        listener.onDeleteStoryClick(story);
                    }
                    return true;
                default:
                    return false;
            }
        });
        
        popupMenu.show();
    }
    
    @Override
    public int getItemCount() {
        return stories.size();
    }
    
    public Story getStoryAt(int position) {
        if (position >= 0 && position < stories.size()) {
            return stories.get(position);
        }
        return null;
    }
    
    public void setStories(List<Story> stories) {
        final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return StoryAdapter.this.stories.size();
            }
            
            @Override
            public int getNewListSize() {
                return stories.size();
            }
            
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return StoryAdapter.this.stories.get(oldItemPosition).getId().equals(
                        stories.get(newItemPosition).getId());
            }
            
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Story oldStory = StoryAdapter.this.stories.get(oldItemPosition);
                Story newStory = stories.get(newItemPosition);
                
                // Safely compare folder IDs (either both null or equal)
                boolean folderIdsEqual = (oldStory.getFolderId() == null && newStory.getFolderId() == null) ||
                                        (oldStory.getFolderId() != null && 
                                         newStory.getFolderId() != null && 
                                         oldStory.getFolderId().equals(newStory.getFolderId()));
                
                return oldStory.getTitle().equals(newStory.getTitle()) &&
                       oldStory.getAuthor().equals(newStory.getAuthor()) &&
                       oldStory.getDescription().equals(newStory.getDescription()) &&
                       oldStory.isCustom() == newStory.isCustom() &&
                       folderIdsEqual &&
                       oldStory.getPosition() == newStory.getPosition();
            }
        });
        
        this.stories = stories;
        result.dispatchUpdatesTo(this);
    }
    
    /**
     * Shows a dialog with detailed story information
     * @param story The story to show details for
     * @param view The view that triggered the dialog (for context)
     */
    private void showStoryDetailsDialog(Story story, View view) {
        Dialog dialog = new Dialog(view.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_story_details);
        
        // Get views from dialog
        TextView titleTextView = dialog.findViewById(R.id.text_view_title);
        TextView authorTextView = dialog.findViewById(R.id.text_view_author);
        TextView descriptionTextView = dialog.findViewById(R.id.text_view_description);
        Chip customChip = dialog.findViewById(R.id.chip_custom);
        TextView lastReadTextView = dialog.findViewById(R.id.text_view_last_read);
        
        // Set data to views
        titleTextView.setText(story.getTitle());
        authorTextView.setText(view.getContext().getString(R.string.by, story.getAuthor()));
        descriptionTextView.setText(story.getDescription());
        
        // Show/hide custom chip
        if (story.isCustom()) {
            customChip.setVisibility(View.VISIBLE);
        } else {
            customChip.setVisibility(View.GONE);
        }
        
        // Format and set last read time
        if (story.getLastOpened() != null && !story.getLastOpened().isEmpty()) {
            try {
                long lastOpenedTime = Long.parseLong(story.getLastOpened());
                String formattedTime = DateUtils.getRelativeTimeSpanString(
                        lastOpenedTime, 
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                ).toString();
                lastReadTextView.setText(view.getContext().getString(R.string.last_read_format, formattedTime));
                lastReadTextView.setVisibility(View.VISIBLE);
            } catch (NumberFormatException e) {
                // Hide the view if we can't parse the time
                lastReadTextView.setVisibility(View.GONE);
            }
        } else {
            // Hide the view if there's no last opened time
            lastReadTextView.setVisibility(View.GONE);
        }
        
        // Size the dialog appropriately
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        dialog.show();
    }
    
    class StoryViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private ImageButton buttonMenu;
        private Story story; // Store the story for use in long click
        
        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            buttonMenu = itemView.findViewById(R.id.button_menu);
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onStoryClick(stories.get(position));
                }
            });
            
            // Set long click listener to show story details
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Story currentStory = stories.get(position);
                    showStoryDetailsDialog(currentStory, v);
                    return true; // Consume the long click
                }
                return false;
            });
        }
    }
    
    public interface OnStoryClickListener {
        void onStoryClick(Story story);
        void onEditTimestampsClick(Story story);
        void onDeleteStoryClick(Story story);
        void onMoveStoryClick(Story story);
    }
}