# Nihon Reader Android Implementation

## Overview

Nihon Reader is a Japanese text reader application with synchronized audio playback. This Android implementation allows users to read text while listening to audio narration, with each sentence being highlighted as it's being read. Users can add their own custom stories with associated audio files.

## Key Components

### Data Models
- **Story**: Represents a story with metadata (title, author, description)
- **StoryContent**: Contains the actual content (text, audio URI, segments)
- **AudioSegment**: Represents a segment of text with start/end timestamps
- **UserProgress**: Tracks user progress for a story
- **VocabularyItem**: Represents vocabulary items for study

### Database (Room)
- **AppDatabase**: Main database class
- **StoryDao**: Data access for Story entities
- **StoryContentDao**: Data access for StoryContent entities
- **UserProgressDao**: Data access for UserProgress entities
- **VocabularyDao**: Data access for Vocabulary entities

### Activities
- **MainActivity**: Displays the list of available stories
- **StoryReaderActivity**: For reading stories with synchronized audio
- **AddStoryActivity**: For adding new custom stories

### ViewModels
- **StoryListViewModel**: ViewModel for the story list screen
- **StoryReaderViewModel**: ViewModel for the story reader screen
- **AddStoryViewModel**: ViewModel for the add story screen

### Adapters
- **StoryAdapter**: For displaying stories in a RecyclerView
- **TextSegmentAdapter**: For displaying text segments in a RecyclerView

### Utilities
- **AudioUtils**: Utility functions for audio-text synchronization
- **FileUtils**: Utility functions for file operations

## Features

### Story Management
- View a list of available stories
- View story details (title, author, description)
- Delete stories

### Story Reading
- Synchronized text and audio playback
- Text highlighting for current segment
- Audio controls (play/pause, skip forward/backward)
- Automatic progress saving

### Adding Custom Stories
- Import text and audio files
- Add optional timing information for precise synchronization
- Auto-generate timings when not provided

## Technical Implementation

1. **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern
2. **Database**: Room for local storage
3. **Media Playback**: Android MediaPlayer for audio playback
4. **UI Components**: RecyclerView, Material Design components
5. **Navigation**: Activity-based navigation with extras
6. **File Handling**: Content provider for file selection, local storage for imported files

## How Audio-Text Synchronization Works

1. When a story is imported, timing data is either provided by the user or auto-generated
2. During playback, the MediaPlayer position is monitored using a Handler
3. The current segment is determined by finding which segment's time range contains the current playback position
4. The current segment is highlighted in the RecyclerView
5. The RecyclerView automatically scrolls to keep the current segment visible

## Future Improvements

1. **Vocabulary Tracking**: Implement tapping on words to add them to vocabulary
2. **Text Analysis**: Add Japanese-specific text analysis (furigana, dictionary lookups)
3. **Cloud Sync**: Add ability to sync stories and progress across devices
4. **Enhanced Timing Editor**: Add a visual editor for creating precise timing files
5. **Text-to-Speech**: Add support for generating audio from text for stories without audio