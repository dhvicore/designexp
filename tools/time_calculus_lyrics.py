"""Derive read-along timing candidates from the eight original Calculus MP3s.
Install faster-whisper and imageio-ffmpeg. Models and reports stay local.
Review reports before adding cues to the shipped catalog. No audio is modified.
"""
import hashlib
import json
import re
import subprocess
import sys
import unicodedata
from pathlib import Path
import imageio_ffmpeg
import numpy as np
from faster_whisper import WhisperModel
from time_saved_lyrics import align


def clean_lines(text):
    return [s.strip() for s in text.splitlines() if s.strip() and not s.strip().startswith(('[', '```')) and s.strip() != 'Verse]']


def unicode_tokens(text):
    return [word for token in unicodedata.normalize('NFKC', text.lower()).split()
            if (word := re.sub(r'[^\w\u0900-\u097f]', '', token))]


if __name__ == '__main__':
    import time_saved_lyrics
    time_saved_lyrics.tokens = unicode_tokens
    root = Path(__file__).resolve().parents[1]
    mode = sys.argv[1]
    model_path = root / 'server/.venv' / ('alignment-model' if mode == 'en' else 'alignment-model-multilingual')
    model = WhisperModel(str(model_path), device='cpu', compute_type='int8', cpu_threads=4)
    rows = json.loads((root / 'app/src/main/assets/demos/catalog.json').read_text())
    for song in rows:
        if song['topicId'] not in (0,17) or (song['category'] in ('English','Lo-fi')) != (mode == 'en'):
            continue
        path = root / 'app/src/main/assets' / song['assetPath']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == song['sha256']
        lines = clean_lines(song['lyrics'])
        report = Path('/tmp') / (path.stem + '-timing.json')
        if report.exists():
            print('Existing report:', report, flush=True)
            continue
        print('Transcribing', path.name, flush=True)
        pcm = subprocess.check_output([imageio_ffmpeg.get_ffmpeg_exe(), '-v','error','-i',str(path),'-ac','1','-ar','16000','-f','f32le','-'])
        audio = np.frombuffer(pcm, dtype=np.float32)
        segments, _ = model.transcribe(audio, language=mode, word_timestamps=True, beam_size=5, initial_prompt=' '.join(lines)[:1200], condition_on_previous_text=False)
        words, sections = [], []
        for segment in segments:
            print(round(segment.start,2), round(segment.end,2), segment.text, flush=True)
            sections.append(dict(start=segment.start,end=segment.end,text=segment.text))
            words.extend(dict(word=w.word,start=w.start,end=w.end) for w in segment.words or [])
        cues, missing = align(lines, words)
        report.write_text(json.dumps(dict(asset= song['assetPath'],sha256=song['sha256'],lines=lines,words=words,segments=sections,cues=cues,missing=missing,duration=len(audio)/16000),ensure_ascii=False,indent=2))
        print('MATCHED',len(cues),'/',len(lines),'MISSING',missing,flush=True)
