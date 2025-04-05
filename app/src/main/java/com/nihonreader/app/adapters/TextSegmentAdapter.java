package com.nihonreader.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nihonreader.app.R;
import com.nihonreader.app.dialogs.WordDetailsDialog;
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
                        // Show dialog with the word details including vocabulary item
                        holder.itemView.post(() -> {
                            WordDetailsDialog dialog = new WordDetailsDialog(
                                    holder.itemView.getContext(), 
                                    word, 
                                    vocabularyItem);
                            dialog.show();
                        });
                    }
                    
                    @Override
                    public void onDefinitionNotFound() {
                        // Show dialog with just the word details from Kuromoji
                        holder.itemView.post(() -> {
                            WordDetailsDialog dialog = new WordDetailsDialog(
                                    holder.itemView.getContext(), 
                                    word);
                            dialog.show();
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