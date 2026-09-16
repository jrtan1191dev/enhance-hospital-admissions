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

This video is an authoritative, in-depth **product walkthrough film** (~10–12 minutes) whose purpose
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
  walkthrough.
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

## 5. Master Beat Sheet & Scene-by-Scene Script (10–12 Minutes)

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

* **Visual:** Title card (Marp) + walled-off animated "Today, without the system" workflow sketch of
  fragmented communication. No claim that the sketch is part of the product.
* **Voiceover:**
  > If you've ever accompanied an elderly parent to a public hospital emergency department, you know
  > the quiet despair of waiting eight, ten, or fourteen hours for an inpatient bed. You watch doctors
  > running between triage bays, nurses answering endless phone calls, and families demanding answers
  > staff simply don't have.
  >
  > When CNA's Talking Point documented this persistent crisis, it confirmed what healthcare leaders
  > already know: the bed crunch is not just an infrastructure deficit — you cannot build your way out
  > of a flow bottleneck. It is an **orchestration failure** — manual phone tag between doctors,
  > multi-bed cubicles frozen by gender and infection locks, and discharge gridlocks that trap beds
  > deep into the afternoon.
  >
  > This prototype was built to attack those root causes. Let me walk you through how it works — live.

---

### Beat 1 (Pain Points 1 & 2): Diagnostic Synthesis, Specialist Broadcast & Direct BMU Queue (1:15 – 3:30)

* **Routes:** `/ed` (Awaiting-Assessment board → Assessment modal) and `/specialist` (Broadcast feed).
* **Before contrast (walled-off):** ED attending paging a registrar and waiting for a callback;
  fragmented paper notes; bed requests placed by phone.

* **Live walkthrough:**
  1. **Beat 1a — Diagnostic synthesis (PP1):** On `/ed`, open **Chua Wee Kiat (`Q-P120`, 64M)**. His
     seeded baseline (BP 98/62, HR 112, SpO₂ 94, **troponin 180 ng/L**, WBC 13.2) drives the smart
     assessment to pre-populate **Tier 2 Acute Urgent, Cardiology, continuous telemetry**. Attending
     confirms with 1 click. (Contrast with **Nurul Huda `Q-P121`**, whose normal troponin defaults to
     the stable Tier 3 / General Medicine / no-telemetry pathway.)
  2. **Specialist broadcast pool:** Switch to `/specialist`. Show the discordant case
     **Mr Fernandez (`Q-P105`)**: ED assessed Tier 3; the Cardiology specialist's completed consult
     note reads *"Elevated troponin trend and dynamic ST changes. Upgrading to Tier 2. Continuous
     telemetry mandatory. Recommending chained surgery review for concurrent abdominal pain."*
  3. **Safety-first discordance engine:** Show both judgments side-by-side. Explain the invariant: the
     system **auto-adopts the higher acuity tier (effective Tier 2) and unions telemetry**, and a
     **chained Surgery broadcast** keeps the consensus gate open — all without delaying dispatch.
  4. **Beat 1b — Direct digital BMU dispatch (PP2, zero phone calls):** Show structured packets
     entering the BMU queue digitally. No phone call is placed.

* **Hidden-depth features to surface in this beat (implemented & verified):**
  * **SLA-timeout default on-call auto-assignment** *(Story 1.2.2)* — narrate that if no specialist
    claims a broadcast within the SLA window, the system auto-assigns the designated default on-call
    physician. Message: *"a consult is never lost to an unmonitored feed."*
  * **Side-by-side reconcile + secure clinician messaging** *(Story 1.3.2)* — on the discordance
    view, highlight the dual-column comparison and the "Prompt Clinician Reconcile" action. Message:
    *"clinical autonomy is preserved — the system escalates for safety but opens a channel for the
    doctors to align."*
  * **Consult chaining** — Mr Fernandez's Cardiology consult *chains* an open Surgery broadcast for
    concurrent abdominal pain; the consensus gate stays open until it resolves. Message:
    *"multi-specialty cases run in parallel, not in a serial queue of phone calls."*
  * **5-Tier priority framework** — briefly show the disposition spectrum (Tier 1 Critical/ICU → Tier
    5 Short-Stay/CDU) so the audience sees the model covers the whole admit/observe/divert range.

* **On-screen badges:**
  * `Phone calls: 0` *(LIVE — workflow property; every bed request is digital)*
  * `Effective acuity: Tier 2 (safety-first escalation)` *(LIVE — from Q-P105)*
  * `Consult SLA: auto-escalates to default on-call` *(LIVE — architectural/workflow property)*
  * `Diagnostic deliberation latency` *(PROJECTED / design target — the app does not compute a
    before/after delta)*

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

* **Live walkthrough:**
  1. **The 4-state bed lifecycle** (exact enum, verified): `Mustard Yellow` (vacated, pending 30-min
     cleaning) → `White` (clean, available) → `Green` (assigned/in-transit) → `Grey` (occupied).
  2. **Cubicle cohort locking** on ⟨Ward Class, Gender, Infection Status⟩.
  3. **Heuristic constraint solver (verified: 4 hard constraints + 3 soft rules):** Process
     **Tan Ah Meng (`Q-P101`)**'s request against hard invariants (ward class, gender, telemetry,
     infection isolation) and soft scoring (Specialty +40, Consolidation +30, Fall-risk proximity +15).
     Show the **Top-3 recommended beds** with rationale.
  4. **Batch holding ward:** The male B2 non-infectious surge cluster (**Tan Ah Meng + Goh Beng Kiat +
     Teo Hock Seng**) triggers the **Ward 8B** batch holding-room recommendation (4 unlocked beds).
  5. **Dynamic cohort-swap:** **Ward 9B** is blocked by a single Green reservation — **Mr David Koh
     (`Q-P115`, 9B-01)**, who hasn't left the ED. The solver surfaces a **Cohort-Swap Suggestion**:
     move Koh to **8A-03**, resetting Ward 9B to all-`White` for a batch surge cohort. 1-click approval.

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
  * **Early caregiver readiness checklist** *(Story 4.1.2)* — at D-2/D-3, nurses work a structured
    checklist (insulin/wound training, home-equipment setup, transport booking) that flips to
    *"Ready for midday discharge"* only when complete. Message: *"the human bottleneck — unprepared
    caregivers — is retired days early, not on the discharge morning."*
  * **Closed-loop BMU bed release** — terminal-cleaning sign-off doesn't just flip the bed to
    `White`; it **auto-notifies BMU** that the bed is allocatable, closing the ED→ward→turnover→ED
    loop. Message: *"the recovered bed re-enters the allocation engine automatically — no phone call
    to tell BMU it's ready."*

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

3. **Comprehensive Runtime:** Keep a single, linear 10–12 minute full-depth walkthrough. The purpose is
   to *fully explain* the concept, features, and usefulness — not to optimize completion rate. Timeline
   **chapter markers** (additive) will be published for navigation.

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
