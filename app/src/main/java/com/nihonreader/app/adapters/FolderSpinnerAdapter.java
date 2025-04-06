package com.nihonreader.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nihonreader.app.R;
import com.nihonreader.app.models.Folder;

import java.util.List;

public class FolderSpinnerAdapter extends ArrayAdapter<Folder> {
    private final LayoutInflater inflater;
    private final int selectedItemLayout;
    private final int dropdownItemLayout;

    public FolderSpinnerAdapter(@NonNull Context context, List<Folder> folders) {
        super(context, 0, folders);
        this.inflater = LayoutInflater.from(context);
        this.selectedItemLayout = R.layout.item_spinner_folder;
        this.dropdownItemLayout = R.layout.item_spinner_folder_dropdown;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent, selectedItemLayout);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent, dropdownItemLayout);
    }

    private View createItemView(int position, View convertView, ViewGroup parent, int layout) {
        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
        }

        Folder folder = getItem(position);
        if (folder != null) {
            TextView folderName = convertView.findViewById(R.id.text_folder_name);
            TextView storyCount = convertView.findViewById(R.id.text_story_count);

            folderName.setText(folder.getName());
            storyCount.setText(String.valueOf(folder.getStoryCount()));
        }

        return convertView;
    }
} 