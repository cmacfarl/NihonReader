package com.nihonreader.app.utils;

import android.util.Log;

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
    private static final Pattern TIMING_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\]\\s*(.*)");
    
    /**
     * Parse a text file containing timestamps and corresponding text segments
     * Format expected: [00:00.00] Text segment one
     *                  [00:05.25] Text segment two
     * @param content The content of the timing file
     * @return List of AudioSegment objects
     */
    public static List<AudioSegment> parseTimingFile(String content) {
        List<AudioSegment> segments = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return segments;
        }
        
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
                long startTime = (minutes * 60 + seconds) * 1000 + centiseconds * 10;
                String text = matcher.group(4);
                
                // Calculate end time (start time of next segment or end of audio)
                long endTime;
                if (i < lines.length - 1) {
                    Matcher nextMatcher = TIMING_PATTERN.matcher(lines[i + 1].trim());
                    if (nextMatcher.matches()) {
                        int nextMinutes = Integer.parseInt(nextMatcher.group(1));
                        int nextSeconds = Integer.parseInt(nextMatcher.group(2));
                        int nextCentiseconds = Integer.parseInt(nextMatcher.group(3));
                        endTime = (nextMinutes * 60 + nextSeconds) * 1000 + nextCentiseconds * 10;
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