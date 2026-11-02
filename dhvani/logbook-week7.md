# Logbook — Week 7 (NEW ENTRY)

> Added one week after the latest entry. Previous entries:
> Week 4 — 20/7/26, Week 5 — 29/7/26, **Week 6 — 11/8/26** → **Week 7 — 18/8/26**.
> This entry is also written out in words in the report, the presentation deck, and the speech script. Copy the text below into your handwritten logbook.

---

## WEEK 7

**Date of meeting:** 18 / 08 / 26

**Points discussed:**
- Dataset work for Calculus is complete — syllabus specification (38 topics), song-type list, and the song↔topic mapping CSV are all done and verified.
- Project now moves to the **AI phase: lyrics generation** for the mapped topics.
- Discussed *which AI models* can write accurate, age-appropriate educational lyrics, and *which tools* turn those lyrics into real songs.

**Research done this week (my task):**
- **LLMs we can use for lyrics:** compared **GPT-4 / GPT-4o** (best rhyme & structure), **Meta Llama 3** (open-source, self-hostable, fine-tunable — follows up the earlier NLP/RAG/Llama task), and **Claude** (long-context, strong instruction-following). Conclusion: use an LLM with the exact syllabus formula in the prompt ("grounding") so the math stays correct.
- **MagicSchool AI:** an education platform with a **Song Generator** tool — type a topic + grade, it writes an age-appropriate song in seconds and can also auto-build a lesson plan/activity around it. Free tier. Good for first-draft lyrics + the "Activity" layer of our SAT framework.
- **Suno AI:** text-to-song tool — paste lyrics + a style prompt and it returns a full track **with vocals** in ~1 minute. This is what produces the actual playable song. (Udio is a backup.)
- **MusicGen (Meta, open-source):** text-to-*instrumental* model from the referenced Kaggle notebook; 32 kHz output, best at 30–74 s clips. For free, reproducible backing tracks.
- Reviewed the IEEE paper *"Harmonizing AI-Generated Music"* (MuseCoco + MusicGen hybrid) to understand how text aligns to music (CLAP score).

**Task assigned (next week):**
- Generate original draft lyrics for Unit 1 calculus topics using an LLM (grounded on the syllabus formulas), then produce 2–3 sample songs with Suno AI for the demo.
- Keep all lyrics **original** (reference songs are style-only, to avoid copyright).

---

### Copy-ready version (for the handwritten logbook)

```
WEEK 7
* Date of meeting: 18/08/26

* Points Discussed:
   - Calculus dataset done (syllabus + song list + mapping CSV).
   - Moving to AI phase: lyrics generation.
   - Discussed which LLMs & tools to use.

* Research done:
   - LLMs: GPT-4 / Llama 3 / Claude — ground on exact formula.
   - MagicSchool AI: Song Generator tool, writes a topic song +
     lesson plan in seconds (free).
   - Suno AI: paste lyrics + style -> full song with vocals (~1 min).
   - MusicGen (Kaggle): open-source text-to-instrumental, 32kHz.

* Task Assigned:
   - Generate original lyrics for Unit 1 topics with an LLM and
     make 2-3 sample songs in Suno for the demo.
```
