package com.nihonreader.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story currentStory = stories.get(position);
        holder.textViewTitle.setText(currentStory.getTitle());
        holder.textViewAuthor.setText(holder.itemView.getContext().getString(R.string.by, currentStory.getAuthor()));
        holder.textViewDescription.setText(currentStory.getDescription());
        
        if (currentStory.isCustom()) {
            holder.chipCustom.setVisibility(View.VISIBLE);
        } else {
            holder.chipCustom.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return stories.size();
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
                
                return oldStory.getTitle().equals(newStory.getTitle()) &&
                       oldStory.getAuthor().equals(newStory.getAuthor()) &&
                       oldStory.getDescription().equals(newStory.getDescription()) &&
                       oldStory.isCustom() == newStory.isCustom();
            }
        });
        
        this.stories = stories;
        result.dispatchUpdatesTo(this);
    }
    
    class StoryViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewTitle;
        private TextView textViewAuthor;
        private TextView textViewDescription;
        private Chip chipCustom;
        
        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.text_view_title);
            textViewAuthor = itemView.findViewById(R.id.text_view_author);
            textViewDescription = itemView.findViewById(R.id.text_view_description);
            chipCustom = itemView.findViewById(R.id.chip_custom);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onStoryClick(stories.get(position));
                }
            });
        }
    }
    
    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }
}