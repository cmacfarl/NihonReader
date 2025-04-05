package com.nihonreader.app.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nihonreader.app.models.AudioSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for audio-related operations
 */
public class AudioUtils {
    
    private static final String TAG = "AudioUtils";
    // Updated pattern to handle flexible digit counts and any text format (including hash symbols)
    private static final Pattern TIMING_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})\\.(\\d{1,2})\\]\\s*(.*)");
    
    /**
     * Parse a file containing timestamps and corresponding text segments
     * Supports both text format: [00:00.00] Text segment one
     * and JSON format with timestamps and Japanese text segments
     * @param content The content of the timing file
     * @return List of AudioSegment objects
     */
    public static List<AudioSegment> parseTimingFile(String content) {
        List<AudioSegment> segments = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return segments;
        }
        
        // First attempt to determine if content is JSON format
        boolean isLikelyJson = isLikelyJsonFormat(content);
        
        if (isLikelyJson) {
            try {
                List<AudioSegment> jsonSegments = parseJsonTimingFile(content);
                // If we successfully parsed JSON segments, return them
                if (!jsonSegments.isEmpty()) {
                    return jsonSegments;
                }
                // Otherwise fall through to text format parsing
            } catch (JsonSyntaxException e) {
                Log.e(TAG, "Failed to parse JSON timing file", e);
                // Fall back to text format parsing if JSON parsing fails
            }
        }
        
        // Parse as text format
        String[] lines = content.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            
            Matcher matcher = TIMING_PATTERN.matcher(line);
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int seconds = Integer.parseInt(matcher.group(2));
                int centiseconds = Integer.parseInt(matcher.group(3));
                // For single-digit centiseconds, multiply by 100 instead of 10
                int multiplier = matcher.group(3).length() == 1 ? 100 : 10;
                long startTime = (minutes * 60 + seconds) * 1000 + centiseconds * multiplier;
                String text = matcher.group(4);
                
                // Calculate end time (start time of next segment or end of audio)
                long endTime;
                if (i < lines.length - 1) {
                    Matcher nextMatcher = TIMING_PATTERN.matcher(lines[i + 1].trim());
                    if (nextMatcher.matches()) {
                        int nextMinutes = Integer.parseInt(nextMatcher.group(1));
                        int nextSeconds = Integer.parseInt(nextMatcher.group(2));
                        int nextCentiseconds = Integer.parseInt(nextMatcher.group(3));
                        // For single-digit centiseconds, multiply by 100 instead of 10
                        int nextMultiplier = nextMatcher.group(3).length() == 1 ? 100 : 10;
                        endTime = (nextMinutes * 60 + nextSeconds) * 1000 + nextCentiseconds * nextMultiplier;
                    } else {
                        endTime = startTime + 5000; // Default 5 seconds if can't determine
                    }
                } else {
                    endTime = startTime + 5000; // Default 5 seconds for last segment
                }
                
                segments.add(new AudioSegment(startTime, endTime, text));
            }
        }
        
        return segments;
    }
    
    /**
     * Determines if the content is likely to be in JSON format
     * Uses more robust heuristics than just checking the first character
     * @param content The content to analyze
     * @return True if the content is likely JSON, false otherwise
     */
    private static boolean isLikelyJsonFormat(String content) {
        // Trim whitespace
        String trimmed = content.trim();
        
        // If it starts with a curly brace, it's likely JSON
        if (trimmed.startsWith("{")) {
            return true;
        }
        
        // If it starts with a square bracket, we need additional checks
        if (trimmed.startsWith("[")) {
            // Check if the second character is a curly brace or another square bracket
            // which would indicate a JSON array
            if (trimmed.length() > 1 && (trimmed.charAt(1) == '{' || trimmed.charAt(1) == '[')) {
                return true;
            }
            
            // Check if the content contains JSON-like structures
            boolean containsJsonStructures = 
                trimmed.contains("\"start\"") ||
                trimmed.contains("\"text\"") || 
                trimmed.contains("\"end\"");
            
            if (containsJsonStructures) {
                return true;
            }

            // If first line starts with [xx:xx.xx] pattern, it's likely text format
            String firstLine = trimmed.split("\\n")[0].trim();
            if (TIMING_PATTERN.matcher(firstLine).matches()) {
                return false;
            }

            // Check for numeric arrays which could be timestamps in JSON
            if (trimmed.matches("\\[\\s*\\d+.*")) {
                return true;
            }
        }
        
        // Try to parse a sample of the content as JSON
        try {
            // Get a small sample (first 100 chars or less) to avoid performance issues with large files
            String sample = trimmed.substring(0, Math.min(trimmed.length(), 100));
            JsonParser.parseString(sample);
            return true;
        } catch (JsonSyntaxException e) {
            // If parsing fails, check some additional JSON indicators
            boolean hasJsonIndicators = 
                trimmed.contains("\"segments\"") || 
                trimmed.contains("\"startTime\"") || 
                trimmed.contains("\"endTime\"") || 
                (trimmed.contains("{") && trimmed.contains("}")) || 
                (trimmed.contains("[") && trimmed.contains("]"));
            
            return hasJsonIndicators;
        }
    }
    
    /**
     * Parse a JSON timing file containing segments with start/end times and text
     * Supports both array format and object format with segments array
     * @param jsonContent The JSON content string
     * @return List of AudioSegment objects
     */
    private static List<AudioSegment> parseJsonTimingFile(String jsonContent) {
        List<AudioSegment> segments = new ArrayList<>();
        
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonContent);
            JsonArray segmentsArray;
            
            // Handle both array and object format
            if (jsonElement.isJsonArray()) {
                segmentsArray = jsonElement.getAsJsonArray();
            } else if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                // Look for a segments array in the JSON object
                if (jsonObject.has("segments")) {
                    segmentsArray = jsonObject.getAsJsonArray("segments");
                } else {
                    // Try to find any array property
                    segmentsArray = null;
                    for (String key : jsonObject.keySet()) {
                        if (jsonObject.get(key).isJsonArray()) {
                            segmentsArray = jsonObject.getAsJsonArray(key);
                            break;
                        }
                    }
                    
                    if (segmentsArray == null) {
                        // No array found, return empty list
                        Log.e(TAG, "No segments array found in JSON");
                        return segments;
                    }
                }
            } else {
                // Invalid JSON format
                Log.e(TAG, "Invalid JSON format: neither array nor object");
                return segments;
            }
            
            // Process each segment
            for (int i = 0; i < segmentsArray.size(); i++) {
                JsonObject segment = segmentsArray.get(i).getAsJsonObject();
                
                // Extract start time, searching for common key names
                long startTime = -1;
                if (segment.has("start")) {
                    startTime = getTimeInMillis(segment.get("start"));
                } else if (segment.has("startTime")) {
                    startTime = getTimeInMillis(segment.get("startTime"));
                } else if (segment.has("start_time")) {
                    startTime = getTimeInMillis(segment.get("start_time"));
                } else if (segment.has("from")) {
                    startTime = getTimeInMillis(segment.get("from"));
                }
                
                // Extract end time, searching for common key names
                long endTime = -1;
                if (segment.has("end")) {
                    endTime = getTimeInMillis(segment.get("end"));
                } else if (segment.has("endTime")) {
                    endTime = getTimeInMillis(segment.get("endTime"));
                } else if (segment.has("end_time")) {
                    endTime = getTimeInMillis(segment.get("end_time"));
                } else if (segment.has("to")) {
                    endTime = getTimeInMillis(segment.get("to"));
                }
                
                // If we're missing start/end time but have following segment, use its start time
                if (i < segmentsArray.size() - 1 && endTime == -1) {
                    JsonObject nextSegment = segmentsArray.get(i + 1).getAsJsonObject();
                    if (nextSegment.has("start")) {
                        endTime = getTimeInMillis(nextSegment.get("start"));
                    } else if (nextSegment.has("startTime")) {
                        endTime = getTimeInMillis(nextSegment.get("startTime"));
                    } else if (nextSegment.has("start_time")) {
                        endTime = getTimeInMillis(nextSegment.get("start_time"));
                    } else if (nextSegment.has("from")) {
                        endTime = getTimeInMillis(nextSegment.get("from"));
                    }
                }
                
                // Default end time if still missing
                if (endTime == -1 && startTime != -1) {
                    endTime = startTime + 5000; // Default 5 seconds if can't determine
                }
                
                // Extract text, searching for common key names
                String text = null;
                if (segment.has("text")) {
                    text = segment.get("text").getAsString();
                } else if (segment.has("content")) {
                    text = segment.get("content").getAsString();
                } else if (segment.has("transcript")) {
                    text = segment.get("transcript").getAsString();
                } else if (segment.has("value")) {
                    text = segment.get("value").getAsString();
                } else {
                    // Look for any string property
                    for (String key : segment.keySet()) {
                        if (segment.get(key).isJsonPrimitive() && 
                            segment.get(key).getAsJsonPrimitive().isString()) {
                            text = segment.get(key).getAsString();
                            break;
                        }
                    }
                }
                
                // Add segment if we have all required fields
                if (startTime != -1 && endTime != -1 && text != null && !text.isEmpty()) {
                    segments.add(new AudioSegment(startTime, endTime, text));
                } else {
                    Log.w(TAG, "Skipping segment missing required fields: " + segment);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON timing file", e);
        }
        
        return segments;
    }
    
    /**
     * Extract time in milliseconds from a JSON element
     * Supports numeric (milliseconds) or string (MM:SS.SS format) values
     */
    private static long getTimeInMillis(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                // Direct milliseconds value
                return element.getAsLong();
            } else if (element.getAsJsonPrimitive().isString()) {
                String timeStr = element.getAsString();
                
                // Try to parse as MM:SS.SS format (with flexible digit count)
                Matcher matcher = TIMING_PATTERN.matcher("[" + timeStr + "] dummy");
                if (matcher.matches()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int centiseconds = Integer.parseInt(matcher.group(3));
                    // For single-digit centiseconds, multiply by 100 instead of 10
                    int multiplier = matcher.group(3).length() == 1 ? 100 : 10;
                    return (minutes * 60 + seconds) * 1000 + centiseconds * multiplier;
                }
                
                // Try to parse as seconds with decimal
                try {
                    double seconds = Double.parseDouble(timeStr);
                    return (long) (seconds * 1000);
                } catch (NumberFormatException ignored) {
                    // Not a valid number format
                }
            }
        }
        
        // Couldn't parse the time
        return -1;
    }
    
    /**
     * Find the current segment based on the current playback time
     * @param segments List of audio segments
     * @param currentTime Current audio playback time in milliseconds
     * @return The index of the current segment or -1 if not found
     */
    public static int findCurrentSegment(List<AudioSegment> segments, long currentTime) {
        if (segments == null || segments.isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < segments.size(); i++) {
            AudioSegment segment = segments.get(i);
            if (currentTime >= segment.getStart() && currentTime < segment.getEnd()) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Auto-generate segments by splitting text into sentences and assigning approximate timestamps
     * This is a fallback when no timing information is provided
     * @param text Full text content
     * @param audioDuration Total audio duration in milliseconds
     * @return List of audio segments with estimated timestamps
     */
    public static List<AudioSegment> autoGenerateSegments(String text, long audioDuration) {
        List<AudioSegment> segments = new ArrayList<>();
        
        if (text == null || text.isEmpty() || audioDuration <= 0) {
            return segments;
        }
        
        // Simple sentence splitting (not perfect for all languages)
        String[] sentences = text.replaceAll("([.!?])\\s+(?=[A-Z])", "$1|").split("\\|");
        
        long avgDuration = audioDuration / sentences.length;
        long currentTime = 0;
        
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (!trimmedSentence.isEmpty()) {
                long endTime = currentTime + avgDuration;
                segments.add(new AudioSegment(currentTime, endTime, trimmedSentence));
                currentTime = endTime;
            }
        }
        
        return segments;
    }
    
    /**
     * Format milliseconds to MM:SS format
     * @param milliseconds Time in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(long milliseconds) {
        int totalSeconds = (int) (milliseconds / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}