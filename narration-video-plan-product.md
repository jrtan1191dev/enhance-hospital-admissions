# Video Production Plan: Enhancing Hospital Admissions
## Patient Admission & Discharge Management Application

> **Alignment note (verified against the running codebase):** Every clinical case, route, KPI, and
> feature named in this plan has been traced to the actual prototype — the seeded patients in
> `DataInitializer.java`, the routes under `frontend/src/routes/`, and the metrics computed by
> `KpiMetricsService`. All on-screen names, tokens, and numbers in this plan are the *real* ones the
> app renders, so the voiceover never contradicts the footage. KPI values quoted here were captured
> live from `GET /api/v1/analytics/kpis/summary` under the `prototype` profile.

---

## 1. Executive Summary & Strategic Intent

This video is an authoritative, in-depth **product walkthrough film** (~14–16+ minutes — runtime
ceiling lifted per Decision #20-e; full feature coverage outranks completion rate) whose purpose
is to fully explain the concept, features, and usefulness of the **Patient Admission & Discharge
Management Application** — how it intends to solve the systemic hospital bed-crunch pain points.
Rather than abstract slide-ware, the film walks through the **live, running prototype**, proving how
the platform tackles inter-departmental phone tag, recovers "ghost" bed capacity without new physical
construction, and restores transparency to patients and families through **intelligent clinical and
operational orchestration**.

---

## 2. Target Audience & Stakeholder Value Alignment

The video speaks to a **Dual Constituency** — uniting operational decision-makers with clinical
frontline leadership:

| Stakeholder Persona | Core Motivations & Pain Points | What This Video Proves |
| :--- | :--- | :--- |
| **Hospital Operations Leadership**<br>*(COO, CMIO, Director of BMU & Nursing)* | • Chronic ED boarding & ambulance diverts<br>• High inpatient occupancy & gridlocks<br>• Nurse burnout & inter-departmental friction<br>• Lack of real-time operational capacity data | • **Recovers hidden bed capacity** via automated cohort packing & dynamic cohort-swaps without new real estate.<br>• **Reduces ED boarding** via direct digital handoffs.<br>• **Full operational instrumentation** — a computed KPI dashboard with dual-pathway (SQL + structured-log) extraction. |
| **Clinical Frontline Leadership**<br>*(ED Attending Chief, Specialist Chiefs, Nurse Managers)* | • Endless phone calls to BMU & specialists<br>• Contentious specialist consult handoffs<br>• Anxious families verbally abusing triage nurses<br>• Afternoon discharge medication exit blocks | • **"Zero phone calls" workflow** from diagnostic synthesis to bed placement.<br>• **Clinical autonomy preserved** — 1-click reviews with safety-first peer discordance escalation.<br>• **Nurses shielded from queue friction** via a public milestone tracker with empathetic delay tags. |

---

## 3. Narrative Architecture: The Tri-Hybrid Arc

The film synthesizes three storytelling frameworks:

```
┌──────────────────────────┬─────────────────────────────┬───────────────────────────────┐
│ A. Patient Tracer        │ B. Before vs. After         │ C. Pain Point Matrix          │
│    Journeys              │    Contrast                 │    Systematic Proof           │
├──────────────────────────┼─────────────────────────────┼───────────────────────────────┤
│ Follows the REAL seeded  │ Visceral comparison between │ Directly proves solutions for │
│ cases through the        │ the broken manual status    │ all 6 documented systemic     │
│ operational pipeline,    │ quo (clearly-labelled       │ root causes from              │
│ step-by-step.            │ illustration) and the       │ pain-points.md.               │
│                          │ orchestrated digital reality│                               │
│                          │ (live UI).                  │                               │
└──────────────────────────┴─────────────────────────────┴───────────────────────────────┘
```

### The Real Seeded Cases (used on screen)

The prototype deliberately distributes its capabilities across purpose-built seeded patients — no
single hero patient carries the whole arc, so the film follows each capability on the patient the
system built to demonstrate it. **These are the exact names/tokens shown on screen:**

| Capability demonstrated | Seeded patient (on-screen) | State |
| :--- | :--- | :--- |
| ED smart-assessment pre-fill (acute cardiac) | **Chua Wee Kiat** (64M, ACS, troponin 180) — `Q-P120` | On ED board, `ASSESSMENT_PENDING` |
| ED smart-assessment pre-fill (stable pathway) | **Nurul Huda** (47F, pneumonia) — `Q-P121` | On ED board |
| Specialist discordance → safety-first escalation + telemetry union + chained consult | **Mr Fernandez** (55M, Cardiology) — `Q-P105` | Discordant: primary Tier 3 → effective Tier 2, chained Surgery consult open |
| Consult-gated consensus cleared → BMU assignment | **Mr Goh Beng Kiat** (44M, appendicitis) — `Q-P107` | All broadcasts complete |
| MIC@Home (Hospital-at-Home) diversion | **Mrs Tan Boon Hwa** (63F) — `Q-P106` | `DIVERTED_HAH`, virtual bed `MIC-V042` |
| Sister Community Hospital (OCH) referral | **Mr Ahmad Ibrahim** (71M, post-stroke subacute) — `Q-P116` | 30-min bilateral SLA to OCH |
| Direct digital BMU bed request | **Tan Ah Meng** (68M, Cardiology, telemetry, fall-risk 65) — `Q-P101` | `BED_REQUESTED` |
| Batch holding-ward surge cluster | Tan Ah Meng + Goh Beng Kiat + **Teo Hock Seng** (`Q-P112`) → Ward 8B | 3-patient male B2 cluster |
| Dynamic cohort-swap | **Mr David Koh** (`Q-P115`) blocking Ward 9B → swap to 8A-03 | `BED_ALLOCATED` (Green), still in ED |
| Empathetic delay tag (UV isolation) | **Mdm Wong Siew Kuan** — `Q-P118` | Prolonged-wait, specialized-cleaning delay |
| Discharge runway (D-2 → ready-to-vacate) | Inpatients inp1–inp5 on `/ward` | Full 5-stage progression, incl. bedside meds delivered |

---

## 4. Production Style & Visual Directives

* **Visual Format:** **Live, high-fidelity React UI screen-capture walkthrough + voiceover.** The
  heart of the film is the running prototype itself, captured at each route with the real seeded
  patients above. *(Revised from an on-camera founder-led cinematic format — see Decision #5.)*
* **Supporting slide layer:** The existing `video-generation/` Marp pipeline supplies the title card,
  the walled-off **Before vs. After** contrast panels, and inter-act transitions — not the core
  walkthrough. **This slide layer is being upgraded into a polished, SCR-structured pitch deck that
  doubles as the film's narrative spine — see §7.** The deck's slides replace the current wordy Marp
  cards and are slotted into the storyboard as per-pain-point *setup* cards ("here is the issue, here
  is the resolution") that immediately hand off to the live Playwright walkthrough that demonstrates
  the resolution.
* **Routes captured:** `/ed`, `/specialist`, `/bmu`, `/bmu-config`, `/patient`, `/ward`, `/analytics`.
  (Persona switching via the prototype `X-User-Role` header: `ED_ATTENDING`, `SPECIALIST`,
  `BMU_COORDINATOR`, `PATIENT`, `WARD_NURSE`, `HOUSEKEEPING`.)
* **Camera work:** Software zooms into UI micro-interactions (dropdown chip overrides, state-machine
  badge transitions, discordance alerts). No live-action camera or animated motion-graphics required.
* **Data overlays — two clearly-distinct badge classes (see Decision #3):**
  * **LIVE metric badges** — values the app genuinely computes and that are verifiable on
    `/analytics`. Styled as "read from the system."
  * **PROJECTED / DESIGN-TARGET badges** — improvement *deltas* the app cannot compute (no baseline
    exists). Styled distinctly (e.g. a "Design target" ribbon) and spoken as *the outcome the system
    is designed to drive*, never as measured results.
* **"Before" segments:** Always an explicitly-labelled illustration ("Today, without the system"),
  visually walled off (desaturated / sketch treatment), never sharing a frame with real UI, with
  figures framed as representative. Sourced only where genuine (lived experience + CNA Talking Point
  for the *qualitative* crisis — not for precise minutes/pages).
* **Sound Design & Music:** Clean, modern cinematic tech pulse for walkthroughs, softening during
  narrative moments.

---

## 5. Master Beat Sheet & Scene-by-Scene Script (measured ~11m30s, runtime ceiling raised to 12
minutes per Decision #21 — superseding the earlier 14–16 minute estimate, which predated the
narration clarity rewrite)

> **Source of truth for narration:** the verbatim spoken lines now live **only** in
> `presentations/product-demo/storyline.yml`, one `narration:` field per entry, and compile to
> `presentations/product-demo/storyboard.json`. The voiceover text quoted in this section is a **mirror for
> review**, not the source — when the two disagree, `storyline.yml` wins. This section's value is the
> beat structure, routes, selectors and seeded-case grounding; do not edit narration here and expect
> it to reach the film. See Decisions #21–#33 in §6 for the rules every line is written against.

```
00:00      01:15      03:30      05:00      07:30      09:00      10:30      12:00
  │          │          │          │          │          │          │          │
  ▼          ▼          ▼          ▼          ▼          ▼          ▼          ▼
[Act 1]   [Beat 1]   [Beat 2]   [Beat 3]   [Beat 4]   [Beat 5]   [Act 3]    [Close]
Origin &  PP 1 & 2   PP 3       PP 4       PP 5       PP 6       Observ.    Call to
Status Q  ED+Consult Diversion  Ghost Cap  Patient    Ward Exit  Control    Action /
          Discord.   OCH/MIC    Cohort-Sw  Tracker    Meds       Tower      Live Demo
```

*Chapter markers aligned to these Acts/Beats will be published on the video timeline (additive; does
not change the linear film — Decision #7).*

---

### Act 1: The Origin & The Broken Status Quo (0:00 – 1:15)

* **Visual:** Four bento slides — `01-situation` (stat-deepdive, 8–14h), `01b-handoffs`
  (process-flow, five desks), `02-complication` (comparison), `02b-frozen` (stat-deepdive, 5 beds).
  Act 1 is now **four** SCR beats, not one title card: situation and complication are each at the
  archetype ceiling of 2 (`deck.mjs` SCR_SKELETON). No claim that any sketch is part of the product.
* **Voiceover** *(mirror of `storyline.yml`; that file is the source — Decisions #21–#33):*
  > **`01-situation`** — If you have waited with a parent for a hospital bed, you know the quiet
  > despair of it. Eight to fourteen hours from arrival to a ward — and for most of it, nobody can
  > tell you why.
  >
  > **`01b-handoffs`** *(new — the archetype's situation-evidence slot, previously unused)* — Because
  > no one person owns that wait. An emergency doctor decides, a specialist confirms, bed management
  > searches, the ward receives, housekeeping releases. Five desks — and today, every handoff between
  > them is a phone call.
  >
  > **`02-complication`** — You cannot build your way out of this, because the beds already exist.
  > They are lost to callbacks that run for hours, to cubicles that can only accept one kind of
  > patient, and to discharges that clear in the afternoon when the queue formed at breakfast.
  >
  > **`02b-frozen`** *(new — the archetype's complication-consequence slot, previously unused)* — Here
  > is that cost at its sharpest. Put one man in an empty six-bed cubicle, and the other five beds can
  > now only take men — same ward class, same infection status. Five beds, empty on the board,
  > unavailable in practice. That is the capacity we set out to recover.

* **What changed and why:** the CNA reference and the phrase "orchestration failure" were **cut from
  the voice**. The CNA citation is unverifiable inside the film and reads as borrowed authority; the
  README carries it instead. "Multi-bed cubicles frozen by gender and infection locks" was the single
  most opaque clause in the old script and was spoken ~15 seconds in — it is now paraphrased in `02`
  and then *priced* in `02b`, which is the archetype's own remedy (`deck.mjs:215` warns when a
  complication names no cost). The five-desk count in `01b` is what later lets `10e` close the loop
  by referring back to "the last of those five handoffs".

---

### Beat 1 (Pain Points 1 & 2): Diagnostic Synthesis, Specialist Broadcast & Direct BMU Queue (1:15 – 3:30)

* **Routes:** `/ed` (Awaiting-Assessment board → Assessment modal) and `/specialist` (Broadcast feed).
* **Before contrast (walled-off):** ED attending paging a registrar and waiting for a callback;
  fragmented paper notes; bed requests placed by phone.

* **Scene-level SCR discipline (Decision #20):** every capture scene in this beat is tested against
  the **visibility-of-tension test** (Decision #20-b): a scene earns its own micro-SCR only if the
  complication is not visible in the same frame, or the action on screen could otherwise read as
  arbitrary. The **first** capture after the beat's setup slide is self-contained (Option A); every
  capture after it **threads** off the immediately preceding scene's resolution rather than
  re-establishing tension from zero (Option B) — see Decision #20-c. Hidden-depth features get a
  one-clause thread-in, then a direct, unhedged sell with no invented tension (Decision #20-d/g).

* **Live walkthrough — six capture scenes, up from three (Decision #20-h, grounded against the
  retired Marp storyboard `storyboard.marp-old.json`, itself re-verified live against
  `frontend/src/routes/*.tsx`):**
  1. **`04a` — Diagnostic synthesis (PP1), self-contained:** On `/ed`, open
     **Chua Wee Kiat (`Q-P120`, 64M)**. His seeded baseline (BP 98/62, HR 112, SpO₂ 94,
     **troponin 180 ng/L**, WBC 13.2) drives the smart assessment to pre-populate **Tier 2 Acute
     Urgent, Cardiology, continuous telemetry**. Attending confirms with 1 click.
  2. **`04a2` — The five-tier framework + stable-pathway contrast, threaded:** *(new scene)* Still on
     `/ed`, open the **Urgency Acuity Tier** selector (`ed.tsx` line 708) to show the full spectrum —
     Tier 1 Critical/Resuscitation through Tier 5 Extended Observation/CDU — then cut to
     **Nurul Huda (`Q-P121`, 47F, pneumonia, normal troponin — confirmed live in
     `DataInitializer.java` line 946/953)**, whose case defaults to the stable Tier 3 / General
     Medicine / no-telemetry pathway. This is the PP1 contrast pair named in §3's table but never
     previously captured — a genuine gap closed, not an invented one.
  3. **`04b` — Specialist discordance, threaded:** Switch to `/specialist`. Show the discordant case
     **Mr Fernandez (`Q-P105`)**: ED assessed Tier 3; the Cardiology specialist's completed consult
     note reads *"Elevated troponin trend and dynamic ST changes. Upgrading to Tier 2. Continuous
     telemetry mandatory."* Explain the safety-first invariant: the system auto-adopts the higher
     acuity tier and unions telemetry.
  4. **`04b2` — Consult chaining, threaded:** *(new scene)* Fernandez's case doesn't stop at
     Cardiology — it **chains an open Surgery broadcast** for concurrent abdominal pain via the
     **Chain Consult** action (confirmed live in `specialist.tsx` line 483, `useChainConsult` hook).
     The consensus gate stays open until every chained consult resolves — multi-specialty cases run
     in parallel, not a serial queue of phone calls.
  5. **`04c` — Direct digital BMU dispatch (PP2, zero phone calls), threaded:** Show structured
     packets entering the BMU queue digitally. No phone call is placed.
  6. **`04c2` — Reconcile without blocking, hidden-depth direct-sell:** *(new scene)* On `/bmu`, the
     coordinator is not locked out of the clinical picture — the **"View Comparative Notes"** action
     (confirmed live in `bmu.tsx` line 381) opens a **side-by-side "Clinical Consensus & Comparative
     Notes"** view of the ED and specialist assessments, so clinical autonomy is preserved even after
     the safety-first escalation has already fired.

* **On-screen badges:**
  * `Phone calls: 0` *(LIVE — workflow property; every bed request is digital)*
  * `Effective acuity: Tier 2 (safety-first escalation)` *(LIVE — from Q-P105)*
  * `Chained consult: Surgery` *(LIVE — from Q-P105)*
  * `Diagnostic deliberation latency` *(PROJECTED / design target — the app does not compute a
    before/after delta)*

* **Documented gap — not captured (Decision #20-f/g):** the plan's originally-named
  **SLA-timeout default on-call auto-assignment** has **no on-screen surface at all** — it is a
  backend fallback map (`ClinicianService.java`, `DEFAULT_SPECIALISTS.getOrDefault(...)`) with no
  visible countdown, banner, or button. Per Decision #20-f/g, this is still narrated — as a plain,
  unhedged, "behind the scenes" assertion layered over the existing `/specialist` broadcast-pool
  footage in `04b` or `04b2` — rather than dropped, accepting the credibility trade-off the user
  explicitly chose over the recommended verbal hedge.

---

### Beat 2 (Pain Point 3): Deterministic Care Diversion — Sister Hospitals & MIC@Home (3:30 – 5:00)

* **Routes:** `/bmu` (diversion evaluation) and `/patient` (financial/care explainer).
* **Before contrast (walled-off):** Clinicians defaulting to acute beds because community-hospital or
  virtual-ward referrals *used to* require manual multi-page packets and bilateral phone negotiations.

* **Live walkthrough:**
  1. **MIC@Home:** Show **Mrs Tan Boon Hwa (`Q-P106`, 63F)** — `DIVERTED_HAH`, virtual bed `MIC-V042`.
  2. **Sister Community Hospital:** Show **Mr Ahmad Ibrahim (`Q-P116`, 71M, post-stroke subacute)** —
     an active 30-minute bilateral SLA referral to **Outram Community Hospital (OCH)**.
  3. **BMU operational routing authority:** The coordinator reviews and approves the diversion with a
     1-click digital transfer packet.
  4. **In-app Financial & Care Advisory (FYI Insights):** On `/patient`, show the mobile explainer —
     ward-class-specific co-pay bands (e.g. Class B2 ≈ **$60–110/day, subsidized up to ~70%
     means-tested, MediShield claimable**), care expectations, and direct MSW / financial-counseling
     hotlines. Informational — no bureaucratic sign-off.
  5. **Capacity saved:** An acute inpatient bed is preserved.

* **On-screen badges:**
  * `Diversions: 2 — MIC@Home 1 / Community Hospital 1` *(LIVE)*
  * `Acute inpatient bed preserved` *(LIVE — qualitative)*
  * *Sister-hospital SLA compliance is shown on the dashboard grid only; it is **not** foregrounded as
    a spoken performance stat (simulated integration — see Decision #6).*

---

### Beat 3 (Pain Point 4): Ghost Capacity via Bed State Machine & Dynamic Cohort-Swaps (5:00 – 7:30)

* **Routes:** `/bmu` (bed capacity grid, recommendation drawer, batch & cohort-swap cards) and
  `/bmu-config`.
* **Before contrast (walled-off):** A 6-bed Class B2 cubicle with 5 empty beds that *cannot* be used
  because one patient's gender/infection profile locks the room.

* **Live walkthrough — five capture scenes, up from four (Decision #20-h):**
  1. **`07a` — The four-state bed lifecycle, self-contained (new scene, inserted first in this beat):**
     Before any mechanism can make sense, the lifecycle itself has to be shown, not just narrated —
     the old Marp-era storyboard proved this is independently capturable on `/bmu`'s
     **"Live Bed Inventory Matrix (Level → Ward → Bed)"** panel (confirmed live in `bmu.tsx` line
     1274), with all four states visible at once: `Mustard Yellow` (vacated, pending 30-min cleaning,
     line 432/539) → `White` (clean, available) → `Green` (assigned/in-transit) → `Grey` (occupied).
     This scene is placed **before** the solver scene, not after — pack-then-batch and cohort-swap are
     both operations *on* this lifecycle, so the audience needs the states named before either
     mechanism is demonstrated.
  2. **`08a` — Heuristic constraint solver, threaded off the lifecycle:** Process
     **Tan Ah Meng (`Q-P101`)**'s request against 4 hard invariants (ward class, gender, telemetry,
     infection isolation) and 3 soft scoring rules (Specialty +40, Consolidation +30, Fall-risk
     proximity +15). Show the **Top-3 recommended beds** with rationale, in under 50ms.
  3. **`08b` — Batch holding ward, threaded:** The male B2 non-infectious surge cluster
     (**Tan Ah Meng + Goh Beng Kiat + Teo Hock Seng**) triggers the **Ward 8B** batch holding-room
     recommendation (4 unlocked beds) — the same solver, run across a cluster at once.
  4. **`08c` — Dynamic cohort-swap, threaded:** **Ward 9B** is blocked by a single Green reservation —
     **Mr David Koh (`Q-P115`, 9B-01)**, who hasn't left the ED. The solver surfaces a
     **Cohort-Swap Suggestion**: move Koh to **8A-03**, resetting Ward 9B to all-`White` for a batch
     surge cohort. 1-click approval.
  5. **`08d` — Tunable solver policy, hidden-depth direct-sell:** The scoring behind those three
     decisions is not fixed — a coordinator can retune **Specialty Alignment Weight** and the other
     scoring weights live, on the **"BMU Optimization Weight Configuration"** screen (`/bmu/config`,
     confirmed live in `bmu-config.tsx` line 30/59/162/301).

* **Hidden-depth features to surface in this beat (implemented & verified):**
  * **Two-phase strategy — the actual mechanism that recovers ghost capacity:** make explicit that
    the solver runs **Phase 1 (consolidation packing)** — packing waiting patients into partially
    filled cubicles that already match their cohort, *to protect all-`White` cubicles from premature
    single-patient locking* — *before* **Phase 2 (holding-room batching)** designates an all-`White`
    cubicle for a ≥3-patient surge cluster. This "pack first, then batch" ordering is the core
    insight; the batch-holding-ward adoption KPI (**10.5%** on the current seed) measures it.
  * **Optimistic locking on cohort-swap** *(concurrency correctness)* — note that concurrent
    coordinator actions are guarded (HTTP 409 on version conflict). Message: *"it stays correct when
    many coordinators act at once."*

* **On-screen badges:**
  * `Solver evaluation: <50ms` *(LIVE — architectural property; synchronous pure-Java heuristic)*
  * `Phase 1 packing → Phase 2 batching` *(LIVE — two-phase strategy)*
  * `Batch holding-ward adoption: 10.5%` *(LIVE — verified)*
  * `Cohort lock reset: Ward 9B freed` *(LIVE — from the seeded swap)*
  * `Ghost capacity recovered` *(PROJECTED / design target — bed-days delta is not computed)*

---

### Beat 4 (Pain Point 5): Patient & Family Milestone & Queue Transparency (7:30 – 9:00)

* **Route:** `/patient` (mobile, token-based access — no passwords, no app download).
* **Before contrast (walled-off):** Families crowding the ED nurse counter every 20 minutes.

* **Live walkthrough:**
  1. **3-stage milestone stepper + queue position by ward class (verified):** Open
     **`Q-P101` (Tan Ah Meng, B2, `BED_REQUESTED`)**. The tracker computes a real position within the
     B2 queue (sorted by acuity tier then request time) with an estimated wait.
  2. **Empathetic delay tag (verified):** Open **`Q-P118` (Mdm Wong Siew Kuan)** — the specialized UV
     isolation-cleaning delay renders real reassurance copy (*"Your specialized isolation room is
     completing a mandatory 30-minute UV disinfection cycle for your safety."*) plus a contact hotline.
  3. **Diversion tracker view:** Open **`Q-P116` / `Q-P106`** — care-guidance copy for
     community-hospital / MIC@Home candidates.
  4. **Design intent (narrated, not lingered on):** the stepper is *held quiescent during ED clinical
     deliberation* so families aren't alarmed before a bed is requested, and the system supports
     *periodic 2-hourly status updates* (a scheduler capability).
  5. **Impact on nurses:** triage nurses focus on clinical stabilization, not crowd control.

* **On-screen badges:**
  * `Queue position by ward class` *(LIVE — computed)*
  * `Empathetic delay tag active` *(LIVE — from Q-P118)*
  * `Triage-nurse interruptions` *(PROJECTED / design target)*

---

### Beat 5 (Pain Point 6): Multi-Day Discharge Runway & Auto-Queued Bedside Meds (9:00 – 10:30)

* **Route:** `/ward` (inpatient runway & bed turnover).
* **Before contrast (walled-off):** The afternoon discharge gridlock — patients waiting until 3 PM
  for pharmacy meds while ED patients wait in hallways for those beds.

* **Live walkthrough (verified fixtures inp1–inp5):**
  1. **D-2 / D-3 runway:** The ward dashboard shows EDD with confidence indicators; nurses engage
     caregivers early. (inp1 = RUNWAY_D2, inp2 = RUNWAY_D3.)
  2. **Morning sign-off:** A physician completes the discharge sign-off (inp3 = READY_FOR_MORNING_SIGNOFF).
  3. **Auto-queued bedside medication:** Sign-off queues discharge meds; runners deliver to the
     bedside before 11:00 AM (inp4 = MEDICATIONS_PENDING / packing; inp5 = READY_TO_VACATE with meds
     `DELIVERED_BEDSIDE`).
  4. **30-minute housekeeping SLA:** On vacate, the bed turns `Mustard Yellow`; environmental services
     work an enforced 30-minute countdown back to `White`. (A breached example — `9A-03` — is seeded to
     show the overdue-alert state honestly.)

* **Hidden-depth features to surface in this beat (implemented & verified):**
  * **Closed-loop BMU bed release** — terminal-cleaning sign-off doesn't just flip the bed to
    `White`; it **auto-notifies BMU** that the bed is allocatable, closing the ED→ward→turnover→ED
    loop. Message: *"the recovered bed re-enters the allocation engine automatically — no phone call
    to tell BMU it's ready."* Placed as a **threaded hidden-depth clause folded into the existing
    housekeeping-turnover capture** (the old Marp-era storyboard's `29-ward-turnover` narrated this
    exact claim over the turnover-queue screen itself — the most specific, credible target available,
    reused here rather than inventing a sixth Beat 5 scene per Decision #20-h's placement review).
  * **Early caregiver readiness checklist** *(Story 4.1.2, as originally scoped)* — **documented gap,
    not captured (Decision #20-f/g):** a full grep of `ward.tsx` for "caregiver", "checklist",
    "insulin", "equipment", and "transport" returned **zero matches** — no structured checklist UI
    exists on screen at D-2/D-3, unlike closed-loop release, which at least shows its effect. Per
    Decision #20-f/g this is still narrated — as a plain, unhedged, "behind the scenes" assertion
    layered over the existing D-2/D-3 runway capture — rather than dropped, on the same accepted
    credibility trade-off as the SLA-timeout auto-assignment gap in Beat 1. Message: *"behind this
    screen, nurses also work a structured readiness checklist — insulin training, home equipment,
    transport — before the discharge morning ever arrives."*

* **On-screen badges:**
  * `Bedside medication delivery adoption: 50%` *(LIVE — verified)*
  * `Housekeeping turnover avg: 26.5 min` and `30-min SLA compliance: 87.5%` *(LIVE — verified)*
  * `Caregiver readiness checklist: complete before discharge morning` *(LIVE — workflow property)*
  * `Discharge before noon` *(LIVE — 100% on current seed; may be shown, spoken carefully)*

---

### Act 3: Operational Control Tower, Synthesis & Call to Action (10:30 – 12:00)

* **Route:** `/analytics` (dual-pathway observability dashboard) + live demo link.
* **Framing (Decision #4): proof of instrumentation & observability — not proof of outcome
  magnitude.** The message:
  > Every operational decision in this system is measured. Here are the operational KPIs the platform
  > computes in real time — the same numbers a control tower would watch — and each is extractable via
  > two independent pathways: SQL queries for BI dashboards, and structured-log parsing for real-time
  > SIEM alerting, verified for mathematical parity.

* **LIVE KPI values to speak (captured from the running prototype — all non-zero after seed
  enrichment):**
  * **21 digital bed requests — zero phone calls** (`digitalBedRequestCount = 21`)
  * **Specialist concordance 66.7%** *(small denominator — frame as "the mechanism resolves
    discordance," not as a headline rate)*
  * **BMU suggestion acceptance 63.2%**
  * **ED assessment turnaround p95 ≈ 16 min** (`avgEdTurnaroundMinutes = 2.9`)
  * **Batch holding-ward adoption 10.5%** · **Diversions 2** (MIC@Home 1 / Community Hospital 1,
    diversion rate 9.5%)
  * **Patient tracker access 19%** · **2-hour update delivery 100%** · **prolonged-wait
    communication 100%** · **caregiver-counseling connect 100%**
  * **Bedside medication delivery adoption 50%** · **advance discharge runway 28.6%**
  * **Discharge before noon 100%** · **Housekeeping turnover 26.5 min avg / 87.5% within 30-min SLA**
* **Framing notes:** the dashboard now reads non-zero across every metric on the synthetic seed, so a
  curious viewer inspecting the live prototype (per the CTA) finds a self-consistent control tower.
  Several rates are small-denominator (concordance 66.7% of 3; batch-holding 10.5%) — speak these as
  *"the mechanism works and is measured,"* not as benchmarked performance. Sister-hospital SLA % is
  shown on the grid but not spoken as measured performance (Decision #6).
* **Projected / design-target framing** for any improvement deltas.

* **Closing monologue & CTA:**
  > Hospital bed capacity is not fixed. When clinical decisions are synthesized in real time, when bed
  > constraints are solved mathematically instead of over the phone, and when discharge logistics are
  > pulled forward into morning rounds, we recover capacity that was there all along.
  >
  > Explore the interactive live prototype. Test the clinical flows, inspect the constraint engine, and
  > see how intelligent orchestration can transform hospital admissions.

* **Closing screen:** Live prototype URL `https://enhance-hospital-admissions.onrender.com` · QR code ·
  repository & documentation references.

---

## 6. Documented Assumptions & Decision Path Log

*(This log reflects the decisions taken during the codebase-grounded review. Items marked **[REVISED]**
changed from the original plan.)*

1. **Dual Constituency Target:** Pitch jointly to Hospital Operations C-Suite (COO/CMIO) and Clinical
   Frontline Chiefs (ED/Ward Nursing) to create both executive budget motivation and grassroots
   clinician buy-in.

2. **Tri-Hybrid Narrative:** Combine the Patient Tracer Journeys, the Before-vs-After contrast, and the
   Pain Point Matrix (systematic proof of all 6 root causes). The pain-point → beat mapping is verified
   1-to-1 against `pain-points.md`; Beat 1 intentionally covers PP1 (synthesis/broadcast) and PP2
   (digital dispatch) as sub-beats 1a/1b.

3. **[REVISED per Decision #20-e] Comprehensive Runtime:** Originally scoped as a single, linear
   10–12 minute walkthrough. **Superseded** by a live design-review session (Decision #20-e): "sell
   the prototype properly" outranks the runtime ceiling — the film now runs longer (est. 14–16+
   minutes with the 5 added capture scenes in §5) because full feature coverage is the binding
   constraint, not completion rate. Timeline **chapter markers** (additive) remain published for
   navigation, which matters more once runtime grows.

4. **[REVISED] Real Seeded Personas (was: composite Mr. Tan / Mdm. Halimah):** Follow the *actual*
   seeded patients through their real states (see §3 table). The value is systemic behavior distributed
   across purpose-built fixtures; live screen-capture must match the on-screen names/tokens exactly.

5. **[REVISED] Production Aesthetic (was: founder-led on-camera + kinetic motion-graphics):** The core
   is a **live React UI screen-capture walkthrough + voiceover + simple static badges.** The existing
   `video-generation/` Marp pipeline is a *supporting* layer (title, before/after panels, transitions),
   not the film's heart. Rationale: the entire credibility argument rests on "this is real, running,
   and inspectable"; footage of the live app beats slides and matches the "go try it" CTA.

6. **Prototype Boundaries / IT Jargon:** Sell the *concept*. Omit dense integration jargon (FHIR/HL7,
   procurement) from the narrative; the README carries the honest prototype-vs-production boundaries.
   **Refinement:** do not verbally foreground simulated-flow hard numbers (e.g. sister-hospital SLA %)
   as measured performance in the finale.

7. **[NEW] "Before" treatment:** Every "before" visual is an explicitly-labelled, walled-off
   illustration ("Today, without the system"), figures representative, sourced only where genuine
   (lived experience + CNA for the qualitative crisis).

8. **[NEW] KPI badge classes:** Two distinct visual classes — **LIVE** (computed, verifiable on
   `/analytics`) vs **PROJECTED / DESIGN-TARGET** (improvement deltas the app cannot compute). `<50ms`
   solver and `Phone calls: 0` are architectural/workflow properties, not dashboard readings.

9. **[NEW] Finale = observability proof:** `/analytics` proves *instrumentation and dual-pathway
   extraction*, not outcome magnitude (values are computed from a synthetic dataset).

10. **[NEW] Seed self-consistency:** The seed was additively enriched (new patients `Q-DIS-556`
    discharged-with-bedside-meds; `Q-P130` tracker/periodic/delay exemplar; `Q-P131`/`Q-P132`
    batch-holding allocations; caregiver-counseling interactions for `Q-P106`/`Q-P116`; tracker access
    on `Q-P101`) so **every finale KPI reads non-zero and self-consistent**. Final verified values:
    21 digital requests, ED p95 16 min, concordance 66.7%, BMU acceptance 63.2%, batch-holding 10.5%,
    tracker access 19%, 2-hour updates 100%, prolonged-wait comms 100%, caregiver counseling 100%,
    bedside meds 50%, discharge-before-noon 100%, turnover 26.5 min / 87.5% SLA, 2 diversions. No
    existing asserted fixtures were altered; the full backend suite of **214 tests passes**.

11. **[NEW] Hidden-depth features surfaced:** Beats 1/3/5 now explicitly showcase implemented-but-
    previously-unsold capabilities — **SLA-timeout default on-call auto-assignment**, **side-by-side
    reconcile + secure clinician messaging**, **consult chaining**, the **5-tier priority framework**,
    the **two-phase pack-then-batch solver strategy** (the real ghost-capacity mechanism), **optimistic
    locking** on cohort-swap, the **early caregiver readiness checklist**, and **closed-loop BMU bed
    release**. Positioning spine for the dual audience: **safety-first by construction**,
    **audit-ready dual-pathway observability**, and **production-architected profile boundary** (default
    beans are production contracts that fail-fast until real infrastructure is wired).

12. **[NEW] SCR pitch deck as narrative spine (§7):** Author a standalone, polished, non-wordy pitch
    deck structured on the McKinsey **Situation–Complication–Resolution** framework. The deck is the
    *source of truth*; the same slides become the film's replacement slide layer. Rejected: SCR as a
    mere cold-open, and a full re-skin of the film body.

13. **[NEW] Nested SCR structure:** One macro-SCR — global **Situation** (the bed crunch) and
    **Complication** (an orchestration failure, not a construction problem) are established *once*.
    The **Resolution** then decomposes into the 6 pain points, each carrying its *own local*
    complication→resolution. Deck and film share one SCR spine; the deck is the compressed subset, the
    film carries every feature. Rejected: restating full S-C-R at every feature (repetition fatigue)
    and a single macro-SCR only (undersells the features).

14. **[NEW] Deck size & granularity:** ~13–15 slides at **pain-point granularity** — one *hero*
    feature named per pain point, with supporting/hidden-depth features as sub-bullets or speaker
    notes. One message per slide. Rejected: 6-slide ultra-compressed (undersells depth) and ~30-slide
    feature-granularity (rebuilds the film in slides).

15. **[NEW] Slides frame — not contain — the walkthrough:** The slides are polished SCR *message
    cards* positioned around the untouched live `playwright_web` scenes. Chosen arrangement is
    **replace + minimal insert**: replace all existing Marp slides for polish, and insert only a small
    number of new cards where a pain point currently lacks a setup beat (a governing Resolution-overview
    card, plus PP2 and PP5 setup cards). Per-block cadence: **setup slide (issue + resolution) → live
    Playwright payoff**. Rejected: embedding clips inside slides (rebuilds the film, breaks the PDF
    leave-behind) and a slide before every micro-scene (runtime bloat, breaks walkthrough momentum).

16. **[NEW] Voice register — warm bookends, sharp middle:** Human/cinematic voice for the Situation
    and the CTA; crisp analytical SCR for the 6 pain-point setup slides. SCR structure is present but
    **unlabelled** (flows as prose, not literally tagged "Issue:/Resolution:") — contrast comes from
    visual layout and narration cadence. *(Flag: flip to explicit labels if overt McKinsey scaffolding
    is preferred.)*

17. **[NEW] Proof placement — mechanism-only setup slides:** Setup slides carry the qualitative
    resolution *mechanism* only (at most one LIVE architectural fact such as "zero phone calls" or
    "<50ms solver"). All quantified proof — LIVE and PROJECTED — stays on the walkthrough footage and
    consolidates on the control-tower slide, preserving SCR's claim-then-evidence order and the
    LIVE-vs-PROJECTED integrity discipline (Decisions #6/#8/#9).

18. **[NEW] Build format — stay in Marp:** Build the deck in the existing Marp pipeline
    (`render-marp.mjs`: `scenes/*.md` → PNG → MP4) with an SCR **template system** (three layouts:
    *cinematic-bookend*, *sharp-SCR-body*, *control-tower*). Polish via type scale, whitespace, a
    restrained 2-accent palette, and fewer words. For the 1–2 diagram-heavy slides (Resolution-overview
    map, pack-then-batch), embed an authored SVG/image asset. Rationale: keeps the deck and the film's
    slide layer a *single source of truth*. Rejected: authoring externally (Keynote/Figma/Gamma) —
    forks the artifact and breaks the deck-drives-film link.

19. **[NEW] Narration workflow — narration-first spine + verification gate:** Write the SCR narration
    script first; derive slide headlines from it (shared wording). Scope the rewrite to the ~10 slide
    scenes plus a light SCR retouch of live-scene openings; leave the bulk of the verified live-scene
    narration intact. **Mandatory gate:** any narration touching a name, token, route, or metric is
    re-verified against the codebase and live `GET /api/v1/analytics/kpis/summary` *before*
    re-synthesis; then re-synthesise affected audio and re-resolve timing. Rejected: slides-only
    (wording/cadence mismatch) and a full 34-scene rewrite (cost + regression risk against the locked,
    verified audio).

20. **[NEW] Scene-level SCR + full hidden-depth coverage (Socratic design review, this session).**
    A live question-by-question review (see chat transcript) reached the following sub-decisions,
    which together supersede parts of Decisions #15/#19 above:
    * **20-a — Scope:** SCR framing applies at the **individual capture-scene** level, not just the
      beat/setup-slide level (Decision #15's original grain) — but *selectively*, not universally.
    * **20-b — Test:** A scene earns its own micro-SCR only if it fails the **visibility-of-tension
      test**: the complication is not visible in the same frame, or the action could otherwise read
      as arbitrary/unmotivated. Scenes that pass (tension already visible, or continuing a beat
      already framed by its setup slide) stay pure evidence, per Decision #17.
    * **20-c — Threading:** Within a beat, only the **first** capture after the setup slide is
      self-contained (states its own mini complication→resolution in isolation); every subsequent
      capture in that beat **threads** off the immediately preceding scene's resolution rather than
      re-establishing tension from zero — preserving flow, avoiding the "slide before every
      micro-scene" runtime-bloat problem Decision #15 already rejected, just applied one level down.
    * **20-d — Hidden-depth placement:** A hidden-depth/direct-sell scene landing mid-thread bridges
      in with one clause referencing the prior scene's momentum, then pivots to a bare, un-hedged
      capability statement — no fabricated tension is invented for features that were always meant to
      be sold directly (per the original hidden-depth carve-out).
    * **20-e — Runtime ceiling lifted:** superseding Decision #3 above — "sell the prototype
      properly" is the binding goal; runtime is not a constraint. See Decision #3's revision note.
    * **20-f/20-g — Undemonstrable features, still sold, unhedged:** two named hidden-depth features
      (SLA-timeout default on-call auto-assignment; early caregiver readiness checklist) were found,
      via direct grep against the live codebase, to have **no on-screen UI surface at all** — not
      even a partial one. A third (closed-loop BMU bed release) has a demonstrable *effect* (bed
      turns white, reappears in the BMU queue) but not a demonstrable *notification*. All three are
      still narrated as plain, confident, **unhedged** assertions layered over the nearest relevant
      real screen (not a dedicated new scene) — a deliberate, explicit trade of narration-vs-footage
      credibility risk in exchange for full feature coverage. The originally-recommended verbal tell
      (e.g. "though you won't see it here...") was considered and explicitly rejected.
    * **20-h — Gap-fill against the retired Marp storyboard:** the pre-skill `storyboard.marp-old.json`
      (34 scenes, superseded when the deck moved to the `video-generator` skill's `storyline.yml`/SCR
      pipeline) was mined as a **grounding reference only** — its narration was not reused (it predates
      every SCR/threading/hidden-depth rule above), but its Playwright selectors and `assert` blocks
      were, since they were previously proven against the running app. This surfaced 5 real,
      previously-uncaptured, capturable moments, all re-verified live against current route source
      before being added to §5: the five-tier dropdown + Nurul Huda (`Q-P121`) stable-pathway
      contrast, Chain Consult, the Reconcile / "View Comparative Notes" side-by-side view, and the
      four-state Live Bed Inventory Matrix. Total capture scenes: **18 → 22** *(verified against the
      compiled storyboard: 34 entries = 12 slides + 22 captures; the earlier figure of 23 was an
      off-by-one)*.

21. **Narration clarity rewrite — the diagnosis.** Review of the shipped narration found it *terse but
    referentially incomplete*: the film named mechanisms without binding them. Four distinct gap
    classes were identified, and every line was rewritten against them:
    * **Unbound noun** — the noun is right but unqualified. *"Findings are synthesised on arrival"* —
      which findings? Also *"the packet"* (`04c`), *"comparative notes"* (`04c2`).
    * **Undefined jargon** — a term of art spoken before the viewer could know it. Worst case:
      *"cubicles frozen by cohort locks"* ~15s in, carrying the entire Complication.
    * **Unstated criterion** — a matching claim with no basis given. *"Three matching patients"*
      (`08b`), *"matching cubicles"* (`07`), *"the top three beds"* (`08a`).
    * **Missing payoff** — the feature described, the point never stated. Clearest case `08d`, which
      described a config screen and never said why tunability matters.
    A structural cause was also identified: the granular capture split (Decision #20-h) raised the
    need for connective tissue between clips, and that transition debt was never paid. Runtime
    ceiling set at **12 minutes**; measured result ~11m30s across 32 scenes.

22. **Plain language in the voice; technical labels stay on the cards.** Jargon that is merely
    shorthand for an idea survives paraphrase intact, so it is dropped from the voice entirely:
    *cohort lock, discordance, bilateral, subacute, step-down, concordance, ghost capacity*. The
    slide cards keep the precise labels — card = label, voice = meaning.

23. **Arbitrary internal labels are paraphrased; professional vocabulary is kept.** The dividing line
    is *does the listener already own this word?*, not *is it on screen?* The bed-state colours are
    internal codes that mean nothing outside this codebase, so the voice says "being cleaned, ready,
    promised, occupied" — and `bmu.tsx:1285-1297` already prints colour **and** meaning in the legend,
    so the screen does the teaching for free. "Acuity tier", "telemetry" and "troponin" are real
    clinical vocabulary and are kept, glossed once on first use.

24. **Audience is A + B + D simultaneously** (technical reviewer, health executive, general public) —
    explicitly *not* C (clinician-only). Since A and B lose nothing from a plain explanation but D
    loses everything from a bare precise term, the resolution is ordering within the sentence:
    the **no-orphan rule** — plain meaning first, precise term second in apposition, and a precise
    term is never the sole carrier of an idea.

25. **Decision #17 reinterpreted: it bars outcome magnitude, not numerals.** Read literally, "withhold
    all quantified proof until the control tower" was the *direct cause* of the vagueness in #21 —
    "four rules", "five beds", "three patients sharing a ward class" are all numbers. The distinction
    that resolves it: *proof numbers* assert the system worked and must be auditable in one place;
    *descriptive cardinalities* define what a mechanism is and withholding them merely leaves it
    unspecified. This codifies existing practice rather than loosening a rule — `05`, `07a`, `08a` and
    `09` already spoke numerals, while `stat` blocks appeared only on `01` and `11`.

26. **Per-scene shape: contrast bridge → mechanism with bound nouns → payoff.** The bridge names the
    old way in one clause, which is how the retired Marp deck's separate before/after slides are
    recovered *without* new beats — resolution beats are hard-capped at 5 (`deck.mjs:92`, error not
    warning) and all 5 are in use. The shape is a drafting checklist, not an audible template; which
    part is explicit varies by scene.

27. **Deixis at every screen or persona change** ("Up on the ward now", "Back in bed management"). With
    22 hard cuts, each costs the viewer a "where am I?"; two words resolve it before the content
    arrives. This is the cheapest available fix for the between-scenes gap.

28. **Authorial "we" for design rationale only, rationed to ~5 uses.** Audience A is evaluating
    judgement, not the hospital — and only an authorial voice can *defend a decision* (e.g. "we take
    the higher tier … because averaging two clinical opinions is the one thing a system must never
    do"). Impersonal for mechanism description; second person rejected as reading like marketing.

29. **Protagonist: Mr Tan Ah Meng.** He already appeared in `04c` / `04c2` / `08a` / `10a` — dispatch →
    comparative notes → solver match → tracker, a nearly complete journey the script never told the
    viewer to follow. Each return is now signposted, which doubles as connective tissue and shows the
    features are *one system* rather than seven independent screens. The other six named patients stay
    as one-off exhibits; an audit confirmed **none is redundant** (each carries a mechanism he cannot).

30. **Name collision handled linguistically, not by reselection.** Mr Tan Ah Meng and Mrs Tan Boon Hwa
    share a surname four minutes apart in an audio-only channel. P106 is the **only** hospital-at-home
    diversion in the seed (`DataInitializer.java:1245`), so no swap is possible; both are therefore
    always spoken in full with honorifics, never as bare "Tan", and `06a` marks her explicitly as a
    different patient.

31. **Clinical specifics restored in full** from the seed, so the voice describes what is actually on
    screen: Chua Wee Kiat BP 98/62, troponin 180 ng/L (`DataInitializer.java:922-935`); Nurul Huda BP
    124/78, troponin Normal (`:946-959`); and the abdominal pain that justifies Fernandez's chained
    surgical review (`:800`) — previously omitted from the voice **despite being printed on screen**.
    Voice–screen agreement is itself a comprehension mechanism, and the specificity is what
    distinguishes "built a form" from "modelled a clinical decision".

32. **The priced complication (`02b`) uses no invented figure.** `product-idea/` contains no citable
    status-quo cost, and fabricating one was rejected. The stat is derived from facts already in the
    system: a Class B2 cubicle is 5–6 beds (`ed.tsx` ward-class options) and gender cohorting is an
    absolute invariant, therefore five of six beds can only go to patients matching the first. True by
    construction, and a cost of **today** — so it does not breach #25's reservation of proof for the
    control tower.

33. **Capture delays are derived, not guessed.** Slides are elastic (`core.mjs planTiming`:
    `audio-dictates`, a still is held for any duration at zero cost), but captures are inelastic —
    speech is rate-fitted, inaudible to ±7%, capped to 20%, then `reject`. Each capture's trailing
    `wait delayMs` is therefore set to *(estimated narration ms + 400ms tail) − (sum of preceding step
    delays)*, at ~150 wpm. **These must be re-derived from measured audio after the first
    `synthesize.mjs` run** — guessing them is what would force a second, expensive capture pass.

---

## 6a. Open items carried forward

* **"Troponin" gloss** — kept and glossed once ("the protein that leaks into the blood when heart
  muscle is dying"). This is the one place the A+B+D audience cannot all be served optimally; the
  alternative was speaking only the plain form and letting the number sit on screen unspoken.
* **The printed deck loses the contrast.** Because #26 puts the before/after in the *voice*, the
  PDF/PNG deck shows mechanism cards with no before/after columns. A `comparison`-layout variant on
  the deck-only path would fix it.
* **ROI vs spoken values.** `04a` is scoped to `.lg\:col-span-5`; any value narrated but rendered
  outside that panel breaks the voice–screen match. Troponin survives via the on-panel
  "troponin-positive chest pain" string, but this needs a visual check at capture time.
* **The 9:16 output is format-mismatched** at ~11m30s (vertical norms are under ~90s). Accepted for
  now by choosing a single cut; the `in:` field already used by `00-answer` is the mechanism if a short
  cut is ever wanted.
* **`08d` and `12d` remain the two weakest scenes** and survive only on the strength of their new
  payoff clauses.

---

## 6b. Decision #34 — the source file must be named `storyline.yml`

The authored source was originally `video-generation/deck.yaml`, which *appeared* to work because
every command in this project was run with an explicit `--storyline=` override. It was renamed to
`storyline.yml` after the override was found to be load-bearing in a way that fails silently.

* `compile.mjs:38` — `storylineFile ?? path.join(workDir, 'storyline.yml')`: overridable.
* `deck.mjs:305` — same fallback: overridable.
* `generate.mjs:83` — **hardcodes** `path.join(workDir, 'storyline.yml')` with no flag. The documented
  one-liner cannot be pointed at another filename at all.

Under the old name the two failure modes were both silent, because each is guarded by `existsSync`
rather than an error:

1. `generate.mjs:90` gates compilation on the file existing, so it **skipped compiling entirely** and
   ran against whatever `storyboard.json` was already committed. Narration edits would have been
   dropped with no warning — defeating the skill's own reason for committing the lockfile.
2. `deck.mjs` fell back to `storylineSource = null`, and `slidesFor` (`deck.mjs:63`) returns only
   storyboard slides in that case. Since `in: [deck]` entries are **excluded** from the storyboard by
   design, both answer-first slides (`00-answer`, `00b-resolution-map`) silently vanished from the
   deck. This was caught by file mtimes: the two PNGs were 22 hours older than the other ten, i.e.
   stale leftovers that had never been overflow-checked against the current source. After the rename
   the render goes from 10+10 to **12 landscape + 12 vertical**.

The fix is the rename, not a wrapper script: aligning with the tool's convention removes the failure
mode, whereas documenting the override preserves it for whoever forgets. The header of
`storyline.yml` now carries this warning inline, since that is where someone tempted to rename it
will be looking.

---

This section specifies the **Situation–Complication–Resolution (SCR)** pitch deck that is authored as
the *source of truth* for the film. The deck exists as a standalone, polished, non-wordy artifact
(fixing the current slides' two problems: unpolished styling and excess words) **and** provides the
replacement slide layer that frames the live walkthrough. It reuses the existing verified narration,
reframed to *sell* each feature via SCR.

> **Why SCR:** the McKinsey SCR framework is answer-first — establish the *Situation*, name the
> *Complication*, then deliver the *Resolution*. Our structure embodies this literally: a setup slide
> states the issue and the resolution, and the live Playwright footage that follows is the
> demonstration/evidence. See <https://managementconsulted.com/mckinsey-scr-framework/>.

### 7.1 Nested SCR model

* **Macro-Situation (once):** the bed crunch — ageing population, fixed bed stock, hours-long ED waits.
* **Macro-Complication (once):** it is an *orchestration failure*, not a construction deficit — you
  cannot build your way out of a flow bottleneck.
* **Macro-Resolution (decomposed):** intelligent orchestration across the whole journey, expressed as
  **6 local complication→resolution pairs**, one per documented pain point. Each pain point names its
  *specific* complication (not a rephrase of the global one) and the hero feature that resolves it.

This avoids repetition fatigue (the global Situation is established once) while still selling every
feature (each pain point gets its own crisp tension→release).

### 7.2 Deck outline (~13–15 slides, one message per slide)

> **Note:** the "Maps to film scenes" column below still reflects this section's original
> 34-scene numbering scheme from the pre-`video-generator`-skill Marp era. The actual, current
> implementation lives in `presentations/product-demo/storyline.yml` and uses its own scene IDs (e.g. `04a`,
> `04a2`, `04b`, `04b2`, `04c`, `04c2` for PP1/PP2; `07a`, `08a`–`08d` for PP4) — see the updated
> Beat 1 and Beat 3 walkthroughs in §5 above for the current, authoritative scene list and
> threading order per Decision #20. This table is kept for historical SCR-role/register reference
> only; treat `storyline.yml` as the source of truth for exact scene IDs and count.

| # | Slide | SCR role | Register / template | Maps to film scenes |
| :-- | :--- | :--- | :--- | :--- |
| 1 | Title | — | cinematic-bookend | 01 |
| 2 | **Situation** — the human cost of the bed crunch | S (macro) | cinematic-bookend (human) | 02 |
| 3 | **Complication + Resolution overview** — orchestration failure + the 6-part answer-first map | C + governing R | sharp-SCR-body (+ embedded SVG map) | 03 |
| 4 | **PP1** — ED synthesis, specialist broadcast & safety-first discordance | c→r | sharp-SCR-body | 04–12 |
| 5 | **PP2** — zero-phone digital BMU dispatch *(new card)* | c→r | sharp-SCR-body | 11–12 |
| 6 | **PP3** — deterministic diversion (MIC@Home / sister hospital) | c→r | sharp-SCR-body | 13–16 |
| 7 | **PP4** — ghost capacity: pack-then-batch solver + cohort-swap *(consolidates old ghost-capacity + two-phase; embedded SVG)* | c→r | sharp-SCR-body | 17–23 |
| 8 | **PP5** — patient & family transparency *(new card)* | c→r | sharp-SCR-body | 24–25 |
| 9 | **PP6** — discharge runway, bedside meds & closed-loop turnover | c→r | sharp-SCR-body | 26–29 |
| 10 | **Control tower** — every decision measured (instrumentation proof) | R-proof | control-tower | 30–33 |
| 11 | **Close / CTA** — capacity was there all along; explore the live prototype | — | cinematic-bookend (human) | 34 |
| 12–15 | *Flex/appendix* — promote a hidden-depth feature to its own slide, or appendix | — | — | — |

**Standalone deck** = these slides. **Film slide layer** = the same slides slotted at the mapped scene
positions, each followed by its live payoff scenes.

### 7.3 Per-block cadence in the film

For every pain point: **SCR setup slide (issue + resolution, mechanism-only) → live Playwright
walkthrough (the demonstration).** The setup slide plants the local complication and names the
resolution; the footage proves it. Resulting film spine (~10 slide scenes around the untouched live
scenes):

```
02 Situation (human) → 03 Complication + Resolution-overview map
  → PP1 setup → live 04–12
  → PP2 setup (new) → live 11–12
  → PP3 setup → live 14–16
  → PP4 setup (consolidated) → live 18–23
  → PP5 setup (new) → live 24–25
  → PP6 setup → live 27–29
  → Control tower → live 31–33
  → Close/CTA (human)
```

### 7.4 Voice & proof rules

* **Register:** warm/human for the Situation and CTA bookends; crisp analytical SCR for the
  pain-point setup slides. SCR structure present but **unlabelled** (prose, not "Issue:/Resolution:"
  tags).
* **Stance (Decision #28):** impersonal for mechanism description; **authorial "we" reserved for
  design rationale**, ~5 uses across the film. Deixis at every screen or persona change
  (Decision #27). Second person is not used.
* **Vocabulary (Decisions #22–#24):** plain language in the voice, technical labels on the cards.
  Arbitrary internal labels (bed-state colours) are paraphrased; real clinical vocabulary is kept and
  glossed once. The **no-orphan rule** governs every precise term: plain meaning first, precise term
  second in apposition.
* **Every line binds its nouns (Decision #21):** to the scene's own input *and* output, never to a
  downstream payload. The ED synthesis emits tier / specialty / ward class / monitoring — **not a
  bed**; the bed is the solver's output two beats later.
* **Proof placement — REVISED per Decision #25:** setup slides remain **mechanism-only** in the sense
  that matters — no `stat` block, and no claim about how well the prototype performed. But
  **descriptive cardinalities are now encouraged wherever they remove ambiguity** ("four rules it will
  never break", "five beds", "a thirty-minute clock", "three patients sharing a ward class"). The
  earlier blanket ban on numerals was found to be the direct cause of the film's vagueness, and it
  never matched practice — `05`, `07a`, `08a` and `09` already spoke numerals. *Outcome magnitude*
  (both LIVE and PROJECTED) still consolidates on the control-tower slide, preserving the
  claim-then-evidence order and the LIVE-vs-PROJECTED integrity discipline (Decisions #6/#8/#9).
* **No invented figures (Decision #32).** Where no citable number exists, price the status quo from
  facts already true in the system, or stay qualitative. `02b`'s "five frozen beds" is derived, not
  sourced.

### 7.5 Build & pipeline

* **Superseded:** the Marp pipeline (`scenes/*.md` → `render-marp.mjs`, now under
  `presentations/product-demo/`) is retired.
  Those `scenes/*.md` files and `storyboard.marp-old.json` are kept as historical grounding references
  only (Decision #20-h) and are **not** read by the current build.
* The live pipeline is the `presentation-producer` skill's: **`presentations/product-demo/storyline.yml` is the
  authored source** (`engine: html`, bento templates, `theme: light`) — the filename is fixed by the
  tooling, see Decision #34 — → `compile.mjs` → `storyboard.json`
  (generated lockfile, committed, **never hand-edited** — `compile.mjs` refuses a file without the
  `$generated` marker) → `deck.mjs` (slides, free, no TTS) → **outline gate** → `synthesize.mjs` →
  `capture.mjs` → `compose.mjs` → `verify.mjs`.
* Stages are resumable and the cache is content-addressed per scene, so editing one sentence costs one
  scene — **except** that capture keys include the app version (`ledger.mjs:96`), so a changed app
  HEAD or a changed `delayMs` rebuilds that clip.
* Regeneration order after a narration edit: `compile` → `deck` → review PNGs → `synthesize` →
  **re-derive every capture's trailing `wait delayMs` from measured audio (Decision #33)** →
  `capture` → `compose` → `verify`.
* For the 1–2 diagram-heavy slides (the 6-part Resolution-overview map; the pack-then-batch visual),
  author the diagram as an embedded SVG/image asset within the slide.

### 7.6 Narration workflow (order of operations)

1. Write the **SCR narration script** (the spine): macro-S, macro-C + Resolution-overview, the 6
   local complication→resolution setups, control-tower, CTA.
2. **Re-verify** every name, token, route, and metric against the codebase and live
   `GET /api/v1/analytics/kpis/summary` (prototype profile).
3. Derive **slide headlines** from the script (shared wording) and build the three Marp templates +
   the ~11-slide deck.
4. **Slot** slides into the storyboard, adding the PP2, PP5, and Resolution-overview scenes.
5. **Re-synthesise** only the ~10 slide scenes + the lightly-retouched live-scene openings; leave the
   bulk of the verified live-scene audio intact.
6. **Re-render** via `render-marp.mjs` and re-compose.

### 7.7 Open items to confirm before build

* **PP4 consolidation:** the old ghost-capacity + two-phase slides are merged into one PP4 setup card
  (one-slide-per-block pattern). If the pack-then-batch insight — the core ghost-capacity mechanism —
  deserves its own beat, split it back into two setup slides.
* **Unlabelled vs explicit SCR:** body slides use unlabelled SCR prose; flip to explicit
  "Issue/Resolution" labels if overt scaffolding is preferred.
* **Flex slides (12–15):** reserved for promoting a hidden-depth feature (e.g. two-phase solver,
  closed-loop bed release) to its own slide, or for an appendix — currently unallocated.
