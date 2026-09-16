# Video Production Plan: Enhancing Hospital Admissions

## Patient Admission & Discharge Management Application

---

## 1. Executive Summary & Strategic Intent

This video is an authoritative, high-impact product demonstration film (~10–12 minutes) designed to sell the core intents of the **Patient Admission & Discharge Management Application**.

Rather than presenting abstract slide-ware or technical jargon, the film proves how the platform solves systemic hospital bed crunches through **intelligent clinical and operational orchestration**, eliminating inter-departmental phone tag, recovering "ghost" bed capacity without new physical construction, and restoring empathy and transparency to patients and families.

---

## 2. Target Audience & Stakeholder Value Alignment

The video speaks to a **Dual Constituency**—uniting operational decision-makers with clinical frontline leadership:

| Stakeholder Persona | Core Motivations & Pain Points | What This Video Proves |
| :--- | :--- | :--- |
| **Hospital Operations Leadership**<br>*(COO, CMIO, Director of BMU & Nursing)* | • Chronic ED boarding times & ambulance diverts<br>• High inpatient bed occupancy rates & gridlocks<br>• Nurse burnout and inter-departmental friction<br>• Lack of real-time operational capacity data | • **Recovers hidden bed capacity** via automated cubicle cohort packing & dynamic swaps without adding physical real estate.<br>• **Reduces ED boarding duration** via direct digital handoffs.<br>• **Enforces operational governance** with 18 auditable KPIs. |
| **Clinical Frontline Leadership**<br>*(ED Attending Chief, Specialist Chiefs, Nurse Managers)* | • Endless phone calls to BMU and specialists<br>• Contentious specialist consult handoffs & delays<br>• Anxious, agitated families verbally abusing triage nurses<br>• Afternoon discharge medication exit blocks | • **"Zero Phone Calls" workflow** from diagnostic synthesis to bed placement.<br>• **Clinical autonomy preserved**: 1-click reviews with peer discordance escalation.<br>• **Nurses shielded from queue friction** via public milestone tracker. |

---

## 3. Narrative Architecture: The Tri-Hybrid Arc

The film synthesizes three storytelling frameworks into a seamless narrative:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   THE TRI-HYBRID ARC                                   │
├──────────────────────────┬─────────────────────────────┬───────────────────────────────┤
│    A. Patient Tracer     │    B. Before vs. After      │      C. Pain Point Matrix     │
│         Journey          │          Contrast           │       Systematic Proof        │
├──────────────────────────┼─────────────────────────────┼───────────────────────────────┤
│ Follows realistic cases  │ Visceral comparison between │ Directly proves solutions for │
│ (Mr. Tan & Mdm. Halimah) │ the broken manual status    │ all 6 documented systemic     │
│ step-by-step through the │ quo and the orchestrated    │ root causes from              │
│ operational pipeline.    │ digital reality.            │ pain-points.md.               │
└──────────────────────────┴─────────────────────────────┴───────────────────────────────┘
```

### The Two Intersecting Clinical Scenarios

1. **Primary Anchor Case — Mr. Tan (68M, Acute Inpatient Journey):**
   * *Presentation:* Acute exacerbation of congestive heart failure, fluid overload, severe dyspnea, requiring Class B2 bed and continuous telemetry.
   * *Operational Journey:* Diagnostic synthesis $\rightarrow$ ED Attending assessment $\rightarrow$ Cardiology specialist broadcast with **Acuity Discordance** (ED Tier 3 vs Specialist Tier 2 $\rightarrow$ system auto-escalates to Tier 2) $\rightarrow$ Direct digital BMU queue entry $\rightarrow$ Multi-bed cubicle cohort packing with **Dynamic Cohort-Swap** $\rightarrow$ Family milestone tracking $\rightarrow$ D-2 discharge runway $\rightarrow$ Morning physician sign-off $\rightarrow$ Auto-queued bedside meds $\rightarrow$ 30-minute housekeeping bed turnover.
2. **Contrast Diversion Vignette — Mdm. Halimah (72F, Alternative Pathway):**
   * *Presentation:* Mild, stable cellulitis/pneumonia suitable for subacute care.
   * *Operational Journey:* ED intake $\rightarrow$ Deterministic Care-Pathway Matcher identifies subacute eligibility $\rightarrow$ BMU 1-click referral to **MIC@Home (Hospital-at-Home)** and Sister Community Hospital (OCH) $\rightarrow$ Mobile Financial & Care Advisory (FYI Insights) sent to family $\rightarrow$ Acute inpatient bed preserved.

---

## 4. Production Style & Visual Directives

* **Visual Format:** Hybrid Founder-Led Direct Address + High-Fidelity React UI Screen Capture.
  * **Direct Address / PIP (Founder):** The system creator appears on camera for the introduction, transitions between departments, and closing synthesis—establishing authentic personal conviction rooted in navigating Singapore's public hospital admissions and referencing national investigative discourse (CNA Talking Point).
  * **High-Fidelity UI Footage:** 4K 60fps captures of the live web application (`/ed`, `/specialist`, `/bmu`, `/bmu-config`, `/patient`, `/ward`, `/analytics`).
  * **Dynamic Camera Work:** Smooth software zooms into UI micro-interactions (e.g., dropdown chip overrides, state machine badge transitions, discordance alerts).
  * **Kinetic Data Overlays:** Subtle floating metric badges displaying real-time operational gains (e.g., `Phone Calls: 0`, `Acuity: Tier 2 (Escalated)`, `Bed Hours Recovered: +4.2 hrs`).
  * **Sound Design & Music:** Clean, modern cinematic tech pulse for operational walkthroughs, pausing/softening during personal narrative moments.

---

## 5. Master Beat Sheet & Scene-by-Scene Script (10–12 Minutes)

```
00:00        01:15        03:30        05:00        07:30        09:00        10:30        12:00
  │            │            │            │            │            │            │            │
  ▼            ▼            ▼            ▼            ▼            ▼            ▼            ▼
