"""Align known Hindi Calculus lyrics to each original recording, on the desktop.
Uses Whisper cross-attention on overlapping acoustic windows, not a uniform timer.
Audio is read only. Outputs candidates for validation before catalog import.
Requires the optional faster-whisper and imageio-ffmpeg authoring environment.
"""
import json
import subprocess
import sys
from pathlib import Path
import numpy as np
import imageio_ffmpeg
from faster_whisper import WhisperModel
from faster_whisper.tokenizer import Tokenizer
from faster_whisper.audio import pad_or_trim
from time_calculus_lyrics import clean_lines, unicode_tokens
import time_saved_lyrics
time_saved_lyrics.tokens = unicode_tokens

root = Path(__file__).resolve().parents[1]
rows = json.loads((root/'app/src/main/assets/demos/catalog.json').read_text())
m = WhisperModel(str(root/'server/.venv/alignment-model-multilingual'), device='cpu',compute_type='int8',cpu_threads=1)
t = Tokenizer(m.hf_tokenizer,m.model.is_multilingual,task='transcribe',language='hi')
for song in rows:
    if song['topicId'] not in (0,17) or song['category'] not in ('Hindi','Bhajan'):continue
    output=Path('/tmp')/(Path(song['assetPath']).stem+'-forced.json')
    if output.exists():continue
    print('ALIGN',song['assetPath'],flush=True)
    audio=np.frombuffer(subprocess.check_output([imageio_ffmpeg.get_ffmpeg_exe(),'-v','error','-i',str(root/'app/src/main/assets'/song['assetPath']),'-ac','1','-ar','16000','-f','f32le','-']),dtype=np.float32)
    lines=clean_lines(song['lyrics']); cues=[]; offset=0.0; index=0
    for attempt in range(30):
        if index>=len(lines) or offset>=len(audio)/16000:break
        chunk=audio[int(offset*16000):int((offset+30)*16000)]
        candidate=lines[index:index+10]
        while len(t.encode(' '.join(candidate)))>400:candidate=candidate[:-1]
        f=m.feature_extractor(chunk);encoder=m.encode(pad_or_trim(f))
        words=m.find_alignment(t,[t.encode(' '.join(candidate))],encoder,min(f.shape[-1],3000))[0]
        # Cross-attention includes the instrumental lead-in in the first word.
        # Cap unusually long leading words like Whisper's own timestamp refinement.
        for w in words:
            if w['end']-w['start']>1.2:w['start']=w['end']-1.2
        part,_=time_saved_lyrics.align(candidate,words)
        accepted=[]
        final=len(chunk)/16000<29.5
        for i,c in enumerate(part):
            if c['text']!=candidate[i] or c['endMs']<=c['startMs']:break
            if not final and c['endMs']>27000:break
            if c['endMs']>len(chunk)*1000/16000+50:break
            accepted.append(c)
        if not accepted:
            print('NO PROGRESS at',index,offset,part,flush=True)
            if len(chunk)/16000 >= 29.5:
                offset += 15.0
                continue
            break
        for c in accepted:
            c['startMs']+=round(offset*1000);c['endMs']+=round(offset*1000)
        cues.extend(accepted); index+=len(accepted);offset=cues[-1]['endMs']/1000
        print('lines',index,'/',len(lines),'through',offset,flush=True)
    output.write_text(json.dumps(dict(assetPath=song['assetPath'],lines=lines,cues=cues,duration=len(audio)/16000),ensure_ascii=False,indent=2))
    print('RESULT',len(cues),'/',len(lines),flush=True)
