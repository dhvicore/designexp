# Syllabeat — simple slide-by-slide script

June–November 2026 · About 5 minutes at a relaxed pace, plus the audio demonstration.

## 1. Meet Syllabeat

Good morning. Our project is Syllabeat, a music-based learning app. I will explain our work from June to November, from research and syllabus files to the Android implementation.

## 2. The learning idea

We use the SAT framework: Song, Activity and Technology. Students hear a topic as a song, then answer questions to check their understanding. We still need a study to measure any improvement in retention.

## 3. June–November journey

June focused on research. July produced the Calculus dataset. August added song mapping and AI research. September continued the file preparation. October focused on implementation, and November brought the latest integrations and documentation.

## 4. June & July: the foundation

I researched the approach and organised supporting files. The July logbook records the syllabus work on the twentieth and song categories on the twenty-ninth. We connected concepts and exact formulas with suitable musical styles.

## 5. August & September: lyrics preparation

On August eleventh, I mapped topics to songs. On August eighteenth, I researched lyric and music tools. During September, I continued research and prepared project files. The aim was to keep the educational meaning correct.

## 6. Research & design decisions

Our research suggested combining songs with active practice and grounding lyrics in real syllabus content. We explored different AI tools, but we do not claim that the app has already improved students’ marks.

## 7. The earlier working prototype

The first prototype used Python and Streamlit. It explored speech-to-song conversion, quizzes and Whisper sing-back checking. The robotic voice quality showed us why natural, understandable singing needed more work.

## 8. The curriculum foundation

The app now contains thirty-eight Calculus topics, sixty Chemistry topics and thirty-two Economics topics. Structured JSON files keep the explanation, formula or key terms, lyrics and music choices connected through a topic ID.

## 9. Inside the Android app

After login, the student selects a topic, chooses music, reviews lyrics, listens and completes an activity. Calculus has Hindi, English, bhajan and lo-fi categories. Chemistry and Economics support English, Hindi and Marathi.

## 10. Hear the implementation

We prepared four recordings each for Rolle’s Theorem and Beta Function, plus one Chemistry recording. These nine files play locally. Their song titles are style references; exact commercial-tune matching is not verified.

## 11. Economics without Suno

We integrated Krishhma’s Economics contribution. Here Android records the lyrics using text-to-speech, shapes them into a melody, and adds a generated beat. It saves the audio and lyric timing without requiring Suno or Colab.

## 12. Activities & feedback

The activity section includes blanks, formula or keyword choices, and putting lines in order. Students receive feedback and can retry. This adds a check of understanding after listening.

## 13. Under the hood

The app uses Kotlin and Jetpack Compose in Android Studio. JSON holds the syllabus, SQLite stores local accounts, and Media3 plays the audio. Timed lyric cues connect the playback position to the highlighted line.

## 14. The code in simple terms

SongStudio controls the screens. Curriculum loads subject data. AccountStore handles accounts. EconomicsAudio creates the local track. LocalSongEngine and TimedLyrics handle listening, while LearningActivity creates and checks the practice tasks.

## 15. From prototype to app

Across the semester we moved through data preparation, a Python prototype, AI audio experiments and the Android app. We improved login persistence, playback buffering, lyric display, quizzes and the visual design, then added more subjects.

## 16. Testing & current limits

Checks covered accounts, curriculum mappings, audio playback and practice. The latest Economics checks generated and played samples in three languages. These are technical checks, not proof of natural singing quality or better learning outcomes.

## 17. The next chapter

Next we want to add Physics and Biology, verified academic selections, saved history and cloud sync. We also want better singing, teacher review, sing-back feedback and a student evaluation of recall.

## 18. The takeaway

This semester produced both the supporting research and a working app. Syllabeat now combines three subjects, educational audio, read-along lyrics and activities. Our next priority is improving quality and testing its usefulness with students. Thank you.

## Short technical answer for questions

Kotlin is the programming language, and Jetpack Compose draws the Android screens. JSON files hold the subject content. SQLite stores local accounts with salted password hashes. Media3 plays audio files. Lyric cues store when each line starts and ends. Economics uses Android TTS plus pitch and syllable processing to make a generated track. This is not a newly trained singing model.
