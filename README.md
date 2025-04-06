# Nihon Reader (Android)

A native Android application for reading Japanese transcripts with audio synchronization. Inspired by Satori Reader, but with the ability to add custom stories with associated audio.

## Features

- Text and audio synchronization - text is highlighted as audio plays
- Import custom stories with audio files
- Add timing information to synchronize text with audio
- Simple and intuitive user interface
- Room database for local storage

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/nihonreader/app/
│   │   │   ├── activities/        # Activities for different screens
│   │   │   ├── adapters/          # RecyclerView adapters
│   │   │   ├── database/          # Room database and DAOs
│   │   │   ├── models/            # Entity classes
│   │   │   ├── repository/        # Repository class
│   │   │   ├── utils/             # Utility classes
│   │   │   ├── viewmodels/        # ViewModel classes
│   │   │   └── MainActivity.java  # Main activity
│   │   │
│   │   └── res/                  # Resources (layouts, strings, etc.)
│   │
│   └── test/                     # Unit tests
│
└── build.gradle                  # App module Gradle build file
```

## Requirements

- Android Studio
- Android SDK 21+
- Java 8+

## Getting Started

1. Clone the repository:
```bash
git clone https://github.com/yourusername/nihon-reader.git
cd nihon-reader/NihonReaderAndroid
```

2. Open the project in Android Studio

3. Build and run the app on your device or emulator

## Usage

### Story Library

The home screen displays your story library with a list of available stories:

- **Open a Story**: Tap on any story title to open it in the reader.
- **View Story Details**: Long-press on a story to view complete details including title, author, description, and when you last read it.
- **Story Options**: Tap the three-dot menu icon on the right side of any story to access additional options:
  - **Edit Timestamps**: Adjust the timing synchronization between text and audio.
  - **Delete**: Remove the story from your library (requires confirmation).
- **Add New Story**: Tap the floating "+" button at the bottom right of the screen to add a new story.

### Adding Stories

When adding a new story:

1. Fill in the **Title** and **Author** fields.
2. Add an optional **Description**.
3. Select a **Text File** containing Japanese text (tap "Select Text File").
4. Select an **Audio File** of the narration (tap "Select Audio File").
5. Tap "Add Story" to import your content.

The app will automatically:
- Generate timestamps by splitting the Japanese text into sentences using linguistic analysis
- Assign default timings to each segment (approximately 3 seconds per segment)
- Open the timestamp editor for further refinement

### Reading Stories

When reading a story:

- The audio controls at the bottom allow you to **play/pause**, **skip forward/backward**, and adjust the **playback position**.
- Text segments are highlighted in sync with the audio playback.
- Tap on any text segment to jump to that portion of the audio.

### Editing Timestamps

The timestamp editor allows you to fine-tune the synchronization between text and audio:

- **Play Segment**: Plays the current segment to check timing.
- **Set Start/End Time**: When audio is playing, you can set the current position as the start or end time for any segment.
- **Manual Adjustment**: Directly edit the start and end times in the text fields.
- **Merge Segments**: Click the up arrow in the upper right corner of any segment (except the first one) to merge it with the segment above it. This is useful for combining sentences that should be played together.
- **Save Changes**: Tap the save button (disk icon) at the bottom right when you're satisfied with your edits.

### Automatic Timestamp Generation

When adding a story, the app automatically:
1. Splits the text into logical sentences using Japanese language analysis
2. Creates timestamp segments for each sentence
3. Assigns default timing values to each segment (3 seconds per segment)

You can then refine these automatically generated timestamps in the timestamp editor.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

Uses KANJIDIC2 for the dictionary.

Uses Atilika Kuromoji for Japanese tokenization.
