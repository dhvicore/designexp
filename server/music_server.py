"""Local demo bridge: Android -> ACE-Step singing model -> generated audio.

Starts on loopback only. No account details are sent upstream. No paid provider,
website scraping, imported finished songs or speech-only substitution is used.
"""
import hashlib
import json
import wave
import shutil
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from uuid import uuid4
import melody_library
from lyric_alignment import fit, repeats_for
from audio_loudness import normalize, ensure_normalized

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "generated"
OUTPUT.mkdir(exist_ok=True)
JOBS = {}
LOCK = threading.Lock()
GPU_LOCK = threading.Lock()
SPACE = "ACE-Step 1.5 / configured GPU API"


def generate(job_id, request):
    job = JOBS[job_id]
    try:
        with GPU_LOCK:
            job.update(status="running", message="The singing model is creating vocals and music…")
            profile = melody_library.lookup(request["song_id"]) if request["melody_mode"] == "reference" else None
            if request["melody_mode"] == "reference" and not profile:
                raise ValueError("The selected melody reference was removed. Refresh the music choices.")
            melody = profile["path"] if profile else ROOT / "missing-reference.wav"
            if profile:
                request = {**request, "bpm": profile.get("bpm", request["bpm"])}
            if profile:
                repeats = repeats_for(request["lyrics"], profile, request["language"])
                if repeats > 1:
                    repeated = OUTPUT / (job["signature"] + "-reference.wav")
                    with wave.open(str(melody), "rb") as source:
                        params = source.getparams()
                        frames = source.readframes(source.getnframes())
                    with wave.open(str(repeated), "wb") as output:
                        output.setparams(params)
                        output.writeframes(frames * repeats)
                    melody = repeated
            target = OUTPUT / (job["signature"] + ".mp3")
            from local_model import render
            mode = render(request, melody, target, lambda message: job.update(message=message))
            if not target.is_file() or target.stat().st_size < 1000:
                raise RuntimeError("The singing model returned no playable audio.")
            job.update(message="Adjusting recording loudness…")
            loudness = normalize(target)
            metadata = {"loudness": loudness, "mode": mode, "topic": request["topic"], "song": request["song"], "lyrics": request["lyrics"], "reference_sha256": profile["audio_sha256"] if profile else None}
            target.with_suffix(".json").write_text(json.dumps(metadata, ensure_ascii=False))
            job.update(status="ready", message="Your song is ready.", audio_path=f"/audio/{target.name}", mode=mode)
    except Exception as exc:
        # Surface quota/auth/provider failures; never bypass an upstream restriction.
        job.update(status="failed", message="Music generation failed: " + str(exc)[:500])


def validate(body):
    if not isinstance(body, dict):
        raise ValueError("Expected a JSON object.")
    for key in ("topic", "song", "song_id", "lyrics", "style_prompt", "language"):
        if not isinstance(body.get(key), str) or not body[key].strip():
            raise ValueError(f"Missing {key}.")
    if len(body["lyrics"]) > 4096 or len(body["style_prompt"]) > 512:
        raise ValueError("Lyrics or music description is too long.")
    if not body["song_id"].replace("-", "").isalnum():
        raise ValueError("Invalid song id.")
    bpm = body.get("bpm")
    if not isinstance(bpm, int) or not 30 <= bpm <= 250:
        raise ValueError("Invalid tempo.")
    if body["language"] not in ("en", "hi", "mr", "sa"):
        raise ValueError("Unsupported lyric language.")
    mode = body.get("melody_mode", "original")
    if mode not in ("reference", "original"):
        raise ValueError("Invalid melody mode.")
    if mode == "reference" and not melody_library.lookup(body["song_id"]):
        raise ValueError("This song's melody reference has not been added yet. Choose an available reference or original music.")
    body["melody_mode"] = mode
    return body


def signature_for(body):
    profile = melody_library.lookup(body["song_id"]) if body["melody_mode"] == "reference" else None
    contract = {**body, "pipeline_version": 3, "reference_sha256": profile["audio_sha256"] if profile else None}
    return hashlib.sha256(json.dumps(contract, sort_keys=True).encode()).hexdigest()


class Handler(BaseHTTPRequestHandler):
    def send_json(self, status, value):
        data = json.dumps(value, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == "/melodies":
            return self.send_json(200, {"melodies": melody_library.available()})
        if self.path == "/health":
            return self.send_json(200, {"ok": True, "provider": SPACE})
        if self.path.startswith("/jobs/"):
            job = JOBS.get(self.path.removeprefix("/jobs/"))
            return self.send_json(200 if job else 404, job or {"message": "Unknown generation job."})
        if self.path.startswith("/audio/"):
            name = self.path.removeprefix("/audio/")
            if len(name) == 68 and name.endswith(".mp3") and all(c in "0123456789abcdef" for c in name[:-4]):
                path = OUTPUT / name
                if path.is_file():
                    self.send_response(200)
                    self.send_header("Content-Type", "audio/mpeg")
                    self.send_header("Content-Length", str(path.stat().st_size))
                    self.end_headers()
                    with path.open("rb") as source:
                        shutil.copyfileobj(source, self.wfile)
                    return
        self.send_json(404, {"message": "Not found."})

    def do_POST(self):
        if self.path not in ("/generate", "/adapt-lyrics"):
            return self.send_json(404, {"message": "Not found."})
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 24000:
                raise ValueError("Invalid request size.")
            body = validate(json.loads(self.rfile.read(length)))
            if self.path == "/adapt-lyrics":
                profile = melody_library.lookup(body["song_id"]) if body["melody_mode"] == "reference" else None
                if not profile:
                    return self.send_json(200, {"lyrics": body["lyrics"], "method": "style_draft", "note": "Original music will be composed around this draft."})
                return self.send_json(200, fit(body["lyrics"], profile, body["topic"], body.get("category", ""), body["language"]))
            signature = signature_for(body)
            with LOCK:
                for job_id, job in JOBS.items():
                    if job["signature"] == signature and job["status"] != "failed":
                        return self.send_json(200, {"job_id": job_id})
                job_id = str(uuid4())
                job = {"signature": signature, "status": "queued", "message": "Waiting for the singing model…"}
                audio = OUTPUT / (signature + ".mp3")
                if audio.exists() and audio.with_suffix(".json").exists():
                    ensure_normalized(audio)
                    mode = json.loads(audio.with_suffix(".json").read_text())["mode"]
                    job.update(status="ready", audio_path=f"/audio/{audio.name}", mode=mode, message="Your saved generation is ready.")
                JOBS[job_id] = job
                if job["status"] != "ready":
                    threading.Thread(target=generate, args=(job_id, body), daemon=True).start()
            return self.send_json(202, {"job_id": job_id})
        except (ValueError, json.JSONDecodeError) as exc:
            self.send_json(400, {"message": str(exc)})
        except Exception:
            self.send_json(500, {"message": "Could not prepare the saved recording. Please retry."})

    def log_message(self, fmt, *args):
        print(time.strftime("%H:%M:%S"), fmt % args, flush=True)


if __name__ == "__main__":
    print("Music bridge: http://127.0.0.1:8787 — keep this terminal open.", flush=True)
    ThreadingHTTPServer(("127.0.0.1", 8787), Handler).serve_forever()
