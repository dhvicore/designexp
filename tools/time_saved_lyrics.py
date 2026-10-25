"""Author English lyric cues locally using faster-whisper. MP3s are never modified.
Install: pip install faster-whisper. Run: python tools/time_saved_lyrics.py MODEL_DIR
Exported timestamps need listening review, particularly for sung or repeated words.
"""
import hashlib
import difflib
import json
import re
import sys
from pathlib import Path


def tokens(text):
    return re.findall(r'[a-z0-9]+', text.lower())


def align(lines, words):
    source, owners, heard, spans = [], [], [], []
    for i, line in enumerate(lines):
        parts = tokens(line)
        source.extend(parts)
        owners.extend([i] * len(parts))
    for word in words:
        parts = tokens(word['word'])
        heard.extend(parts)
        spans.extend([(word['start'], word['end'])] * len(parts))
    anchors = {i: [] for i in range(len(lines))}
    for block in difflib.SequenceMatcher(None, source, heard, autojunk=False).get_matching_blocks():
        for n in range(block.size):
            anchors[owners[block.a + n]].append(spans[block.b + n])
    cues, missing = [], []
    for i, line in enumerate(lines):
        times = anchors[i]
        if len(times) < min(2, len(tokens(line))):
            missing.append(line)
            continue
        start, end = round(times[0][0] * 1000), round(times[-1][1] * 1000)
        if end > start:
            cues.append(dict(startMs=start, endMs=end, text=line))
    for a, b in zip(cues, cues[1:]):
        a['endMs'] = min(a['endMs'] + 250, b['startMs'])
    return cues, missing


if __name__ == '__main__':
    from faster_whisper import WhisperModel
    root = Path(__file__).resolve().parents[1]
    catalog = root / 'app/src/main/assets/demos/catalog.json'
    rows = json.loads(catalog.read_text())
    song = next(r for r in rows if r['topicId'] == 1001 and r['category'] == 'English')
    audio_path = root / 'app/src/main/assets' / song['assetPath']
    if hashlib.sha256(audio_path.read_bytes()).hexdigest() != song['sha256']:
        raise RuntimeError('Source recording changed; review its lyrics before timing it.')
    lines = [s.strip() for s in song['lyrics'].splitlines() if s.strip() and not s.startswith('[')]
    model = WhisperModel(sys.argv[1], device='cpu', compute_type='int8', cpu_threads=4)
    import imageio_ffmpeg
    import subprocess
    import numpy as np
    pcm = subprocess.check_output([imageio_ffmpeg.get_ffmpeg_exe(), '-v', 'error', '-i', str(root / 'app/src/main/assets' / song['assetPath']), '-ac', '1', '-ar', '16000', '-f', 'f32le', '-'])
    audio = np.frombuffer(pcm, dtype=np.float32)
    segments, _ = model.transcribe(audio, language='en', word_timestamps=True, beam_size=5, initial_prompt='Dalton atomic theory, atoms, elements, whole number ratios, isotopes.', condition_on_previous_text=False)
    words = []
    for s in segments:
        print(s.start, s.end, s.text, flush=True)
        words.extend(dict(word=w.word, start=w.start, end=w.end) for w in s.words or [])
    Path('/tmp/dalton-word-timestamps.json').write_text(json.dumps(words, indent=2))
    cues, missing = align(lines, words)
    print('Matched', len(cues), '/', len(lines), 'Missing:', missing, flush=True)
    Path('/tmp/dalton-cues.json').write_text(json.dumps(cues, indent=2))
    # The recognizer hears "at times joint" and "Adams joining information".
    # Use the measured segment boundaries for these two sung source lines.
    if song['sha256'] == 'b8826a968cb89d7b0a392692f06bb9521710a74f0c96033a38327c076104d98d':
        repairs = {8: (32160, 36100), 17: (62000, 64450)}
        for index, (start, end) in repairs.items():
            if lines[index] in missing:
                cues.append(dict(startMs=start, endMs=end, text=lines[index]))
                missing.remove(lines[index])
        cues.sort(key=lambda cue: cue['startMs'])
        for a, b in zip(cues, cues[1:]):
            a['endMs'] = min(a['endMs'], b['startMs'])
        cues[-1]['endMs'] = 114260
    if missing:
        raise RuntimeError('Review unmatched lyric lines before exporting.')
    song['cues'] = cues
    song['cueMethod'] = 'Automatic matching of supplied lyrics to recognized word timestamps; approximate'
    catalog.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + '\n')
