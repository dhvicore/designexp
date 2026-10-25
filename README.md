# Syllabus Song Studio — Android

Kotlin / Jetpack Compose app with 38 Calculus topics and 60 Chemistry topics.
Repository: https://github.com/dhvicore/designexp

## Run the offline demo first

1. Clone `https://github.com/dhvicore/designexp.git` and open its root folder in Android Studio.
2. Let Gradle sync; use SDK 36 and Android Studio's bundled JDK.
3. Run the `app` configuration on an Android emulator. Sign up on that device, then log in.
4. Choose **Unit 1 → Rolle’s Theorem** or **Unit 3 → Beta Function**.
5. Choose **Hindi, English, Bhajan or Lo-fi**. The saved recording is the first recommendation.
6. Tap **Load saved song lyrics → Continue to song → Play song**.
7. Tap **Try the activity →** for Step 5: blanks, formula selection and lesson ordering.

These eight supplied Suno recordings are packaged in `app/src/main/assets/demos/`.
They play offline through the buffered Media3 player; **Colab and the Mac server are not required**
for them. The read-along text comes from each MP3's supplied lyrics metadata.
Only the mapped topic/category/song combination uses each recording. Editing a
lyric draft does not change its saved audio. Exact named-song melody matching
remains unfinished; do not present these recordings as verified exact covers.

| Topic | English | Hindi | Bhajan | Lo-fi |
|---|---|---|---|---|
| Rolle’s Theorem | Levitating | Kala Chashma | Raghupati Raghav Raja Ram | Perfect |
| Beta Function | Shape of You | Chaiyya Chaiyya | Om Jai Jagdish Hare | Sunflower |

Step 5 adapts the existing Python prototype's activity types, works offline for
all 38 Calculus topics, and gives feedback, scores and retry. Rolle and Beta have
curated understanding checks. Practice is currently in English. The prototype's
bonus microphone/Whisper sing-back has not been ported.

Verification includes Android build/lint, native MP3 decoding/playback checks, three lyric-timing tests, and a Media3 playback test covering all nine bundled recordings with no reported audio underruns. Playback checks establish technical playback, not musical fidelity or lyric intelligibility.

## Optional experimental singing engine with Colab

