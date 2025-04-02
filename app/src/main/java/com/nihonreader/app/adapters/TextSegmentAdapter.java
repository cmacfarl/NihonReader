package com.nihonreader.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nihonreader.app.R;
import com.nihonreader.app.models.AudioSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying text segments in a RecyclerView
 */
public class TextSegmentAdapter extends RecyclerView.Adapter<TextSegmentAdapter.TextSegmentViewHolder> {
    
    private List<AudioSegment> segments = new ArrayList<>();
    private int selectedPosition = -1;
    
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
        holder.textViewSegment.setText(segment.getText());
        
        // Set selection state
        holder.itemView.setSelected(position == selectedPosition);
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
        private TextView textViewSegment;
        
        TextSegmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSegment = itemView.findViewById(R.id.text_view_segment);
        }
    }
}