package com.nihonreader.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.nihonreader.app.R;
import com.nihonreader.app.fragments.WordPopupFragment;
import com.nihonreader.app.models.AudioSegment;
import com.nihonreader.app.models.JapaneseWord;
import com.nihonreader.app.repository.StoryRepository;
import com.nihonreader.app.utils.DictionaryLookupService;
import com.nihonreader.app.views.JapaneseTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying text segments in a RecyclerView with Japanese word parsing
 */
public class TextSegmentAdapter extends RecyclerView.Adapter<TextSegmentAdapter.TextSegmentViewHolder> {
    
    private List<AudioSegment> segments = new ArrayList<>();
    private int selectedPosition = -1;
    private DictionaryLookupService dictionaryLookupService;
    
    public TextSegmentAdapter(Context context) {
        StoryRepository repository = new StoryRepository(context);
        dictionaryLookupService = new DictionaryLookupService(context, repository);
    }
    
    @NonNull
    @Override
    public TextSegmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_text_segment, parent, false);
        return new TextSegmentViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TextSegmentViewHolder holder, int position) {
        AudioSegment segment = segments.get(position);
        
        // Use the JapaneseTextView to display parsed text
        holder.japaneseTextView.setJapaneseText(segment.getText());
        
        // Set selection state
        holder.itemView.setSelected(position == selectedPosition);
        
        // Set word click listener to show the dialog with dictionary lookup
        holder.japaneseTextView.setOnWordClickListener(word -> {
            // Only respond to clickable words (not particles, etc.)
            if (word.isClickable()) {
                // Look up the word in the dictionary
                dictionaryLookupService.lookupWord(word, new DictionaryLookupService.OnWordDefinitionFoundListener() {
                    @Override
                    public void onDefinitionFound(com.nihonreader.app.models.VocabularyItem vocabularyItem) {
                        // Show popup fragment with the word details below the clicked word
                        holder.itemView.post(() -> {
                            Context context = holder.itemView.getContext();
                            if (context instanceof FragmentActivity) {
                                FragmentActivity activity = (FragmentActivity) context;
                                
                                // Calculate click position (if available from event)
                                int x = 0;
                                int y = 0;
                                
                                // Create and show the fragment
                                WordPopupFragment fragment = WordPopupFragment.newInstance(
                                        word, vocabularyItem, holder.japaneseTextView, x, y);
                                fragment.show(activity.getSupportFragmentManager(), "word_popup");
                            }
                        });
                    }
                    
                    @Override
                    public void onDefinitionNotFound() {
                        // Show popup fragment with just the word details from Kuromoji
                        holder.itemView.post(() -> {
                            Context context = holder.itemView.getContext();
                            if (context instanceof FragmentActivity) {
                                FragmentActivity activity = (FragmentActivity) context;
                                
                                // Calculate click position (if available from event)
                                int x = 0;
                                int y = 0;
                                
                                // Create and show the fragment
                                WordPopupFragment fragment = WordPopupFragment.newInstance(
                                        word, null, holder.japaneseTextView, x, y);
                                fragment.show(activity.getSupportFragmentManager(), "word_popup");
                            }
                        });
                    }
                });
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return segments.size();
    }
    
    public void setSegments(List<AudioSegment> segments) {
        this.segments = segments;
        notifyDataSetChanged();
    }
    
    public void setSelectedPosition(int position) {
        if (position != selectedPosition) {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            
            if (previousSelected >= 0) {
                notifyItemChanged(previousSelected);
            }
            if (selectedPosition >= 0) {
                notifyItemChanged(selectedPosition);
            }
        }
    }
    
    class TextSegmentViewHolder extends RecyclerView.ViewHolder {
        private JapaneseTextView japaneseTextView;
        
        TextSegmentViewHolder(@NonNull View itemView) {
            super(itemView);
            japaneseTextView = itemView.findViewById(R.id.text_view_segment);
        }
    }
}