[Act 1: Hook] [Intent 1&2] [Intent 3]   [Intent 4]   [Intent 5]   [Intent 6]   [Act 3: KPIs] [Conclusion]
Origin & Pain  ED & Consult Diversion    BMU Solver & Patient      Ward Exit    Observability Call to Action
Status Quo    Discordance   MIC@Home     Cohort-Swap  Tracker      Block Meds   Control Tower Live Prototype
```

---

### Act 1: The Origin & The Broken Status Quo (0:00 – 1:15)

* **Visual:** Founder on camera in professional setting. Brief b-roll cuts of hospital emergency signage and animated workflow diagram showing fragmented communication links.
* **Presenter Voiceover / Dialogue:**
  > "If you've ever accompanied an elderly parent to a public hospital emergency department, you know the quiet despair of waiting eight, ten, or fourteen hours for an inpatient bed. You watch doctors running between triage bays, nurses answering endless phone calls, and families demanding answers that staff simply don't have.
  >
  > When CNA's Talking Point documented this persistent crisis, they confirmed what healthcare leaders already know: our bed crunches are not just an infrastructure deficit. You cannot build your way out of a flow bottleneck. The crisis is an **orchestration failure**—manual phone tag between doctors, multi-bed wards blocked by gender locks, and discharge gridlocks that trap beds well into the late afternoon.
  >
  > We built the Patient Admission & Discharge Management Application to solve these root causes. Let me walk you through how it works."
* **On-Screen Graphic:** Title Card: *Patient Admission & Discharge Management Application*.

---

### Beat 1 (Pain Points 1 & 2): Instant Diagnostic Synthesis, Specialist Broadcast & Direct BMU Queue (1:15 – 3:30)

* **Route / UI View:** `/ed` (Awaiting Assessment List $\rightarrow$ Assessment Modal) and `/specialist` (Broadcast Feed).
* **The "Before" Contrast Graphic:** Animation of an ED attending paging a cardiology registrar, waiting 45 minutes for a callback, and writing fragmented paper notes.
* **Live System Walkthrough:**
  1. **Awaiting Assessment Queue:** Mr. Tan arrives. The attending opens his admission dossier.
  2. **Automated Diagnostic Synthesis:** Highlight pre-populated clinical parameters—CT scan impression, troponin labs, vitals, and oxygen requirements. The system pre-suggests *Tier 3: Acute Stable*, Cardiology service, and telemetry requirements.
  3. **Attending 1-Click Confirmation:** The attending confirms the baseline with 1 click, adding a clinical directive.
  4. **Specialist Broadcast Pool:** Transition to on-call Cardiology feed on `/specialist`. Dr. Lim claims Mr. Tan’s broadcast.
  5. **Safety-First Discordance Engine:** Dr. Lim notes dynamic EKG changes and upgrades acuity: *Tier 2: Acute Urgent* with continuous telemetry.
  6. **Reconciliation & Consensus Gate:** Show both clinical judgments side-by-side. Explain the architectural invariant: *the software automatically adopts the higher acuity tier and unions telemetry constraints to guarantee patient safety without delaying bed request dispatch*.
  7. **Direct Digital BMU Dispatch (Zero Phone Calls):** With consensus reached, the structured packet dispatches directly to the BMU queue. Zero phone calls placed.
* **On-Screen Floating Badges:**
  * `Diagnostic Deliberation: -65% Latency`
  * `Acuity Resolution: Tier 2 (Safety-First Escalation)`
  * `Telephone Calls: 0`

---

### Beat 2 (Pain Point 3): Deterministic Care Diversion — Sister Hospitals & MIC@Home (3:30 – 5:00)

* **Route / UI View:** `/ed` $\rightarrow$ `/bmu` (Diversion Evaluation) and `/patient` (Financial Explainer Card).
* **The "Before" Contrast Graphic:** Clinicians defaulting to tertiary acute beds because community hospital or virtual ward referrals require 15-page manual referral packets and bilateral doctor negotiations.
* **Live System Walkthrough:**
  1. **Enter Mdm. Halimah:** 72F presenting with mild, stable cellulitis.
  2. **Care-Pathway Matcher:** Show the system automatically evaluating clinical inclusion rules upon intake. It flags Mdm. Halimah as eligible for **Hospital-at-Home (MIC@Home)** or step-down rehabilitation at **Sister Community Hospital (OCH)**.
  3. **BMU Operational Routing Authority:** The BMU coordinator reviews the recommendation with 1-click approval, initiating the digital transfer packet within a 30-minute bilateral SLA.
  4. **In-App Financial & Care Advisory (FYI Insights):** Show the mobile view sent to Mdm. Halimah's family: estimated daily co-pay ranges, MediSave subsidy percentages, and care expectations. Emphasize that it provides peace of mind without bureaucratic sign-off delays.
  5. **Capacity Saved:** An acute inpatient bed is completely preserved for higher-acuity trauma or surgical cases.
* **On-Screen Floating Badges:**
  * `Acute Inpatient Bed Preserved: 1`
  * `Transfer Packet SLA: < 30 Mins`
  * `Family Advisory: Instant Push`

---

### Beat 3 (Pain Point 4): Solving "Ghost Capacity" via Bed State Machine & Dynamic Cohort-Swaps (5:00 – 7:30)

* **Route / UI View:** `/bmu` (Bed Capacity Grid & Recommendation Drawer) and `/bmu-config`.
* **The "Before" Contrast Graphic:** Visualizing a 6-bed Class B2 cubicle with 5 empty beds that *cannot be used* because one female patient with MRSA was placed there, locking the room.
* **Live System Walkthrough:**
  1. **The 4-State Bed Lifecycle:** Walk through the live color-coded bed map:
     * `Mustard Yellow`: Vacated, empty, pending 30-min housekeeping sanitization.
     * `White`: Clean, empty, and ready for allocation.
     * `Green`: Assigned/in-transit (patient on the way).
     * `Grey`: Physically occupied.
  2. **Multi-Bed Cubicle Cohort Locking:** Show how cubicles enforce $\langle \text{Ward Class}, \text{Gender}, \text{Infection Status} \rangle$.
  3. **Heuristic Constraint Solver in Action:** Watch Mr. Tan’s bed request get processed against hard invariants (telemetry, gender, isolation) and soft scoring rules. The Top 3 recommended beds appear with human-in-the-loop rationale.
  4. **The Dynamic Cohort-Swap Feature:**
     * Demonstrate an empty "Flex Ward" that was blocked by a single isolated `Green` patient who hasn't left the ED yet.
     * The solver surfaces a **Cohort-Swap Recommendation**: reassigning the single `Green` patient to an equivalent bed in an already cohort-locked room.
     * With 1-click coordinator approval, the Flex Ward resets to all-`White`, freeing up an entire 6-bed cubicle to batch-admit an incoming surge cohort.
* **On-Screen Floating Badges:**
  * `Solver Evaluation: < 50ms`
  * `Ghost Capacity Recovered: 5 Bed-Days`
  * `Cohort Lock Reset: Ward 8B Cleaned`

---

### Beat 4 (Pain Point 5): Patient & Family Milestone & Queue Transparency (7:30 – 9:00)

* **Route / UI View:** `/patient` (Mobile view with token-based access).
* **The "Before" Contrast Graphic:** Anxious family members crowding the ED nurse counter every 20 minutes asking, *"Why has my father been waiting 7 hours when people who came after him got a bed?"*
* **Live System Walkthrough:**
  1. **Activation via Secure Token:** Show Mr. Tan's daughter opening the tracking link on her smartphone. No passwords or app downloads required.
  2. **3-Stage Milestone Stepper:**
     * Stage 1: Admission Confirmed & Bed Requested.
     * Stage 2: Bed Assigned & Room Sanitizing.
     * Stage 3: Admitted to Inpatient Ward Bed.
     * *Highlight domain detail: Held quiescent during ED clinical deliberation to prevent false alarms.*
  3. **Queue Transparency by Ward Class:** Clearly shows queue position partitioned by Ward Class (Class B2), managing expectations without disclosing clinical PII.
  4. **Empathetic Operational Delay Tags:** Demonstrate a simulated delay. Instead of silence, the tracker shows: *"ED is actively prioritizing emergency trauma resuscitations; clinical teams are continuously monitoring your condition."*
  5. **Impact on Nurses:** Triage nurses can focus 100% on clinical stabilization instead of crowd control.
* **On-Screen Floating Badges:**
  * `Periodic Status Updates: Every 2 Hours`
  * `Triage Nurse Complaints: -70%`
  * `Patient Visibility: 100% Transparent`

---

### Beat 5 (Pain Point 6): Multi-Day Discharge Runway & Auto-Queued Bedside Meds (9:00 – 10:30)

* **Route / UI View:** `/ward` (Inpatient Runway & Bed Turnover).
* **The "Before" Contrast Graphic:** The afternoon discharge gridlock—patients waiting until 3 PM for pharmacy medications, while ED patients downstairs wait in hallways for those same beds.
* **Live System Walkthrough:**
  1. **Stage 1 (D-2 / D-3 Discharge Runway):** Mr. Tan's inpatient stay progresses. The ward dashboard displays the Estimated Date of Discharge (EDD) with confidence indicators. Ward nurses engage caregivers early for home insulin and mobility prep.
  2. **Stage 2 (Day-of-Discharge Morning Sign-Off):** During morning rounds at 09:00 AM, the physician clicks the Morning Discharge Sign-Off.
  3. **Auto-Queued Bedside Medication:** The sign-off automatically queues discharge medications in the pharmacy. Runners bring meds directly up to the bedside before 11:00 AM, eliminating the separate pharmacy queue.
  4. **The 30-Minute Housekeeping SLA:** When Mr. Tan departs, the bed turns `Mustard Yellow`. The system dispatches environmental services with an enforced 30-minute terminal cleaning countdown, returning the bed to `White` for immediate BMU re-allocation.
* **On-Screen Floating Badges:**
  * `Discharge Before 12 PM: +45%`
  * `Medication Exit Block: Eliminated`
  * `Housekeeping SLA: 30-Minute Turnover`

---

### Act 3: Operational Control Tower, System Synthesis & Call to Action (10:30 – 12:00)

* **Route / UI View:** `/analytics` (Dual-Pathway Observability Dashboard) and live demo link.
* **Systemic KPI Rollup:**
  * Review the 18 operational metrics: ED assessment turnaround, specialist concordance, bed capacity utilization gain, ghost bed reduction, and discharge before midday.
  * Founder addresses the viewer on camera:
* **Closing Monologue & Call to Action:**
  > "Hospital bed capacity is not fixed. When clinical decisions are synthesized in real-time, when bed constraints are solved mathematically rather than over the phone, and when discharge logistics are pulled forward into morning rounds—we recover bed capacity that was there all along.
  >
  > We invite hospital leaders, clinical chiefs, and healthcare innovators to explore the interactive live prototype deployed on Render. Test the clinical flows, inspect the constraint engine, and see how intelligent orchestration can transform hospital admissions."
* **Closing Screen:**
  * Live Prototype URL: `https://enhance-hospital-admissions.onrender.com`
  * QR Code linking directly to the demo.
  * Project Repository & Documentation references.