1. Open [the GPU notebook](https://colab.research.google.com/github/dhvicore/designexp/blob/main/notebooks/Calculus_Singing_GPU.ipynb).
2. Choose **Runtime → Change runtime type → T4 GPU**.
3. Run cells 1 and 2. Dependencies and several GB of weights download on the first run.
   Cell 2 sets Matplotlib to `Agg` and enables float32 for the T4 run. The pinned
   upstream loader needs the notebook's small, opt-in precision patch to honor
   `ACESTEP_DTYPE`; its error hint alone does not implement that environment flag.
4. Run cell 3. It must produce a real sung Calculus clip and an audio player. If it
   errors, resolve that error before claiming the app's generation is working.
5. Run cell 4. It starts a temporary API-key-protected service and downloads
   `song-service.env`. Move that file into `server/` and rename it **`.env`**.
   Do not share its contents or commit it. The existing `.gitignore` excludes it.
6. On the Mac, double-click `server/Start Music Server.command`; keep its terminal
   and the Colab runtime open. The bridge binds only to the Mac's loopback address.
7. Run the Android app on the emulator. Log in, select a topic, choose music,
   create/review lyrics and press **Generate song**. After completion, press Play.
   For the experimental melody workflow: **Unit 1 → Rolle’s Theorem → Bhajan →
   Raghupati Raghav Raja Ram → Follow melody → Generate lyric draft →
   Continue to song → Generate song → Play song**. Use Browse all if needed.

The bridge reads `.env` on requests, so a new Colab endpoint can be configured
without rebuilding the APK. The endpoint changes when the temporary tunnel is
restarted. If Colab stops, new generation stops; completed app audio is kept offline.

This is an emulator demo: Android talks to the Mac at `10.0.2.2:8787`, and the Mac
calls the GPU API over HTTPS. No student account details are sent to the GPU;
only curriculum lyrics and musical parameters are submitted. Physical-device
networking and a production-hosted backend are not configured.

## What is implemented

- Persistent device-local signup/login with salted PBKDF2 password hashes, email
  validation and duplicate-account checks. The last successful signup/login email is
  prefilled after relaunch; existing account databases also supply a prefill on upgrade.
  Passwords must be entered again. Backgrounding ends the session; rotation
  preserves it. There is no SAP/portal authentication or cross-device account sync.
- **Calculus Units 1–5: 38 topics**, distributed 9 / 5 / 5 / 8 / 11, using the source
  explanations, formulas and multilingual lyric content from the Python prototype.
- Exactly four initial music choices: **Hindi, English, Bhajan, Lo-fi**.
- Four recommendations vary by topic; **Browse all songs** expands the category's
  catalog. Bollywood includes Chikni Chameli and Chammak Challo.
- Lyric drafts change language/form, phrase grouping and refrain with category and
  song selection. They are locally assembled from syllabus lyrics, not a live LLM.
  Bhajan uses devotional Hindi with a Sanskrit invocation. Subject owners still
  need to review source translations and mathematical assumptions.
- Three built-in instrumental melody guides: Raghupati Raghav Raja Ram,
  Om Jai Jagdish Hare and Vaishnav Jan To. Bhajan recommendations include a guide.
  These are newly rendered notation guides, with approximate reconstructed rhythm,
  rather than the prototype's finished example recordings.
- With **Follow melody**, the bridge fits lyric phrase boundaries to syllable budgets,
  repeats the musical cycle for longer lessons, and sends actual guide audio with
  the lyrics to the model's cover endpoint. The Rolle/Raghupati demo uses a curated
  six-phrase Hindi educational verse in Devanagari with a devotional invocation. Other topics use estimated metrical reflow,
  not semantic AI rewriting or precise phoneme-to-note alignment.
- Generated recordings are normalized to approximately -14 LUFS with a -1.5 dB
  true-peak target. Cached server recordings are normalized on reuse, without another
  GPU job. The player shows device media volume and offers an increase-volume button;
  the activity routes hardware volume keys to music. Already downloaded app files
  receive the normalized server copy when Generate song is pressed again.
- The final song screen displays the full prepared lyrics beneath the player,
  preserving line breaks and section labels. Text is selectable and scrolls with
  the page; there is no timed highlighting or automatic line tracking yet.
- The app submits real generation jobs, polls progress, downloads completed audio,
  and plays it with pause/seek/replay. Failure is shown as failure, not fake success.
- The old import screen, prompt-copy controls and Twinkle recordings have been removed.
  Eight explicitly labelled saved Suno demos are available through the offline flow above.

## Important generation distinction

**Exact famous-song melodies are not supplied by their titles.** Without melody
reference data, the model creates an **original melody in the requested style**;
that mode is explicitly labelled in the app. Changing phrase grouping does not
prove an exact tune fit. The supplied-reference cover path also needs listening
and alignment checks before it can be described as faithful to a selected tune.

To add a melody dataset, place an authorized reference WAV in `server/melodies/`
using the song ID filename, e.g. `chikni-chameli.wav`. This is developer setup,
not a student import step. The app discovers available references through `/melodies`; **Follow melody** uses
that audio for cover mode, while **Original style** explicitly composes a new tune.
A request for an unavailable melody fails instead of silently switching modes.
Cover mode preserves the guide's BPM. Cache keys include the reference audio hash.

An optional matching `chikni-chameli.json` sidecar supplies phrase information:

```json
{
  "title": "Chikni Chameli",
  "bpm": 120,
  "duration_seconds": 35.0,
  "phrase_syllables": [12, 12, 10, 12]
}
```

The numbers above illustrate the schema; measure the real melody before using them.
Use mono/stereo PCM WAV with a clear melody and no original vocal lyrics. Phrase
counts must describe the full guide in order. Refresh the app's music choices after
adding references. Without phrase counts, audio conditioning is supported but the
lyrics are left unchanged and the app reports missing timing metadata.
MIDI/hummed references must first be rendered/prepared as an appropriate WAV.
Do not upload arbitrary YouTube recordings or train on music without the needed rights.

The model is ACE-Step 1.5, run from its official source. This project uses pretrained
inference; it does not train a new singing model. The public hosted demos failed
with GPU/quota errors during our checks; the Colab notebook is the selected route.
On 25 October 2026, a live Colab cover job returned a 35.72-second MP3. The Android
instrumentation test also completed the emulator → Mac → Colab → download round trip,
then verified MediaPlayer playback, pause and seek. This verifies functioning audio,
not perfect melody reproduction, lyric intelligibility or academic correctness;
those still require listening and subject review. Keep Colab running for new jobs.

## Source and ownership

Kotlin source root: `app/src/main/java/com/nmims/songstudio/`.

| Person | Responsibility / files |
|---|---|
| Dhvani | UI/UX and customization: `ui/`, `data/MusicChoices.kt`; lyric drafting: `generation/LyricPrompts.kt` |
| Suraj | Singing integration: `generation/SingingClient.kt`, `server/`, Colab notebook; improve real lyric generation/alignment with Dhvani |
| Aditri | Signup/login/profile/session: `auth/`, `MainActivity.kt` |
| Krishhma | Academic selections and curriculum database: `data/Curriculum.kt`, assets, academic UI |
| Aman | Playback and audio persistence: `playback/`; extend with saved-library/history browsing |

All five supply and validate their own subject datasets, keywords/formulas, song
selections and topic mappings. Integration, screenshots and testing are shared.
Coordinate edits to `ui/SongStudio.kt` and extract module screens as work expands.

## Reusing the existing prototype

`tools/import_prototype.py` validates and refreshes `topics.json` and
`song_catalog.json` from the sibling original `prototype/` folder. It imports no
sample audio. Only Calculus is exposed; other subjects await the team's real data.
Branch/year/semester mappings must likewise come from the verified curriculum.

## Build and tests

Open this repository folder in Android Studio and sync. SDK 36 and Android Studio's
JDK work with the included Gradle wrapper.

```sh
./gradlew :app:assembleDebug :app:lintDebug
tools/run_emulator_tests.sh
cd server
python3 -m unittest discover -v
```

Emulator tests cover persistent accounts, invalid login/signup, background logout,
rotation, all Calculus units, music recommendations, lyric variations and native
playback. The batch test plays each of the eight supplied Suno recordings, pauses,
seeks near the end and verifies completion. The older import tests use a test signal.
Python adapter tests mock the GPU service to check authentication headers, job
polling, error handling and reference uploads. These are not proof of live model
output or exact melody fidelity.

The optional real-GPU integration test consumes a generation and checks returned
mode, fitted Rolle conditions, MP3 duration and native playback:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nmims.songstudio.LiveSingingTest \
  -Pandroid.testInstrumentationRunnerArguments.liveSingingTest=true
```

It is skipped by default. Melody similarity is not asserted by this test.

## GitHub workflow

Clone the same repository, branch from updated `main`, build/test a small change,
then push your feature branch and open a PR for teammate review. Branches:
`feature/ui-lyrics`, `feature/song-synthesis`, `feature/login-profile`,
`feature/academic-data`, `feature/player-library`.

Keep API keys, local account data, temporary generated audio, model weights and SDK paths
out of GitHub. The eight supplied offline demo MP3s are intentionally included; temporary
server output, local experiments, `.env`, virtual environments and build outputs are ignored.

## References

- https://github.com/ace-step/ACE-Step-1.5
- https://github.com/ace-step/ACE-Step-1.5/blob/main/docs/en/API.md
- https://github.com/ace-step/ACE-Step-1.5/blob/main/docs/en/INSTALL.md
- https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/
- https://developer.android.com/develop/ui/compose/documentation

## Rolle’s Theorem and selected song references

Choose Calculus → Unit 1 → Rolle’s Theorem → choose a category → choose a song.
Hindi, English, Bhajan and Lo-fi are a separate category screen before the song list.
Kala Chashma is an example in the Hindi catalog, not a fixed choice for this topic.
The prepared Hindi draft states continuity on the closed interval, differentiability
inside, equal endpoint values, and the existence of an interior zero derivative.
This is educational source text, not a verified fit to the Kala Chashma chorus.
The English category continues to use English lyrics and English song choices.

The missing input is the actual chorus melody. Once a suitable melody/hummed audio
file is supplied, prepare it on the Mac using the existing server environment:

```sh
server/.venv/bin/python tools/register_melody.py /absolute/path/to/chorus.wav \
  --song-id kala-chashma --title "Kala Chashma" --bpm 112 \
  --start 0 --seconds 30
```

Choose the correct chorus start/length and measure BPM for your supplied recording;
112 is catalog metadata, not a measurement of your file. Add `--phrase-syllables`
with comma-separated measured counts to enable approximate lyric phrase fitting.
The tool trims and converts the supplied audio to PCM WAV and records real duration.
It stores the reference under ignored `server/melodies/`; no source audio is committed.
It does not scrape websites, train a model, or generate a placeholder melody.

Reopen the app so its melody list refreshes, select **Follow melody**, then regenerate
and review the lyrics before generating the song. Colab uses the source audio for
cover conditioning. Listening and revision are required to check melody and words;
this is not a guarantee of exact chorus matching. An existing Suno Rolle song can
be a separate quality baseline, but it is not Kala Chashma unless its melody matches.

### Preserve the demo account while testing

Use `tools/run_emulator_tests.sh` to build and install both APKs with `adb install -r`,
then run instrumentation. It never uninstalls the app or clears its data. The default
Gradle connected-test runner used during earlier development was configured with
`uninstall_after_test: true`, which can remove local accounts. Normal Android Studio
Run / in-place APK updates retain accounts. Uninstalling, clearing app data or wiping
the emulator removes device-local accounts; email prefill is not account recovery.

Login distinguishes a missing local account from an incorrect password. Passwords
remain case-sensitive. No password reset or authentication bypass is added.

### Expired Colab connection

If generation reports an unreachable Colab address, keep the runtime connected and
rerun cell 4 to download fresh `song-service.env`; replace `server/.env` with it.
If the model process stopped, rerun cell 2 first and wait for MODEL READY. Cell 4
now verifies the authenticated local model API and replaces its previous tunnel
process when rerun. Existing notebook tabs need the updated cell 4 code after a
GitHub update. The Mac bridge reads `.env` on requests; no APK rebuild is needed.
Never share the API key. A working bridge health check alone does not prove Colab
is reachable. The UI now gives recovery instructions instead of a raw DNS error.

### Colab float16 NaN recovery

If the server traceback reports `NaN or Inf latents` with `dtype=torch.float16`,
use `notebooks/Repair_Colab_Precision.py` in the existing Colab session. It patches
only the pinned loader's precision override, stops only an ACE-Step listener on
port 8001, retains its current API key, and relaunches with float32, LLM disabled,
Agg plotting and CPU offload for support models. The main DiT remains on GPU,
matching the previously working float32 demo configuration. It reuses downloaded
weights. Wait for MODEL READY, rerun cell 3, then cell 4 and refresh `server/.env`.
The helper's loader patch was checked locally; the repaired GPU run still needs
verification in the user's Colab session.

## Score-conditioned singing dataset work

See [datasets/README.md](datasets/README.md) for the checked source registry, four
collected/validated English singing examples (42.13 seconds), local-data fetch and
preparation scripts, and the Rolle lesson specification. This is an ingestion
pilot, not completed training. No aligned Hindi singing corpus or verified exact
famous-song educational render has been obtained. The existing ACE-Step bridge
has not been replaced by a score-controlled singing model in this change.


### Chemistry curriculum

Chemistry is available alongside Calculus: 60 topics across six units and supplementary sections, with English, Hindi and Marathi lyrics, explanations, keywords and song references. Imported from Aman's commit `57c6500` in the original `design-experience` repository. Select Chemistry, then a unit/topic and language, prepare/edit the supplied lyrics, and complete the practice activity. Dalton’s Atomic Theory has a bundled English Suno recording with its embedded lyrics. Other Chemistry selections still need recordings or the configured music service. The eight bundled Calculus recordings remain available.


### Timed lyrics

The bundled English Dalton’s Atomic Theory recording has 30 line cues derived from its audio using local word/segment recognition. Read-along highlighting follows the native player clock, including pause, seek and replay; tapping a line seeks to it. Timing is approximate for sung words. All eight Calculus recordings also have their own acoustic line timings. The one-minute Rolle Hindi dance file ends during its final refrain; unsung remainder lines stay available in full lyrics without invented timestamps. The original MP3 is unchanged. `tools/time_saved_lyrics.py` is an optional desktop authoring tool, not an Android or server dependency.


### Frontend and local playback updates

The app uses the bundled OFL-licensed Outfit font, cream/lime/lavender gradients, original forest-green header artwork and five-step progress navigation. The duplicate saved-song information card has been removed. Media3 plays the original local MP3s with larger audio buffers; lyric cues use its playback clock. No speech recognition or lyric processing runs during Android playback.

For the Mac emulator, hardware graphics reduced the audio timing spikes observed with SwiftShader software rendering. If your emulator has similar crackling, select hardware graphics in its device settings and cold-boot without wiping data. The local demo uses `Pixel_10a` with `hw.gpu.mode=host` and unused microphone input disabled (`hw.audioInput=no`). These are emulator settings, not macOS sound changes or APK requirements. A normal cold boot preserves app data; do not choose Wipe Data.

Optional timing-authoring tools under `tools/` require faster-whisper, NumPy and imageio-ffmpeg on the desktop. They need local Whisper models and output candidate reports for review; model weights and temporary reports are not committed. Android only reads the exported catalog cues. Approximate timings may need listening corrections, and lyrics absent from a shortened recording are not assigned invented timings.
