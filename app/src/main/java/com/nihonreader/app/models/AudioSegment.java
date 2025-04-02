package com.nihonreader.app.models;

/**
 * Model class for an audio segment - representing a piece of text with
 * associated start and end timestamps
 */
public class AudioSegment {
    private long start; // Start time in milliseconds
    private long end;   // End time in milliseconds
    private String text; // The text segment

    public AudioSegment(long start, long end, String text) {
        this.start = start;
        this.end = end;
        this.text = text;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}