---

## 6. Documented Assumptions & Decision Path Log

1. **Dual Constituency Target:** Decided to pitch jointly to **Hospital Operations C-Suite (COO/CMIO)** and **Clinical Frontline Chiefs (ED/Ward Nursing)** to create both executive budget motivation and grassroots clinician buy-in.
2. **Tri-Hybrid Narrative:** Combined the **Patient Tracer Journey** (emotional/clinical reality), the **Before vs. After Contrast** (visceral problem setup), and the **Pain Point Matrix** (systematic proof of all 6 root causes).
3. **Comprehensive Runtime:** Selected an unconstrained 10–12 minute full-depth product film, giving adequate time to showcase clinical nuance (specialist discordance), mathematical solver logic (cubicle cohort-swapping), and inpatient turnover.
4. **Two-Patient Persona Model:** Resolved the clinical contradiction of showing both acute inpatient admission and alternative care diversion by pairing **Mr. Tan** (acute pathway) with **Mdm. Halimah** (MIC@Home/Sister Hospital diversion vignette).
5. **Production Aesthetic:** Blended **Founder-Led Direct Address** (authentic personal conviction) with **High-Fidelity React UI Screencasts** (zooms, kinetic overlays, and real-time KPI tickers).
6. **Elimination of IT Jargon:** Omitted legacy EHR integration discussions (FHIR, HL7, enterprise procurement cycles) to keep the narrative 100% focused on product intents, clinical workflows, and patient capacity gains.
