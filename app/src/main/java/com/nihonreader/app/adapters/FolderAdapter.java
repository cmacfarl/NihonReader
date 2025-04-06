package com.nihonreader.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.nihonreader.app.R;
import com.nihonreader.app.models.Folder;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying folders in a RecyclerView
 */
public class FolderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_ALL_STORIES = 0;
    private static final int VIEW_TYPE_FOLDER = 1;
    
    private List<Folder> folders = new ArrayList<>();
    private OnFolderClickListener listener;
    private Context context;
    
    public FolderAdapter(Context context, OnFolderClickListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ALL_STORIES) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder, parent, false);
            return new AllStoriesViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder, parent, false);
            return new FolderViewHolder(itemView);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ALL_STORIES) {
            AllStoriesViewHolder allStoriesHolder = (AllStoriesViewHolder) holder;
            allStoriesHolder.folderName.setText(context.getString(R.string.all_stories));
            allStoriesHolder.folderStoryCount.setText("");
            allStoriesHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAllStoriesClick();
                }
            });
        } else {
            FolderViewHolder folderHolder = (FolderViewHolder) holder;
            Folder folder = folders.get(position - 1); // Adjust for "All Stories" item
            
            folderHolder.folderName.setText(folder.getName());
            folderHolder.folderStoryCount.setText("");
            
            // Set up menu button
            folderHolder.buttonFolderMenu.setOnClickListener(v -> {
                showFolderMenu(v, folder);
            });
            
            // Set click listener for the entire item
            folderHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFolderClick(folder);
                }
            });
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_ALL_STORIES : VIEW_TYPE_FOLDER;
    }
    
    private void showFolderMenu(View view, Folder folder) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.menu_folder);
        
        // Don't show delete option for default folder
        if (folder.isDefaultFolder()) {
            popup.getMenu().findItem(R.id.action_delete_folder).setVisible(false);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            if (listener != null) {
                int id = item.getItemId();
                if (id == R.id.action_edit_folder) {
                    listener.onEditFolderClick(folder);
                    return true;
                } else if (id == R.id.action_delete_folder) {
                    listener.onDeleteFolderClick(folder);
                    return true;
                }
            }
            return false;
        });
        
        popup.show();
    }
    
    @Override
    public int getItemCount() {
        return folders.size() + 1; // +1 for "All Stories"
    }
    
    public void setFolders(List<Folder> folders) {
        final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return FolderAdapter.this.folders.size() + 1; // +1 for "All Stories"
            }
            
            @Override
            public int getNewListSize() {
                return folders.size() + 1; // +1 for "All Stories"
            }
            
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // "All Stories" item
                if (oldItemPosition == 0 && newItemPosition == 0) {
                    return true;
                }
                
                // Regular folders
                if (oldItemPosition > 0 && newItemPosition > 0) {
                    return FolderAdapter.this.folders.get(oldItemPosition - 1).getId().equals(
                            folders.get(newItemPosition - 1).getId());
                }
                
                return false;
            }
            
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                // "All Stories" item
                if (oldItemPosition == 0 && newItemPosition == 0) {
                    return true;
                }
                
                // Regular folders
                if (oldItemPosition > 0 && newItemPosition > 0) {
                    Folder oldFolder = FolderAdapter.this.folders.get(oldItemPosition - 1);
                    Folder newFolder = folders.get(newItemPosition - 1);
                    
                    return oldFolder.getName().equals(newFolder.getName()) &&
                            oldFolder.getPosition() == newFolder.getPosition() &&
                            oldFolder.isDefaultFolder() == newFolder.isDefaultFolder();
                }
                
                return false;
            }
        });
        
        this.folders = folders;
        result.dispatchUpdatesTo(this);
    }
    
    public void updateStoryCounts() {
        // Update story counts and refresh the UI
        notifyDataSetChanged();
    }
    
    static class FolderViewHolder extends RecyclerView.ViewHolder {
        private ImageView folderIcon;
        private TextView folderName;
        private TextView folderStoryCount;
        private ImageButton buttonFolderMenu;
        
        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.folder_icon);
            folderName = itemView.findViewById(R.id.folder_name);
            folderStoryCount = itemView.findViewById(R.id.folder_story_count);
            buttonFolderMenu = itemView.findViewById(R.id.button_folder_menu);
        }
    }
    
    static class AllStoriesViewHolder extends RecyclerView.ViewHolder {
        private ImageView folderIcon;
        private TextView folderName;
        private TextView folderStoryCount;
        
        AllStoriesViewHolder(@NonNull View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.folder_icon);
            folderName = itemView.findViewById(R.id.folder_name);
            folderStoryCount = itemView.findViewById(R.id.folder_story_count);
            
            // Hide menu button for "All Stories"
            ImageButton menuButton = itemView.findViewById(R.id.button_folder_menu);
            if (menuButton != null) {
                menuButton.setVisibility(View.GONE);
            }
        }
    }
    
    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
        void onAllStoriesClick();
        void onEditFolderClick(Folder folder);
        void onDeleteFolderClick(Folder folder);
    }
} 