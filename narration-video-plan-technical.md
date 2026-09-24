# Video Production Plan: How This System Was Designed — Technical Considerations

## Technical design decisions and trade-offs — Patient Admission & Discharge Management Application

> **Sibling artifact, not a sequel.** [`narration-video-plan-product.md`](narration-video-plan-product.md) plans the
> *product* film (`presentations/product-demo/`), which sells what the system does to a health
> executive. This plan is for a second, independent film and deck (`presentations/technical-design/`)
> that shows **how the system was designed** to a technical hiring audience. The two share a pipeline,
> a house style and a seed dataset. They share no beats, and this one does **not** re-sell the
> product.

> **Status (24 Sep — TEACHING RE-CUT):** the film was re-cut for comprehension. An earlier
> shipped cut (18 Sep) was a tight screening reel — 24 scenes, ~8m49s, `verify.mjs` PASS. On the
> user's instruction the goal function was **inverted**: the audience-model is no longer the
> hostile, time-boxed queue-reviewer who wants a reason to stop, but a viewer who will watch as
> long as it takes to **fully understand every technical consideration, including the hidden
> design choices invisible in the UI**. Duration is explicitly *not* the constraint;
> understanding is. See D30–D32.
>
> What changed: (1) the five resolution beats are the SAME five and in the SAME order — the order
> already *was* the causal chain — but the narration now **voices the chain** instead of counting
> ("First decision… Second decision…"), which was the sole cause of the reported "does not flow";
> (2) a **container** is installed at the complication→resolution seam (end of `04-cannot-fake`)
> so the five read as one argument; (3) each beat is **taught to the floor**, including the hidden,
> UI-invisible mechanisms, in deepened narration; (4) beat 5 (`09-provable`) is explicitly marked
> **different in kind** — a meta-property about the other four, not "item five"; (5) two new
> terminal evidence takes were added at beat 3 (see §5.8, D31); (6) two narration accuracy
> corrections were made against the source code (D32).
>
> **Scene count now 26** (was 24): 10 slides, 15 captures/terminal takes, 1 stills coverage report;
> SCR still **2/2/5** (the two new takes are unroled captures, they do not touch the role budget).
> Deck: 11 slides as PNG + PDF + HTML. **No CTA** (D27). Plan decisions D17–D29 are pipeline/rig
> mechanics that survived the re-cut unchanged — read them before changing anything. Timing will be
> re-converged on regeneration (D29); expect the same two non-fatal `verify.mjs` warnings class
> (duration drift, intended trailing freezes).

> **Re-spine note (this revision).** An earlier cut shipped a film organised around **reversibility
> and judgment** (title *"Build the Foundations, Stub the Rest"*). It was fully re-spined to organise
> around **systems thinking / multi-actor correctness**, on the user's instruction to make the film
> read as a clear, plain-spoken walkthrough of *how the system was designed and the technical
> trade-offs behind it*, for technical + hiring managers. The thesis, title, beat structure and
> most narration changed; the pipeline mechanics (D17–D29), the runbook and the seam/evidence
> plumbing did not. Where a decision below still refers to the old spine's beats by their old IDs,
> the mapping is given in §3.6.

---

## 1. Executive Summary & Strategic Intent

**One conclusion the viewer must reach:** *this person designs for correctness as a property of how
the parts interact, and can name the trade-off behind each design choice — including where the
prototype stops and why.*

This is a systems-design **teaching** walkthrough. The five design decisions are the SAME five
correctness properties as the earlier screening cut, but the film is now built so a viewer *fully
understands* each mechanism — including the hidden implementation choices that never surface in the
UI — rather than being shown just enough to be convinced before moving on. Duration is not the
constraint; comprehension is. Depth is spent *inside* the five beats, never on a sixth argument.

