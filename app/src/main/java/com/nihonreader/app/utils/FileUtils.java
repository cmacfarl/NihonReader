package com.nihonreader.app.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for file operations
 */
public class FileUtils {
    
    private static final String TAG = "FileUtils";
    
    /**
     * Read text from a content URI
     * @param context Application context
     * @param uri URI of the text file
     * @return The content of the text file
     */
    public static String readTextFromUri(Context context, Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        }
        return stringBuilder.toString();
    }
    
    /**
     * Copy a file from a content URI to a local file
     * @param context Application context
     * @param sourceUri Source content URI
     * @param destinationFile Destination file
     * @return True if successful, false otherwise
     */
    public static boolean copyFile(Context context, Uri sourceUri, File destinationFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                return false;
            }
            
            OutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error copying file", e);
            return false;
        }
    }
    
    /**
     * Get the file name from a URI
     * @param context Application context
     * @param uri The URI
     * @return The file name
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
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
    
    /**
     * Generate a unique file name with timestamp
     * @param prefix File name prefix
     * @param extension File extension
     * @return A unique file name
     */
    public static String generateUniqueFileName(String prefix, String extension) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return prefix + "_" + timestamp + "." + extension;
    }
    
    /**
     * Create necessary directories for the app
     * @param context Application context
     */
    public static void createAppDirectories(Context context) {
        File storiesDir = new File(context.getFilesDir(), "stories");
        File audioDir = new File(context.getFilesDir(), "audio");
        
        if (!storiesDir.exists() && !storiesDir.mkdirs()) {
            Log.e(TAG, "Failed to create stories directory");
        }
        
        if (!audioDir.exists() && !audioDir.mkdirs()) {
            Log.e(TAG, "Failed to create audio directory");
        }
    }
}