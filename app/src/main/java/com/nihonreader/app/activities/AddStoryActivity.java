package com.nihonreader.app.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.nihonreader.app.R;
import com.nihonreader.app.utils.FileUtils;
import com.nihonreader.app.viewmodels.AddStoryViewModel;

import java.io.IOException;

/**
 * Activity for adding new custom stories
 */
public class AddStoryActivity extends AppCompatActivity {
    
    private static final int REQUEST_PICK_TEXT = 1;
    private static final int REQUEST_PICK_AUDIO = 2;
    private static final int REQUEST_PICK_TIMING = 3;
    
    private AddStoryViewModel viewModel;
    
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextAuthor;
    private TextInputEditText editTextDescription;
    
    private Button buttonSelectText;
    private Button buttonSelectAudio;
    private Button buttonSelectTiming;
    private Button buttonChangeText;
    private Button buttonChangeAudio;
    private Button buttonChangeTiming;
    private Button buttonImport;
    
    private LinearLayout textPreviewContainer;
    private LinearLayout audioPreviewContainer;
    private LinearLayout timingPreviewContainer;
    
    private TextView textViewTextFileName;
    private TextView textViewAudioFileName;
    private TextView textViewTimingFileName;
    
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_story);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Setup view model
        viewModel = new ViewModelProvider(this).get(AddStoryViewModel.class);
        
        // Initialize views
        initializeViews();
        
        // Setup observers
        setupObservers();
        
        // Setup click listeners
        setupClickListeners();
    }
    
    private void initializeViews() {
        editTextTitle = findViewById(R.id.edit_text_title);
        editTextAuthor = findViewById(R.id.edit_text_author);
        editTextDescription = findViewById(R.id.edit_text_description);
        
        buttonSelectText = findViewById(R.id.button_select_text);
        buttonSelectAudio = findViewById(R.id.button_select_audio);
        buttonSelectTiming = findViewById(R.id.button_select_timing);
        buttonChangeText = findViewById(R.id.button_change_text);
        buttonChangeAudio = findViewById(R.id.button_change_audio);
        buttonChangeTiming = findViewById(R.id.button_change_timing);
        buttonImport = findViewById(R.id.button_import);
        
        textPreviewContainer = findViewById(R.id.text_preview_container);
        audioPreviewContainer = findViewById(R.id.audio_preview_container);
        timingPreviewContainer = findViewById(R.id.timing_preview_container);
        
        textViewTextFileName = findViewById(R.id.text_view_text_file_name);
        textViewAudioFileName = findViewById(R.id.text_view_audio_file_name);
        textViewTimingFileName = findViewById(R.id.text_view_timing_file_name);
        
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void setupObservers() {
        // Observe title
        viewModel.getTitle().observe(this, title -> {
            if (!title.equals(editTextTitle.getText().toString())) {
                editTextTitle.setText(title);
            }
        });
        
        // Observe author
        viewModel.getAuthor().observe(this, author -> {
            if (!author.equals(editTextAuthor.getText().toString())) {
                editTextAuthor.setText(author);
            }
        });
        
        // Observe description
        viewModel.getDescription().observe(this, description -> {
            if (!description.equals(editTextDescription.getText().toString())) {
                editTextDescription.setText(description);
            }
        });
        
        // Observe text file name
        viewModel.getTextFileName().observe(this, fileName -> {
            if (fileName != null && !fileName.isEmpty()) {
                textViewTextFileName.setText(fileName);
                buttonSelectText.setVisibility(View.GONE);
                textPreviewContainer.setVisibility(View.VISIBLE);
            } else {
                buttonSelectText.setVisibility(View.VISIBLE);
                textPreviewContainer.setVisibility(View.GONE);
            }
        });
        
        // Observe audio file name
        viewModel.getAudioFileName().observe(this, fileName -> {
            if (fileName != null && !fileName.isEmpty()) {
                textViewAudioFileName.setText(fileName);
                buttonSelectAudio.setVisibility(View.GONE);
                audioPreviewContainer.setVisibility(View.VISIBLE);
            } else {
                buttonSelectAudio.setVisibility(View.VISIBLE);
                audioPreviewContainer.setVisibility(View.GONE);
            }
        });
        
        // Observe timing file name
        viewModel.getTimingFileName().observe(this, fileName -> {
            if (fileName != null && !fileName.isEmpty()) {
                textViewTimingFileName.setText(fileName);
                buttonSelectTiming.setVisibility(View.GONE);
                timingPreviewContainer.setVisibility(View.VISIBLE);
            } else {
                buttonSelectTiming.setVisibility(View.VISIBLE);
                timingPreviewContainer.setVisibility(View.GONE);
            }
        });
        
        // Observe importing state
        viewModel.getIsImporting().observe(this, isImporting -> {
            progressBar.setVisibility(isImporting ? View.VISIBLE : View.GONE);
            buttonImport.setEnabled(!isImporting);
        });
        
        // Observe import result
        viewModel.getImportResult().observe(this, result -> {
            if (result != null) {
                if (result.startsWith("success")) {
                    Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else if (result.startsWith("error")) {
                    Toast.makeText(this, R.string.error_import, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void setupClickListeners() {
        // Text title field
        editTextTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                viewModel.setTitle(editTextTitle.getText().toString());
            }
        });
        
        // Author field
        editTextAuthor.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                viewModel.setAuthor(editTextAuthor.getText().toString());
            }
        });
        
        // Description field
        editTextDescription.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                viewModel.setDescription(editTextDescription.getText().toString());
            }
        });
        
        // Select text file button
        buttonSelectText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, REQUEST_PICK_TEXT);
        });
        
        // Change text file button
        buttonChangeText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, REQUEST_PICK_TEXT);
        });
        
        // Select audio file button
        buttonSelectAudio.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            startActivityForResult(intent, REQUEST_PICK_AUDIO);
        });
        
        // Change audio file button
        buttonChangeAudio.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            startActivityForResult(intent, REQUEST_PICK_AUDIO);
        });
        
        // Select timing file button
        buttonSelectTiming.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, REQUEST_PICK_TIMING);
        });
        
        // Change timing file button
        buttonChangeTiming.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/*");
            startActivityForResult(intent, REQUEST_PICK_TIMING);
        });
        
        // Import button
        buttonImport.setOnClickListener(v -> {
            if (validateInputs()) {
                viewModel.importStory();
            }
        });
    }
    
    private boolean validateInputs() {
        // Validate title
        if (editTextTitle.getText().toString().trim().isEmpty()) {
            editTextTitle.setError("Title is required");
            editTextTitle.requestFocus();
            return false;
        }
        
        // Validate author
        if (editTextAuthor.getText().toString().trim().isEmpty()) {
            editTextAuthor.setError("Author is required");
            editTextAuthor.requestFocus();
            return false;
        }
        
        // Validate text file
        if (viewModel.getTextFileUri().getValue() == null) {
            Toast.makeText(this, "Please select a text file", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Validate audio file
        if (viewModel.getAudioFileUri().getValue() == null) {
            Toast.makeText(this, "Please select an audio file", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Get file name from uri
                String fileName = getFileNameFromUri(uri);
                
                // Handle different request types
                switch (requestCode) {
                    case REQUEST_PICK_TEXT:
                        try {
                            String textContent = FileUtils.readTextFromUri(this, uri);
                            viewModel.setTextContent(textContent);
                            viewModel.setTextFileUri(uri);
                            viewModel.setTextFileName(fileName);
                            
                            // Auto-set title from file name if not set
                            if (editTextTitle.getText().toString().trim().isEmpty()) {
                                String titleFromFile = fileName;
                                if (titleFromFile.lastIndexOf('.') > 0) {
                                    titleFromFile = titleFromFile.substring(0, titleFromFile.lastIndexOf('.'));
                                }
                                viewModel.setTitle(titleFromFile);
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Error reading text file", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    
                    case REQUEST_PICK_AUDIO:
                        viewModel.setAudioFileUri(uri);
                        viewModel.setAudioFileName(fileName);
                        break;
                    
                    case REQUEST_PICK_TIMING:
                        viewModel.setTimingFileUri(uri);
                        viewModel.setTimingFileName(fileName);
                        break;
                }
            }
        }
    }
    
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}