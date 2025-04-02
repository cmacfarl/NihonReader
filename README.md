# Nihon Reader (Android)

A native Android application for reading Japanese texts with audio synchronization. Inspired by Satori Reader, but with the ability to add custom stories with associated audio.

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

### Adding Custom Stories

1. Prepare your text and audio files:
   - Text file: Plain text (.txt) file with the story content
   - Audio file: MP3 or other supported audio format
   - (Optional) Timing file: A text file with timestamps and corresponding text segments

2. Timing file format:
```
[00:00.00] Text segment one
[00:05.25] Text segment two
...
```

3. In the app, tap "Add Story" and follow the prompts to import your files.

### Reading Stories

1. Select a story from the home screen.
2. Use the audio controls to play, pause, and navigate through the story.
3. Text segments will be highlighted as the audio plays.

## License

This project is licensed under the MIT License - see the LICENSE file for details.