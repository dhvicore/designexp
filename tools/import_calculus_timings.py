"""Validate and import the eight locally derived timing reports. Audio is read only.
Run the transcription/forced-alignment authoring tools first. Timing remains approximate.
"""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
p = root/'app/src/main/assets/demos/catalog.json'
rows = json.loads(p.read_text())
# Recognition of sung maths symbols is imperfect. These boundaries come from
# the recognized word/segment intervals, rather than distributing time evenly.
repairs = {
    'beta_bhajan_om_jai_suno': {18: (69580,76120), 19: (76120,82540)},
    'beta_english_shape_of_you_suno': {10: (29520,31600), 18: (48380,50760)},
    'beta_lofi_sunflower_suno': {12: (62560,65320), 13: (65320,68310), 17: (81320,87260), 18: (87900,90480), 19: (90480,94010)},
}
for song in rows:
    if song['topicId'] not in (0,17):continue
    path = root/'app/src/main/assets'/song['assetPath']
    assert hashlib.sha256(path.read_bytes()).hexdigest() == song['sha256']
    english = song['category'] in ('English','Lo-fi')
    suffix = '-timing.json' if english else '-forced.json'
    report = json.loads((Path('/tmp')/(path.stem+suffix)).read_text())
    lines = report['lines']
    cues = report['cues']
    for index,(start,end) in repairs.get(path.stem,{}).items():
        text = lines[index]
        # Only repair the intended occurrence in an ordered repeated chorus.
        if path.stem == 'beta_english_shape_of_you_suno' and index == 18:
            cues = [c for c in cues if not (c['text']==text and c['startMs']>45000)]
        elif path.stem == 'beta_lofi_sunflower_suno' and index in (18,19):
            cues = [c for c in cues if not (c['text']==text and c['startMs']>75000)]
        elif path.stem == 'beta_bhajan_om_jai_suno' and index == 18:
            cues = [c for c in cues if not (c['text']==text and c['startMs']>60000)]
        else:
            cues = [c for c in cues if c['text'] != text]
        cues.append(dict(startMs=start,endMs=end,text=text))
    cues.sort(key=lambda c:c['startMs'])
    for a,b in zip(cues,cues[1:]):a['endMs'] = min(a['endMs'],b['startMs'])
    assert len(cues) >= len(lines)*.8, song['title']
    assert all(0<=c['startMs']<c['endMs']<=report['duration']*1000+20 and c['text'] in lines for c in cues),song['title']
    song['cues'] = cues
    song['cueMethod'] = 'Acoustic word alignment to this recording; approximate' if english else 'Acoustic forced alignment of supplied Hindi lyrics to this recording; approximate'
    song['timedLineCount'] = len(cues)
    song['suppliedLineCount'] = len(lines)
    print(song['title'],len(cues),'/',len(lines))
p.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