The five properties are taught **as one chain, in the order the patient moves through them**, not as
a numbered list: **eligibility → resolution of disagreement → ownership of the decision →
contention over the decision → (meta) provability.** The first four are places in the patient's
path; the fifth is different in kind — a property *about* the other four. A **container** is
installed at the close of the Complication ("correctness has to hold in five specific places, in the
order the patient moves through them — here is the first") so the set coheres, and every resolution
opener names *its property and the link to the last beat* rather than an ordinal. This is the fix
for the one defect the screening cut had: the narration read as a list because the openers were
counters ("First decision…") carrying no semantic payload and no container to drop into.

The reason for that framing: at senior level, coding ability is assumed by the time anyone is
watching a video. The scarce, hard-to-fake signal in an admissions system is whether the author
reasons about **the whole system** — concurrency, disagreement between actors, separation of
authority, and what is safe to fake — rather than about individual screens. Each beat states a
property, teaches the mechanism in running code or a test, exposes the hidden choices behind it, and
prices the prototype-vs-production trade-off out loud.

It is also the defensive choice. The dominant risk with this audience is the reviewer who concludes
*"impressive-looking prototype, but it is demoware."* A film that keeps naming its own limits — H2
not Postgres, heuristic not Timefold, polling not STOMP, header auth not OIDC, tests not CI, and
three integrations that fail loudly because they were never built — pre-empts that read.

**Title (locked):** *How This System Was Designed — Technical Considerations.* Chosen to be plain
and literal rather than quotable: it tells a queue-scanning reviewer exactly what the next nine
minutes are, with no cleverness to decode. Narration voice is **plain, natural spoken English** —
contractions, short sentences, no words chosen to sound smart, no decorative aphorisms. Technical
terms are fine; the audience is technical.

---

## 2. Target Audience & Value Alignment

| | |
| --- | --- |
| **Primary audience** | Hiring managers and engineering managers assessing a **Principal / Staff+ software engineering** candidate. Assumed technical enough to know that "optimistic locking over pessimistic" or "heuristic over Timefold" is a real decision, not laziness. |
| **Viewing context** | A queue of candidates. The viewer is looking for a reason to *stop* watching, because stopping is how they get through the queue. Every second past the point of judgment can only lose. |
| **Secondary audience** | A panel interrogating the **deck** slide by slide in a system-design conversation. This is the audience the deck exists for, and the reason it is co-primary rather than a by-product: a film cannot be paused and cross-examined. |
| **Explicitly not the audience** | Non-technical recruiters, health executives, clinicians, the public. The product film serves those. |
| **Never named on screen** | Target role, company, seniority. Naming a role invites the viewer to grade against their rubric instead of engaging with the argument, and de-generalises the asset. |

**Standing assumptions (confirmed):**

1. The panel is technical. No 30-second lay on-ramp is budgeted.
2. The film stands alone — a viewer may not have seen the product film — but must not duplicate it.
3. Weaknesses go on screen. The whole argument depends on it.

---

## 3. Narrative Architecture

### 3.1 The organising principle

> **Correctness in this system is a property of how the parts interact.**
> It holds under concurrency and under disagreement between actors, and where a piece is not built
> yet, the boundary is drawn so it fails loudly instead of pretending. Each beat states one such
> property, shows the mechanism, and names the prototype-vs-production trade-off behind it.

This rule earns the spine because it is what makes the domain hard and what a senior reviewer is
actually assessing: not "does each screen work" but "does the system stay correct when six roles act
on one patient asynchronously." It is falsifiable on screen — every property is demonstrated in the
running app or pinned by a test.

**Rejected literal alternative:** an earlier draft phrased the thesis as *"correctness survives
partial failure"* (circuit breakers, retries, resilience). Rejected because those mechanisms are
`[DESIGNED]`/stubbed, not built — unprovable on screen, and claiming them would trigger the exact
demoware verdict the film exists to pre-empt. The shipped thesis claims only what the code proves.

### 3.2 The unit of the spine is a design decision, not a feature

A feature is a claim about the product; a design decision is a claim about how the author thinks
about the system. Feature-per-beat was rejected: structurally it demotes the trade-off to a
footnote, and arithmetically the 28 atomic issues in `.scratch/` at even 20 seconds each is 9m30s of
list with zero room for argument.

**Hard rule: no decision beat without a live artifact behind it** — a running-app capture or a real
test/terminal take. A decision with no on-screen artifact gets a clause, not a beat.

### 3.3 SCR, with Situation and Complication relocated

Archetype 7 (`archetype: scr`), declared so the compiler enforces it: `situation` 1–2,
`complication` 1–2, `resolution` 2–5 (`.kiro/skills/video-generator/scripts/lib/deck.mjs`,
validated at compile). The cap is wanted: it forces the question *"which five design decisions earn
the viewer's time,"* which is itself the editorial judgment the panel is assessing.

In the product film, Situation and Complication belong to the hospital. Reusing that here would
re-sell the product, which is forbidden. So both are **relocated to the engineering problem**:

- **Situation** — admission is a multi-actor asynchronous process; correctness is a whole-system
  property, not a per-screen one.
- **Complication** — the two easy ways to build a prototype like this both fail, *and* the
  safety-critical part cannot be faked.
- **Resolution** — the correctness principle, applied five times.

### 3.4 The unroled-slide loophole

`SCR_SKELETON` binds only entries carrying `role:`. Unroled slides are uncapped. Here they are
reserved for exactly two jobs: the **deck-only opener** that names the decision surface once, and the
**close** (debt ledger + method). There is **no CTA** (D27).

### 3.5 Runtime and shape

| | |
| --- | --- |
| Actual runtime | **8m49s** (target was ~9:00–10:30, ceiling 15:00) |
| Discipline | **the beat count, not the minutes.** Spare budget buys depth *inside* five beats, never a sixth argument |
| Aspect | **Landscape only.** A 9:16 reframe of a nine-minute design argument is format-mismatched (vertical norms are <90s); `--aspect=vertical` reframes, it never shortens |
| Media | **Deck and film co-primary.** `formats: [png, pdf, html]`; the deck opens on the decision-surface slide, the film opens on the Situation |
| Register | **Plain and direct throughout.** No warmth on the bookends, no aphorisms, nothing chosen to sound clever. This audience reads emotional framing or wordplay inside a technical argument as padding |

### 3.6 Old-spine → new-spine beat mapping

The re-spine renamed and reordered beats. Decisions in §6 that predate the re-spine reference the old
IDs; this maps them to what shipped.

| Old spine (reversibility) | New spine (systems correctness) — shipped |
| --- | --- |
| `00-answer` (reversibility rule) | **removed** — no answer-first slide; walkthrough opens on Situation |
| `00b-decision-ledger` | `00-decision-surface` (deck-only opener) |
| S1 "de-risk what you can't undo" | `01-problem` — admission is a multi-actor process; correctness is whole-system |
| S2 domain as a model | `02-domain-model` + `02b-bed-board-capture` |
| C1 both moves fail | `03-both-moves-fail` |
| C2 can't fake the safety part | `04-cannot-fake` |
| R1 "the seam is the deliverable" | **demoted** — the seam/profile boundary is now one card inside R5 (`09-provable`) |
| R2 heuristic over Timefold | **cut as a standalone beat** — Timefold/heuristic survives only as a ledger line and a stub-contract take |
| R3 safety not overridable | folded into R1 (`05-consensus`) + R2 (`06-discordance`) |
| R4 optimistic locking | **R4 concurrency** (`08-concurrency`) — sharpened to two races, one mechanism |
| R5 provable observability | **R5 provable + fails loud** (`09-provable`) |
| (none) | **R3 separation of authority** (`07-separation`) — net-new, ADR-0009 |
| `13-cta` / CTA | **removed from film and deck** (D27) |

---

## 4. Production Style & Visual Directives

- **Engine `html`** (bento templates), **`theme: light`** — matches the app UI so cuts to dashboards
  do not flash, and matches the product deck so the two read as one body of work.
- **No bullets.** Content is cards in one of eight layouts; card text is one or two sentences,
  enforced by the overflow invariant. Each card that states a trade-off carries a `detail:` line
  reading `Trade-off — …`.
- **Slide layouts assigned per beat and then held.** Rigidity is deliberate: once the viewer stops
  decoding layout, attention goes to content.
- **Voice:** Kokoro (local, Apache-2.0), **female** — the same narrator as the product film.
- **Subtitles:** burned ASS with explicit `PlayResX/PlayResY`, Arial, white on opaque `#0F172A`.
- **No generated footage. No b-roll. No music. No code on screen** (D6).

---

## 5. Master Beat Sheet (teaching re-cut)

> **Source of truth for narration:** the verbatim lines live **only** in
> `presentations/technical-design/storyline.yml`, one `narration:` per entry, compiled to
> `storyboard.json`. Lines below are **structure and intent** — when the two disagree, `storyline.yml`
> wins.

```
00:00       00:45       01:45       03:15       04:45       06:00       07:20      08:49
  │           │           │           │           │           │           │          │
  ▼           ▼           ▼           ▼           ▼           ▼           ▼          ▼
[S1 S2]     [C1 C2]     [R1]        [R2]        [R3]        [R4]        [R5]      [Ledger]
problem +   both moves  consensus   disagree    separation  concurrency provable  what's owed
the domain  fail; can't before      -> safety   of          two races,  + fails   + method +
as a model  fake safety eligibility by design    authority   one lock    loud      artifacts
```

### 5.0 Deck-only opener

**`00-decision-surface`** — `tiled`, `in: [deck]`, no `role:`. Names the whole decision surface
**once** — 11 `.wayfinder` tickets, 4 ADRs, 10 architectural dimensions, 5 specs — so the five beats
read as a *selection* from a documented set. Not a beat; excluded from the video.

### 5.1 S1 — `01-problem`: what kind of problem this is

- **Layout:** `stat-deepdive`. **Role:** `situation`.
- **Job:** frame the engineering problem before any domain content. **Five** roles act on one
  patient in turn — ED, specialists, bed management, ward nurse, housekeeping; no single one owns
  whether the outcome is correct; correctness comes from the handoffs.
- **Stat:** 5 roles act on one patient. (Was "6" in the screening cut; corrected to the five acting
  roles the patient actually passes through — D32.)

### 5.2 S2 — `02-domain-model` + `02b-bed-board-capture`: the domain as a data model

- **Layout:** `process-flow`. **Role:** `situation`.
- **Content:** 5 entities in a `Level → Ward → Bed` tree; the bed's **4 states** as a machine; **6**
  acting roles; **3** external systems the design depends on and cannot have (the prototype
  constraint). Taught as a model, not a patient journey — cheaper, keeps the register technical, and
  is itself principal-signal.
- **Evidence (`02b`):** `/bmu` board narrated as a state machine — "a bed can never quietly go
  missing" — not as capacity relief.

### 5.3 C1 — `03-both-moves-fail`: the two easy builds both fail

- **Layout:** `comparison`. **Role:** `complication`.
- **The two moves:** fake everything behind a nice UI (on camera a real rule and a fake one look the
  same → proves nothing); build all of it for real (never finishes — 3 integrations unobtainable, 1
  engineer, 0 production credentials). The load-bearing one-engineer/no-access constraint is stated
  here.

### 5.4 C2 — `04-cannot-fake`: the part you cannot fake

- **Layout:** `stat-deepdive`. **Role:** `complication`.
- **Argument:** clinical safety rules are not optional in a demo. Put one man in an empty six-bed
  cubicle and the other five beds can only take men; get it wrong and it is *wrong*, not unpolished,
  and a domain reviewer sees it instantly. This is what forces the safety rules to be real, and
  decides what got built to production standard vs stubbed.

### 5.5 Beat anatomy — applied to R1–R5

Each resolution beat: **name the property**, **name the alternative a senior engineer would expect**,
**show the mechanism in bound nouns** (running app or test), **state the prototype-vs-production
trade-off out loud** (as a spoken line *and* a `detail:` card). The trade-off card is manual
discipline — the compiler checks cost-words on `complication` only.

### 5.6 R1 — `05-consensus`: consensus before eligibility

- **Layout:** `process-flow`. **Property:** a patient who needs specialist input is not put in the
  bed queue until every consult resolves.
- **Mechanism:** the request is held in `ASSESSMENT_PENDING`, hidden from bed management; it becomes
  eligible only when every broadcast reaches `COMPLETED`; chained consults extend the wait. ADR-0007/
  0008.
- **Trade-off:** slower entry, in exchange for never placing a patient the assessment might still
  change.

| Take | Type | Proves |
| --- | --- | --- |
| `05a` `/ed` — select a patient, check the consult-gated box → "Admission is held in ASSESSMENT_PENDING" | Playwright | The gate, in the UI, on the way in |
| `05b` `run=05b-consensus-gate` — `ClinicianServiceTest` partial-completion + all-completed | terminal | Holds even when consults arrive one at a time; partial agreement is not agreement |

### 5.7 R2 — `06-discordance`: disagreement resolves to safety by construction

- **Layout:** `comparison`. **Property:** when specialists disagree, the system takes the safe side
  with no human arbitration.
- **Mechanism:** highest acuity anyone assigned wins; every monitoring requirement anyone flagged is
  unioned; the queue sorts on the effective tier, not the ED's original number. ADR-0008.
- **Trade-off:** sometimes over-provisions, in exchange for a disagreement never resolving toward the
  less safe option.

| Take | Type | Proves |
| --- | --- | --- |
| `06a` `run=06a-discordance` — `ClinicianService.setEffectiveAcuityTier(highestAcuity)` + the elevation test + `BmuServiceTest` queue-sort | terminal | The rule as code, and the queue sorting on the effective tier |

### 5.8 R3 — `07-separation`: separation of decision authority (net-new, ADR-0009)

- **Layout:** `tiled`. **Property:** who decides the clinical picture and who decides the bed are
  different people, on purpose.
- **Mechanism:** clinicians decide acuity/telemetry/diversion, not the ward; after consensus the BMU
  coordinator sets the admitting cluster and allocates; the solver scores against the coordinator's
  authoritative choice, not the ED's suspected service.
- **Trade-off:** one more handoff, in exchange for each decision being owned by the person who should
  own it and pulling doctors off clinical work less.

| Take | Type | Proves |
| --- | --- | --- |
| `07a` `/bmu` — the "(Admitting)" badge (authoritative) vs the ED's plain service badge | Playwright | Two decisions, two owners, and the system knows which is authoritative |
| `07b` `run=07b-authority-enforced` — `BmuServiceTest` attachDelayTag success (BMU) + forbidden (non-BMU) | terminal | **(B, new)** Authority is enforced in the service method: an ED attending's operational write is refused with `AccessDeniedException`, not just hidden in the UI |
| `07c` `run=07c-two-tier-safety` — `BmuServiceTest` gender/negative-pressure (422) + ward-class/telemetry (400) | terminal | **(B, new)** The two-tier constraint hierarchy: absolute invariants reject with 422 and no override path; operational rules reject with 400 unless a structured reason code is supplied. The payoff of the C2 "cannot fake safety" complication, proven |

The two `07b`/`07c` takes are the depth policy in action (D30/D31): the two hidden mechanisms a
Staff+ reviewer is most likely to *doubt* are converted from asserted narration into on-screen test
evidence, both backed by tests that already existed. Everything else in the beat is taught as
narration depth over the existing `07a` visual.

### 5.9 R4 — `08-concurrency`: two races, one mechanism

- **Layout:** `comparison`. **Property:** two people racing for the same case or the same bed cannot
  both win.
- **Mechanism:** `@Version` optimistic locking on 3 entities (`AdmissionRequest`, `AssessmentBroadcast`,
  `Bed` — verified in source); the losing write gets an HTTP 409 RFC 7807 `ProblemDetail`. Optimistic
  **not** pessimistic, because losing the race is cheap and self-correcting — the loser sees the case
  is taken and picks the next one; a database lock would prevent a conflict that costs nothing and
  adds deadlock risk and pool pressure. **Correction (D32):** the earlier cut said "every transition
  goes through one service method, a single write path." That is an overclaim — there is no single
  method. The accurate mechanism is that **each transition is its own `@Transactional` method with a
  state-guard precondition** (it checks the current bed/broadcast state and throws
  `IllegalStateException` if it is wrong), and the `@Version` column resolves the case where two such
  guarded transactions read the same starting state and both try to advance it. The guard defines the
  legal moves; the version number decides who wins the race for one.
- **Trade-off:** the loser retries, which is free.

| Take | Type | Proves |
| --- | --- | --- |
| `08a` `/ward` vacate → mustard yellow (30-min SLA) | Playwright | The state machine as gated mutation — each move checks the state it is leaving before it writes |
| `08a2` `/ward` housekeeping sign-off → white | Playwright | Each transition is a guarded step in one transaction; the state guard plus the `@Version` column is what makes the concurrent write safe (corrected from "single write path" — D32) |
| `08b` `run=08b-conflict-409` — `ClinicianControllerTest` 409 + `GlobalExceptionHandlerTest` | terminal | The race proven where the UI cannot honestly stage two clients at once |

### 5.10 R5 — `09-provable`: provable where built, loud where not

- **Layout:** `process-flow`. **Property:** every metric is checkable two ways, and the unbuilt parts
  refuse to run rather than lie. Reversibility/the profile boundary lives here as one supporting card.
- **Mechanism:** dual-pathway observability (DB + audit log, a test fails if they disagree); one
  Spring profile picks mocks or production adapters at startup; the 3 unbuilt adapters throw
  `UnsupportedOperationException` on invocation.
- **Trade-off:** only the prototype path is tested — but an unbuilt piece that fails loudly is safer
  than one that quietly pretends.

| Take | Type | Proves |
| --- | --- | --- |
| `09a` `/analytics` — 20 metrics, benchmarks, target-met | Playwright | Pathway A (DB), from the workflows just shown |
| `09b` `run=09b-parity` — `PathBDualPathwayReconciliationIntegrationTest` | terminal | The two pathways are asserted equal, not eyeballed |
| `09c` `run=09c-stub-contract` — `GatewaysAndSolversTest` | terminal | The 3 stubs throw on invocation |
| `09d` JaCoCo report (`:4173`, stills) | Playwright | 94% instr / 97% lines / 69% branches — the branch number spoken |
| `09e` `run=09e-gate-red` — frontend thresholds vs the red 74.84% | terminal | The most honest beat: a gate no pipeline runs, currently red |

### 5.11 Close — `10-ledger` + `10a-artifacts-terminal` (unroled)

- **`10-ledger`** (`tiled`): what is stubbed and the boundary it is stubbed behind — in-memory data,
  polling, heuristic, header personas, no CI — each paired with the interface/profile already in
  place. **Method disclosure lives here:** AI wrote much of the code; the trade-offs, the approach and
  the final shape were the author's.
- **`10a-artifacts-terminal`** (`run=10a-artifacts`): 11 tickets / 28 issues / 5 specs / 4 ADRs,
  counted on screen, proving specs preceded code. **The film ends here** — no CTA (D27).

---

## 6. Decision Log

Each entry records what was chosen, what was rejected, and the reasoning. **D1–D16 describe the
original spine and are retained for provenance; the re-spine (§3.1, §3.6) supersedes the ones marked.
D17–D29 are pipeline/mechanics facts that survived the re-spine unchanged unless noted.**

**D1 — The conclusion is systems-design reasoning, not delivery, domain or rigour.** *(re-spined —
was "judgment under constraint")* The film now argues that correctness is a whole-system property and
prices each design trade-off; rigour remains the proof mechanism, not the claim.

**D2 — Timefold is named as available and declined, not as unknown.** Retained. In the re-spine it
survives only as a ledger line + the stub-contract take, not a standalone beat.

**D3 — The unit of the spine is a design decision; features are the setting.** Retained.

**D4 — Organising principle.** *(re-spined)* Was "investment allocated by reversibility"; now
"correctness is a property of how the parts interact" (§3.1). Reversibility demoted to one card in R5.

**D5 — `archetype: scr`, enforced; decisions grouped into 5 resolution beats.** Retained; the five
beats changed identity (§3.6).

**D6 — Evidence tiers 1–4 accepted; code on screen refused.** Retained. Behaviour under two
configurations is stronger than source under one.

**D7 — E2E and Allure are out of scope.** Retained.

**D8 — No number on screen unless the same shot produces it.** Retained. Kept: the `<50ms` figure is
still dropped (unmeasured); coverage numbers spoken are the measured 94/97/69 and 74.84.

**D9 — AI-augmented development placed by causal role: constraint in the complication, method in the
close, never a reason the design is good.** Retained; method disclosure now lives in `10-ledger`.

**D10 — Persona switching is auditability-by-design, seeded invisibly per capture.** Retained. Each
capture seeds its role via `setupScript` (`localStorage['admissions_role_persona']`) before SPA boot;
the switcher is no longer operated on screen as its own beat (old R1 was demoted).

**D11 — Where the UI cannot prove something honestly, the test is the evidence.** Retained; now
applied to R4's 409 (`08b`).

**D12 — `scripts/` never appears on screen.** Retained; pathway B proven by the reconciliation test
(`09b`).

**D13 — Deliverables staged: plan → `storyline.yml` → compile → slide PNGs → gate → spend.** Retained.

**D14 — Title and CTA.** *(re-spined)* Title is now *"How This System Was Designed — Technical
Considerations"* (plain, literal). **The CTA is removed entirely from film and deck** (D27), not kept
as a deck slide.

**D15 — Plain register throughout; the domain taught as a data model.** *(sharpened)* The re-spine
tightened this to **plain natural spoken English, no aphorisms, nothing chosen to sound clever** —
the user's explicit instruction.

**D16 — Runtime ~9–10:30 target, ceiling 15:00; landscape only; deck co-primary.** Retained; actual
shipped runtime **8m49s**.

**D17 — The architecture/seam diagram enters as a captured web page on `:4173`, not a slide or
footage.** Retained. (In the re-spine the seam material is one R5 card; the diagram page still exists
under `:4173` for the deck.)

**D18 — Terminal takes originally planned as VHS `footage`.** Superseded by D20.

**D19 — R3's absolute-tier evidence is a test, not a screenshot, because the UI branch is dead
code.** Retained as a repo side task; the re-spine no longer leans on that shot.

**D20 — Terminal evidence is recorded output rendered by a page we own, because VHS does not work.**
Retained and load-bearing. `tools/record-runs.mjs` executes each command for real and freezes
`stdout`/`stderr`/exit code/duration into `pages/runs/<id>.json` (committed); `pages/term.html?run=<id>`
renders it; the scene is an ordinary `capture` against `:4173`. A failing command stays failing
(`09e` is red because it *is* red). **Re-spine update:** the run set changed to
`05b-consensus-gate`, `06a-discordance`, `08b-conflict-409`, `09b-parity`, `09c-stub-contract`,
`09e-gate-red`, `10a-artifacts`; the old-spine runs (`05b-profile-fence`, `05c-stub-contract`,
`06c-solver-pinned`, `07a-absolute-tier`, `09b-audit-log`, `09c-parity`) were deleted. **Teaching
re-cut (D31):** two runs added — `07b-authority-enforced` and `07c-two-tier-safety` — bringing the
frozen run set to nine. All asserted strings verified present in the frozen output before any spend.

**D21 — Captures run against the Vite dev server, not the packaged jar.** Retained.
`baseUrl: http://127.0.0.1:3000`; the jar 404s on deep links (no SPA fallback). Vite 8 needs
`--host 127.0.0.1` because it binds IPv6 (`::1`) by default — confirmed again this run: the flagless
server was unreachable on `127.0.0.1`, and the flag fixed it. Still a real product defect (repo side
task 1).

**D22 — R1's fence take must pass `--spring.profiles.active=production` explicitly, and the code is
403.** Retained as a fact about the app; in the re-spine the fence is not its own beat, but the
profile-boundary claim in R5 depends on the same truth. The prototype profile being the config
default is still a ledger-worthy debt.

**D23 — The coverage claim changed after measuring, and for the better.** Retained and shipped.
Backend: 94% instr / 97% lines / **69% branches** (the branch number is spoken). Frontend: declares
four 90% thresholds and currently **fails** — branches **74.84%** — while the committed report is
stale at 100%. `09e` shows the thresholds beside the red failure; kept as the strongest honesty beat.

**D24 — `preflight.mjs` exists, because the most expensive failure mode was discoverable for free.**
Retained and used. **Re-spine result:** preflight caught the `05a` `/ed` capture asserting
`ASSESSMENT_PENDING` on load, which is click-gated; the capture was rewritten to select a patient →
check the consult-gated box → `waitForText ASSESSMENT_PENDING`, landing assertion changed to
"Emergency Department (ED) Clinical Intake". Then **14/14 capture scenes passed preflight**.

**D25 — Beats re-grounded against the live solver.** Retained as historical; the re-spined beats use
their own verified data (partial-completion consensus test, effective-acuity elevation test, the
"(Admitting)" badge on a seeded consult-gated request).

**D26 — Static pages starve Chrome's screencast, so every static page needs one perpetual
animation.** Retained and load-bearing. `term.html`/`seam.html` carry an imperceptible `#ticker`
animation to restore compositor frame rate; the JaCoCo report is third-party and cannot be animated,
so `09d` uses `captureMode: stills` (exactly `sceneMs` from one screenshot).

**D27 — The CTA is removed from the film and the deck** *(re-spine change — was "kept as a deck
slide")*. User instruction: "no need for a final CTA, remove that from the plan." The `11-cta` entry
was deleted; the film ends on `10a-artifacts-terminal`, and the deck ends on the ledger.

**D28 — `VIDEO_GEN_APP_VERSION` and the fresh-JVM rule.** Retained and re-confirmed this run: captures
mutate H2 state (`08a`/`08a2` vacate and clean beds), so a re-capture pass against a mutated tree
aborts on a missing button. `:8080` is restarted before each full capture pass.

**D29 — Timing converged by measurement.** Retained and re-confirmed, with a sharper rule learned
this run: **the correct stage order for a clean standalone-`verify` is capture screencasts →
synthesize → capture again (rebuilds slides + stills at the now-known `sceneMs`) → synthesize →
compose → verify.** Running the stages in the wrong order (slides captured before synthesize set
`sceneMs`) produced drift on every slide, and the coverage-stills scene built at the 5000 ms default
until a second capture pass. Setting each terminal/capture `delayMs` to ≈ its natural rate-0 audio so
scenes land `audio-dictates` (drift ~0) is more robust than relying on speech-fit stretching. Two
captures whose narration was 30–37% longer than the visual (`07-separation`/`07a`, `08a2`) were
lengthened so the narration fits at a natural rate rather than being sped up to the cap.

**D30 — The film was re-cut for comprehension, not screening.** *(24 Sep — supersedes the audience
model in §2's "viewing context" for this cut.)* On the user's instruction the goal function
inverted: understanding is the objective, duration is not a constraint. Structure decision recorded
in the interview: **(A)** deepen the same five correctness beats to the floor — rejected **(B)** a
comprehensive walk of the whole decision surface (would break SCR 2/2/5 and risk the feature-tour
the archetype forbids). No re-sequence (Layer 3) because the shipped order already *is* the causal
chain; no beat change (Layer 4). The reported "does not flow" was diagnosed to a single cause: the
five resolution openers were content-free ordinals ("First decision…") with no *container* for the
set to drop into. Fix: install the container at the close of `04-cannot-fake`, replace every ordinal
with a property-named, causally-linked opener (eligibility → disagreement → ownership → contention →
provability), and mark `09-provable` as **different in kind** (a meta-property about the other four).

**D31 — Two new (B) evidence takes, backed by existing tests.** The depth policy is (A) narration
depth over existing visuals by default, (B) new terminal evidence only for a hidden mechanism a
skeptic would doubt *and* that a real command can honestly pin. Two qualified, both at beat 3:
`07b-authority-enforced` (role-gated write: `attachDelayTag` throws `AccessDeniedException` for a
non-BMU caller — `BmuServiceTest#testAttachDelayTag_Success+testAttachDelayTag_Forbidden_WhenNonBmu`)
and `07c-two-tier-safety` (422 absolute invariants vs 400 overridable rules —
`BmuServiceTest#testAllocateBed_SafetyInvariant_GenderCohorting+…_NegativePressureIsolation+…_OperationalConstraint_WardClassMismatchWithoutReason+…_TelemetryRequiredWithoutReason`).
No new tests were written; both takes use tests that already existed. Recorded via `record-runs.mjs`,
frozen to `pages/runs/07b-authority-enforced.json` and `pages/runs/07c-two-tier-safety.json`
(committed). Everything else hidden is taught as (A) narration.

**D32 — Two narration accuracy corrections against the source.** Reading `ClinicianService`,
`BmuService`, and the entities before rewriting surfaced two claims the screening cut got loose:
(1) `01-problem` said **6 roles**; the patient passes through **5** acting roles (ED, specialists,
bed management, ward nurse, housekeeping) — corrected to 5 in stat, cards and narration. (The domain
model at `02-domain-model` legitimately still says "6 acting roles" — that counts every role that
*can* act on the tree, including the patient/tracker view, which is a different claim from "roles a
patient passes through.") (2) `08a2` said "every transition goes through **one service method**, a
single write path." There is no single method — each transition is its own `@Transactional` method
with a **state-guard precondition**, and `@Version` resolves two guarded transactions racing from the
same state. Narration corrected to that mechanism. `@Version` confirmed on exactly 3 entities.

### 7.1 Rig

| | |
| --- | --- |
| Skill copy | **`.kiro/skills/presentation-producer`** — the current skill. The `video-generator` copies under `.kiro/` and `.agents/` are the superseded predecessor and must not be used |
| Work root / presentation | `presentations/` (holds the one shared `.runtime/`) · `presentations/technical-design/` — run it with `--root=presentations --presentation=technical-design` |
| Source / lockfile | `storyline.yml` authored (filename load-bearing); `storyboard.json` generated, committed, never hand-edited |
| Presentation block | `mode: video`, `engine: html`, `theme: light`, `formats: [png, pdf, html]` |
| Video block | `aspect: landscape`, `outputs: [landscape]` |
| Determinism | `baseUrl: http://127.0.0.1:3000` (D21); `fixedTime: '2026-01-15T09:00:00.000Z'` |
| Voice | Kokoro, `gender: female` |
| Committed | `storyline.yml`, `storyboard.json`, `scenes/`, `pages/` (incl. `pages/runs/*.json`), `tools/`; `assets/` gitignored |

**Multi-origin capture:** an **absolute** `url` bypasses `baseUrl` (`capture.mjs`), so the coverage
reports, the seam diagram and the terminal takes are all captured from the `:4173` origin, leaving the
application untouched.

### 7.2 Runbook — every prerequisite, in order (all executed and verified this run)

1. VHS **not required** (D20); terminal evidence needs nothing beyond Node.
2. `cd backend && ./mvnw test jacoco:report` — **without `clean`** (which deletes the report).
   Produces `backend/target/site/jacoco/index.html` (measured: 214 tests, 94% instr, 69% branches).
3. `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=prototype` (or the jar) on
   `:8080`. **This JVM must survive each capture pass** (H2 is `create-drop`); restart it before each
   full pass (D28).
4. `cd frontend && npm run test:coverage` — generates `frontend/coverage/` (exits non-zero: the
   branch gate is red at 74.84%, which is the point).
5. `cd frontend && npm run dev -- --host 127.0.0.1` — captures target `:3000`; the IPv4 host flag is
   required (D21).
6. `bash presentations/technical-design/tools/serve-evidence.sh` — symlinks the seam page, the terminal
   renderer, the run JSONs and both coverage reports under `:4173` and verifies all URLs return 200.
   (Healthcheck references `runs/05b-consensus-gate.json`.)
7. `node presentations/technical-design/tools/record-runs.mjs` — freeze the terminal evidence (9 runs,
   incl. the two teaching-cut takes `07b-authority-enforced`, `07c-two-tier-safety` — D31).
8. `node presentations/technical-design/tools/preflight.mjs` — must report **16/16 capture scenes ready**
   before any spend.
9. `compile` → `deck` → `synthesize` → `capture` → `synthesize` → `capture` → `compose` → `verify`,
   or run `generate.mjs --yes` which orchestrates the converged order. Restart `:8080` before each
   full capture pass.

### 7.3 Two ordering constraints that are correctness issues, not preferences

- **A capture pass mutates H2 state, so each full pass needs a fresh `:8080`.** `08a`/`08a2` vacate
  and clean beds; a second pass against the mutated tree aborts on a missing button (D28).
- **A dashboard of zeros satisfies every assertion we would naturally write.** `09a`'s captures
  assert on `Target Met` / a non-zero rendered value, and the scene order exercises the workflows
  first, so the dashboard reflects real activity.

### 7.4 Stage order and regeneration

`compile.mjs` → `deck.mjs` (slides, free, no TTS) → gate → **capture screencasts** → `synthesize.mjs`
(fits speech to the real captured visuals) → **capture again** (rebuilds slides + stills at the final
`sceneMs`) → `synthesize.mjs` → `compose.mjs` → `verify.mjs`. `generate.mjs` performs this converged
order automatically; running stages by hand requires it explicitly (D29).

- Set each capture's trailing `wait delayMs` to ≈ its natural rate-0 audio so the scene lands
  `audio-dictates` with ~0 drift; this is more robust than leaning on ±7% speech-fit.
- Terminal takes are **inelastic** — a test suite takes as long as it takes — so narration is fitted
  to them, never the reverse.
- Assertions are mandatory on every capture: `assert.visible` for required content, `assert.notVisible`
  for `['Something went wrong', 'No static resource', 'Sign in']` (or the terminal equivalents).

---

## 8. Open Items

**Closed by measurement this run:**

| Was open | Outcome |
| --- | --- |
| Actual coverage numbers | 94% instr / 97% lines / 69% branches backend; frontend gate **red** at 74.84% (D23) |
| `05a` ED capture asserted click-gated text on load | Rewritten to click the patient + consult box, `waitForText ASSESSMENT_PENDING`; 14/14 preflight (D24) |
| New terminal-run evidence | 7 runs recorded; all 18 asserted strings verified present (D20) |
| Timing convergence | Standalone `verify.mjs` PASS; converged via the capture→synthesize→capture→synthesize order (D29) |

**Remaining, none of which block the gate:**

- Two non-fatal `verify.mjs` warnings: total-duration drift ~124 ms over the soft threshold, and 6
  held-frame freezes >6s (intended trailing holds on terminal/coverage takes).
- On `05a`, `ASSESSMENT_PENDING` renders low in the ED form and can sit below the frame in the final
  still; the `waitForText` gate confirms it rendered during capture. A scroll-into-view step + a
  single-scene re-capture would frame it more prominently if desired.

**Repo side tasks, separate from the film:**

1. **No SPA fallback.** The packaged jar 404s on `/ed`, `/bmu`, `/analytics`; a reviewer refreshing
   the live demo on a deep link gets a 404. One `WebMvcConfigurer` forward fixes it (D21).
2. **`bmu.tsx` dead code.** `safetyViolationReason` / `isOperationalOverride` are never populated by
   the backend (D19).
3. **The prototype profile is the configuration default** (`application.yml`). A production deploy
   that forgets the profile boots the prototype (D22).
4. **The frontend coverage gate is red** (branches 74.84% vs 90%) and the committed report is stale
   at 100% (D23).
5. **README reconciliation:** "18 operational KPI metrics" → 20 metrics + 2 breakdown maps.
6. **`bootstrap.mjs` pins `@remotion/cli@4.0.360`**, which no longer resolves; use
   `--with-remotion=false`.

---

## 9. Non-Goals

- Re-selling the product, re-explaining the six pain points, or reusing the product film's beats.
- Any patient-journey narrative, emotional framing, or aphoristic/"quotable" phrasing.
- Code on screen, IDE footage, generated b-roll, background music.
- `scripts/`, `/h2-console`, E2E/Allure.
- A final CTA (removed from film and deck — D27).
- Vertical (9:16) output, PPTX emission, any claim about future pricing or performance no shot produces.
- Naming a target role, company or seniority.

---

## 10. Definition of Done (met)

1. `storyline.yml` compiles clean — SCR 2/2/5, no overflow, no unknown icons, every capture carries
   `assert.visible` **and** `assert.notVisible`. ✔
2. Every resolution beat names a **trade-off** in narration *and* carries it as a `detail:` card. ✔
3. Every number spoken is produced by the shot it is spoken over. ✔
4. Slide PNGs reviewed at 1920×1080 before TTS/capture spend. ✔
5. All runbook prerequisites up; both §7.3 ordering constraints observed. ✔
6. Standalone `verify.mjs` reports **PASS** (two non-fatal warnings, §8). ✔
7. Deck exports as PDF + HTML and reads standalone without narration. ✔
8. Film ends on the countable-artifacts take; **no CTA**. ✔
