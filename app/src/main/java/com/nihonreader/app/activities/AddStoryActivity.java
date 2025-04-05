package com.nihonreader.app.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.nihonreader.app.utils.JapaneseTextUtils;
import com.nihonreader.app.viewmodels.AddStoryViewModel;
import com.nihonreader.app.repository.StoryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Activity for adding new custom stories
 */
public class AddStoryActivity extends AppCompatActivity {
    
    private static final int REQUEST_PICK_TEXT = 1;
    private static final int REQUEST_PICK_AUDIO = 2;
    
    private AddStoryViewModel viewModel;
    
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextAuthor;
    private TextInputEditText editTextDescription;
    
    private Button buttonSelectText;
    private Button buttonSelectAudio;
    private Button buttonChangeText;
    private Button buttonChangeAudio;
    private Button buttonImport;
    
    private LinearLayout textPreviewContainer;
    private LinearLayout audioPreviewContainer;
    
    private TextView textViewTextFileName;
    private TextView textViewAudioFileName;
    
    private ProgressBar progressBar;
    private TextView textViewStatus;
    private CheckBox checkboxUseAi;
    
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
        buttonChangeText = findViewById(R.id.button_change_text);
        buttonChangeAudio = findViewById(R.id.button_change_audio);
        buttonImport = findViewById(R.id.button_import);
        
        textPreviewContainer = findViewById(R.id.text_preview_container);
        audioPreviewContainer = findViewById(R.id.audio_preview_container);
        
        textViewTextFileName = findViewById(R.id.text_view_text_file_name);
        textViewAudioFileName = findViewById(R.id.text_view_audio_file_name);
        
        progressBar = findViewById(R.id.progress_bar);
        textViewStatus = findViewById(R.id.text_view_status);
        checkboxUseAi = findViewById(R.id.checkbox_use_ai);
    }
    
    private void setupObservers() {
        viewModel.getTitle().observe(this, title -> {
            if (title != null) {
                editTextTitle.setText(title);
            }
        });

        viewModel.getAuthor().observe(this, author -> {
            if (author != null) {
                editTextAuthor.setText(author);
            }
        });

        viewModel.getDescription().observe(this, description -> {
            if (description != null) {
                editTextDescription.setText(description);
            }
        });

        viewModel.getTextFileName().observe(this, fileName -> {
            if (fileName != null && !fileName.isEmpty()) {
                textViewTextFileName.setText(fileName);
                buttonSelectText.setVisibility(View.GONE);
                buttonChangeText.setVisibility(View.VISIBLE);
                textPreviewContainer.setVisibility(View.VISIBLE);
            } else {
                textViewTextFileName.setText(R.string.no_file_selected);
                buttonSelectText.setVisibility(View.VISIBLE);
                buttonChangeText.setVisibility(View.GONE);
                textPreviewContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getAudioFileName().observe(this, fileName -> {
            if (fileName != null && !fileName.isEmpty()) {
                textViewAudioFileName.setText(fileName);
                buttonSelectAudio.setVisibility(View.GONE);
                buttonChangeAudio.setVisibility(View.VISIBLE);
                audioPreviewContainer.setVisibility(View.VISIBLE);
            } else {
                textViewAudioFileName.setText(R.string.no_file_selected);
                buttonSelectAudio.setVisibility(View.VISIBLE);
                buttonChangeAudio.setVisibility(View.GONE);
                audioPreviewContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getUseAiAlignment().observe(this, useAi -> {
            if (useAi != null) {
                checkboxUseAi.setChecked(useAi);
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
            intent.setType("text/plain");
            startActivityForResult(intent, REQUEST_PICK_TEXT);
        });
        
        // Change text file button
        buttonChangeText.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
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
        
        // Import button
        buttonImport.setOnClickListener(v -> {
            if (viewModel.validateInputs()) {
                viewModel.importStory(new StoryRepository.ImportStoryCallback() {
                    @Override
                    public void onSuccess(String storyId) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddStoryActivity.this, R.string.import_success, Toast.LENGTH_SHORT).show();
                            // Launch EditTimestampsActivity with the generated story ID
                            Intent intent = new Intent(AddStoryActivity.this, EditTimestampsActivity.class);
                            intent.putExtra(EditTimestampsActivity.EXTRA_STORY_ID, storyId);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddStoryActivity.this, error, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onProgressUpdate(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddStoryActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, R.string.please_fill_required_fields, Toast.LENGTH_SHORT).show();
            }
        });
        
        // AI checkbox
        checkboxUseAi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setUseAiAlignment(isChecked);
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            
            String fileName = getFileNameFromUri(uri);
            
            switch (requestCode) {
                case REQUEST_PICK_TEXT:
                    viewModel.setTextFileUri(uri);
                    viewModel.setTextFileName(fileName);
                    // Read the text file content
                    try {
                        String content = readTextFile(uri);
                        viewModel.setTextContent(content);
                    } catch (IOException e) {
                        Toast.makeText(this, R.string.error_reading_file, Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case REQUEST_PICK_AUDIO:
                    viewModel.setAudioFileUri(uri);
                    viewModel.setAudioFileName(fileName);
                    break;
            }
        }
    }
    
    private String readTextFile(Uri uri) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Fall back to path-based name extraction
            }
        }
        
        if (result == null) {
            result = uri.getLastPathSegment();
            
            if (result == null) {
                // Last resort: use a default name
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null) {
                    if (mimeType.startsWith("text/")) {
                        return "text_file.txt";
                    } else if (mimeType.startsWith("audio/")) {
                        return "audio_file.mp3";
                    }
                }
                return "selected_file";
            }
        }
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}