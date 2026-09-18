# Video Production Plan: Build the Foundations, Stub the Rest

## Technical design decisions and trade-offs — Patient Admission & Discharge Management Application

> **Sibling artifact, not a sequel.** [`narration-video-plan-product.md`](narration-video-plan-product.md) plans the
> *product* film (`video-generation-product/`), which sells what the system does to a health
> executive. This plan is for a second, independent film and deck (`video-generation-technical/`)
> that sells **how the system was decided** to a technical hiring audience. The two share a pipeline,
> a house style and a seed dataset. They share no beats, and this one does **not** re-sell the
> product.

> **Status:** plan approved through Decision D13; authoring added D17–D19. `storyline.yml`,
> `storyboard.json`, `pages/seam.html` and the six persona setup scripts are written; 13 slides render
> clean at 1920×1080 with PDF + HTML exports. **Stopped at the outline gate** — no TTS, no capture, no
> compose. Eight `.tape` files and the runbook prerequisites are the next step.

---

## 1. Executive Summary & Strategic Intent

**One conclusion the viewer must reach:** *this person makes defensible trade-offs under constraint,
and can name what they rejected and why.*

Not "this person ships fast" (delivery throughput is indistinguishable from having had more time),
not "this person knows healthcare" (irrelevant to most panels), and not merely "this system is
well-architected" (a property of the artifact, whereas panels hire the person). Engineering rigour —
profile boundaries, gateway seams, safety invariants, provable observability — appears throughout,
but strictly as **evidence for the judgment claim**, never as a claim of its own.

The reason for that choice is that at principal level, coding ability is assumed by the time anyone
is watching a video. The scarce, hard-to-fake signal is **decision quality under incomplete
information**, and the only observable proxy for it is the counterfactual: which more-impressive
option was declined, and can the candidate price the trade?

It is also the defensive choice. The dominant risk with this audience is the reviewer who concludes
*"impressive-looking prototype, but it is demoware."* A film built on polish invites that read. A
film whose entire spine is the author naming his own limits — H2 not Postgres, heuristic not
Timefold, polling not STOMP, header auth not OIDC, tests not CI — pre-empts it.

**Title:** *Build the Foundations, Stub the Rest* — chosen to be quotable, on the theory that the
ideal outcome is a panel member repeating the rule to a colleague. Because "foundations" does not
carry its own criterion the way "irreversible" does, the answer-first slide must **define** it:
*foundations are the parts you cannot unwind later.*

---

## 2. Target Audience & Value Alignment

| | |
| --- | --- |
| **Primary audience** | Hiring managers and engineering managers assessing a **Principal / Staff+ software engineering** candidate. Assumed technical enough to know that "heuristic over Timefold" is a real decision and not laziness. |
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

> **Investment was allocated by reversibility.**
> Decisions that are expensive to unwind — safety invariants, state machines, the constraint
> hierarchy, the audit contract, the test suite — were built and tested to production standard.
> Decisions that are cheap to unwind *because an interface was cut first* — EHR, sister hospital,
> solver, auth, transport, persistence, CI wiring — were stubbed.
> **The seam is the deliverable; the adapter behind it is scheduling.**

This rule does three jobs at once, which is why it earns the answer slide: it explains what was
built, it explains what was *not* built without apologising, and it is falsifiable on screen — the
interface, the mock and the fail-fast stub can be shown behaving differently under two profiles.

### 3.2 The unit of the spine is a decision, not a feature

A feature is a claim about the product; a decision is a claim about the author. Feature-per-beat was
rejected on two grounds. Structurally it forces the narration into *"here is what it does, and here
is how I built it,"* which demotes the trade-off to a footnote. Arithmetically, the 28 atomic issues
in `.scratch/` at even 20 seconds each is 9m30s of list with zero room for argument.

Features still appear — as the *setting* in which a decision was exercised, and as the evidence that
it was exercised at all.

**Hard rule: no decision beat without a live artifact behind it.** A decision with no on-screen
artifact does not get a beat; it gets a clause.

### 3.3 SCR, with Situation and Complication relocated

Archetype 7 (`archetype: scr`), declared so the compiler enforces it: `situation` 1–2,
`complication` 1–2, `resolution` 2–5 (`.kiro/skills/video-generator/scripts/lib/deck.mjs:89-92`,
validated at `:195-215`).

The enforcement is wanted, not tolerated. The dominant failure mode of a technical portfolio film is
not shallowness — it is being **unbounded**: every decision feels load-bearing to its author, so with
no cap the film reaches 20 minutes and a viewer who stops at 4:00 has seen the two least interesting
decisions. The cap forces the question *"which five arguments earn the viewer's time"* — the same
editorial judgment the panel is assessing.

In the product film, Situation and Complication belong to the hospital. Reusing that here would
re-sell the product, which is forbidden. So both are **relocated to the engineering problem**:

- **Situation** — a prototype's job is to de-risk architecture, and this domain is hostile to
  prototyping.
- **Complication** — therefore both default moves fail, *and* the domain refuses to let the important
  part be faked.
- **Resolution** — the reversibility rule, applied five times.

### 3.4 The loophole we agreed not to use

`SCR_SKELETON` binds only entries carrying `role:`. Unroled slides are uncapped — which is how the
product film legally holds `00-answer`, `00b-resolution-map` and `13-cta`.

**Unroled slides are reserved here for exactly four jobs: the answer-first slide, the decision-ledger
map, the close, and the CTA.** Running eight mechanism beats with five roled and three unroled would
compile cleanly and would be worse than opting out of the archetype entirely — it is beat-sprawl
wearing the costume of discipline.

### 3.5 Runtime and shape

| | |
| --- | --- |
| Target | **~9:00–10:30**, hard ceiling **15:00** |
| Discipline | **the beat count, not the minutes.** Spare budget buys depth *inside* five beats — more evidence takes, longer inelastic terminal takes — never a sixth argument |
| Aspect | **Landscape only.** A 9:16 reframe of a nine-minute trade-off argument is the worst of both formats (vertical norms are <90s). A short cut, if ever wanted, is a *separate* storyline — `--aspect=vertical` reframes, it never shortens |
| Media | **Deck and film co-primary.** `formats: [png, pdf, html]`; two `in: [deck]` slides open answer-first while the film opens on the Situation |
| Register | **Crisp throughout.** No warmth on the bookends — a deliberate divergence from the product film's warm/analytical split. This audience reads emotional framing inside a technical argument as padding, and worse, as evidence the author cannot tell the two apart. The only human stakes admitted are in C2, and only as a *correctness constraint* |

---

## 4. Production Style & Visual Directives

- **Engine `html`** (bento templates), **`theme: light`** — matches the app UI so cuts to dashboards
  do not flash, and matches the product deck so the two read as one body of work.
- **No bullets.** Content is cards in one of eight layouts; card text is one or two sentences,
  enforced by the overflow invariant.
- **Slide layouts are assigned per beat and then held** (see §5). Rigidity is deliberate: once the
  viewer stops decoding layout, attention goes to content. Variation happens in *which of the five
  narration parts leads*, never in the furniture.
- **Voice:** Kokoro (local, Apache-2.0), **female** — the same narrator as the product film, so both
  artifacts read as one body of work when watched back to back.
- **Subtitles:** burned ASS with explicit `PlayResX/PlayResY`, Arial, white on opaque `#0F172A`.
- **No generated footage. No b-roll. No music.** Atmosphere adds nothing to a trade-off argument and
  costs credibility.
- **No code on screen** — see D6.

---

## 5. Master Beat Sheet

> **Source of truth for narration:** once authored, the verbatim lines live **only** in
> `video-generation-technical/storyline.yml`, one `narration:` per entry, compiled to
> `storyboard.json`. Lines below are **drafting intent for review** — when the two disagree,
> `storyline.yml` wins. This section's durable value is the beat structure, the evidence manifest,
> the routes and selectors, and the grounding references.

```
00:00        00:45        01:30        03:00        04:30        06:00        07:45      ~09:30
  │            │            │            │            │            │            │          │
  ▼            ▼            ▼            ▼            ▼            ▼            ▼          ▼
[S1 S2]      [C1 C2]      [R1]         [R2]         [R3]         [R4 R5]      [Ledger]  [CTA]
purpose +    both moves   the seam     heuristic    safety vs    locking +    what is   "trade
the domain   fail; the    is the       over         operations   provable     owed +    differently"
as a model   domain       deliverable  Timefold                              method
             won't fake
```

### 5.0 Unroled openers (deck only)

**`00-answer`** — `hero`, `in: [deck]`. The reversibility rule as the recommendation, with
"foundations" defined as *the parts you cannot unwind later*. An executive in a briefing cannot walk
out, so answer-first costs nothing; a film viewer can leave at any second, so the film opens on the
Situation and this slide never appears there.

**`00b-decision-ledger`** — `tiled`, `in: [deck]`. Names the whole decision surface **once** — 11
`.wayfinder` tickets, 10 architectural dimensions, 4 ADRs — so the five beats that follow are
understood as a *selection* from a documented set rather than as everything there was. Carries no
`role:` and is not a beat.

### 5.1 S1 — "A prototype's job is to de-risk what you cannot undo"

- **Layout:** `stat-deepdive`. **Role:** `situation`.
- **Job:** the purpose frame, stated before any domain content, because a viewer must know what they
  are watching before they can evaluate it — and because it pre-empts the wrong yardstick. This film
  is not asking to be judged as a product, and says so inside the first fifteen seconds.
- **Note:** the thesis proper is *not* here. It is on the deck-only answer slide, and it lands in the
  film at R1.
- **Draft intent:** a prototype is not a small product. It is an instrument for retiring risk, and
  the only risk worth retiring is the kind you cannot undo later.

### 5.2 S2 — "The domain, as an engineering object"

- **Layout:** `process-flow` (or `lifecycle`). **Role:** `situation`.
- **Content:** 5 entities under a strict `Level → Ward → Bed` hierarchy (`Ward`, `Bed`, `Patient`,
  `AdmissionRequest`, `AssessmentBroadcast`); the bed's **4 states** named as a machine
  (`EMPTY_CLEANED` / white → `EMPTY_ASSIGNED` / green → `OCCUPIED_TAKEN` / grey →
  `EMPTY_PENDING_CLEANING` / mustard yellow → white); **6 actor roles**; **3 external systems** the
  design depends on and cannot have.
- **Why a model and not a story:** the audience has no model of gender cohorting, ward classes or
  acuity tiers, and without *some* of it R3 is incomprehensible. Teaching the domain as a **data
  model** rather than as a patient journey costs less time, keeps the register technical, and is
  itself principal-signal (domain modelling as a competence). The same slide that supplies vocabulary
  also establishes the constraint surface that makes the later decisions non-trivial.
- **Evidence:** `/bmu` board with its bed-state legend (`bmu.tsx:1285-1297` prints state and colour),
  narrated as a state machine rather than as capacity relief. First appearance of the persona
  switcher, since "6 roles" is a claim the switcher makes visible.

### 5.3 C1 — "Both default moves fail"

- **Layout:** `comparison`. **Role:** `complication`.
- **The two moves:** mock everything and polish the UI → proves nothing, because *on video, polish
  and correctness are indistinguishable*. Build it all properly → does not finish.
- **Priced** (the compiler warns when a complication names no cost — `deck.mjs:212-215`): **3**
  integrations that cannot be obtained at any price by a personal project (no FHIR endpoint, no
  Singpass tenant, no Timefold licence), **1** engineer, **0** production credentials.
- **This is where the one-engineer constraint is stated**, because it is the load-bearing premise of
  the entire thesis: a team of eight builds the FHIR adapter, and the reversibility rule would never
  have been needed. See D9 for why AI leverage is named adjacent to this and nowhere near a
  resolution beat.

### 5.4 C2 — "This domain does not let you fake the part that matters"

- **Layout:** `stat-deepdive`. **Role:** `complication`.
- **Argument:** clinical safety rules are not optional in a demo. Mis-cohort two patients and the
  artifact is **wrong**, not merely unpolished — and a reviewer who knows the domain sees it
  instantly. So the usual prototype licence ("it's only a demo") does not extend to the part that
  matters.
- **Priced by construction**, reusing the product film's derived figure rather than inventing one: a
  Class B2 cubicle is 5–6 beds (`ed.tsx` ward-class options), gender cohorting is absolute, therefore
  the first patient constrains up to five more beds.
- **Why C2 is promoted to a complication rather than folded into R3:** it is what converts the film
  from *"look how carefully I mocked things"* into an argument about **where correctness is mandatory
  and where it is negotiable** — the exact axis the reversibility rule cuts along. Without C2, R3's
  investment in safety invariants looks like gold-plating. With it, R3 is forced.

### 5.5 Beat anatomy — applied identically to R1–R5

1. **Name the alternative a senior engineer would expect.**
2. **The constraint that made it unwise** — concrete and checkable (licence, absent endpoint, absent
   SIEM, one engineer).
3. **What shipped, in bound nouns** — mechanism, not adjectives.
4. **The price, stated out loud.** Non-negotiable, and surfaced as a `detail:` field so it is *on
   screen*, not merely spoken.
5. **The seam that makes the price recoverable.**

Part 4 carries the weight. Naming a rejected alternative only proves consideration. Naming your own
choice's cost proves you hold a model of the system's future — *"I am accepting a locally optimal
placement, it holds at this ward count, and here is the interface that buys the optimal version when
the ward count changes."* That is the hire.

**Self-imposed invariant:** every resolution beat names a cost of its own choice. The compiler checks
cost-words on `complication` only, so this one is **manual discipline** and must be verified by hand
at review.

**Anti-template measure:** slide furniture stays rigid; the leading narration part rotates — R2 opens
on the alternative, R3 on the invariant, R4 on the price, R5 on the mechanism, R1 on the constraint.

### 5.6 R1 — The seam is the deliverable

- **Layout:** `process-flow`. **Rejected alternative:** feature flags / `if (isDemo)` branching, or
  no seam at all.
- **Mechanism:** one profile boundary; **3 interfaces** (`BedAllocationSolver`,
  `HospitalEhrGateway`, `SisterHospitalGateway`); **6** `@Profile("prototype")` beans
  (`DataInitializer`, `PrototypeSecurityFilter`, `PrototypeSecurityConfig`,
  `HeuristicBedAllocationSolver`, `MockSisterHospitalGateway`, `MockHospitalEhrGateway`); **3**
  `@Profile("!prototype")` production adapters that exist to fail loudly.
- **Price:** two code paths mean the prototype path is the only one under test, and the production
  adapters are unexercised by definition. The boundary is a promise, not a proof.
- **Seam:** production work is *additive* — a new adapter behind an existing interface, each
  independently implementable without touching domain logic.
- **The persona switcher belongs here** (see D10): `Header.tsx` maps 6 personas to 6 seeded accounts
  (`dr_tan_ed`, `dr_lim_cardio`, `bmu_coord_wong`, `nurse_sarah`, `evs_staff_kumar`, `patient_p101`),
  writes `localStorage['admissions_role_persona']`, sends `X-User-Role`, invalidates every TanStack
  query and routes to that persona's page. Framed **not** as convenience but as a decision about the
  artifact's own **auditability** — a reviewer traverses five desks and four handoffs without five
  login flows, which is what makes the cross-persona workflow demonstrable at all. And the
  trade-off is already resolved in code: header-driven role assumption is a production security hole,
  so it lives behind `@Profile("prototype")` and `SecurityConfig` (`@Profile("!prototype")`) refuses
  it. **Convenience, deliberately fenced.**

| Take | Type | Proves |
| --- | --- | --- |
| The **seam diagram**, served on `:4173` and revealed progressively — one interface fanning out to a `@Profile("prototype")` mock and a fail-fast production adapter, three times over (D17) | Playwright | The shape of the boundary, before it is demonstrated behaving |
| `curl /actuator/health` → UP, then `curl /api/...` → **denied**, on a no-profile JVM (`:8081`) | VHS | The default profile is not a stub of a system; it is a *locked* system. The fence is real, and the app is up — the denial is authorization, not a crash |
| `./mvnw test -Dtest=GatewaysAndSolversTest` | VHS | The stubs' fail-fast is itself asserted |
| Persona switcher, ED → BMU → Ward, no login | Playwright | Reviewability-by-design, and the fence around it |

> **Grounding:** `SecurityConfig.java:20` is `@Profile("!prototype")` and permits only
> `/actuator/health`, `anyRequest().authenticated()`, CSRF on. `TimefoldBedAllocationSolver` throws
> `UnsupportedOperationException` **on invocation, not at startup** — so the fence demo is a
> *request*, never a crash-on-boot. Exact status code (403 vs 401) must be **observed at capture
> time**, not asserted in narration (see §8 open items).

### 5.7 R2 — Bed allocation by heuristic, not by constraint engine

- **Layout:** `comparison` (old first, accent second, so the flip reads downward).
- **Rejected alternative, named explicitly and respectfully:** **Timefold / OptaPlanner** — the
  textbook answer, understood and available, and *declined*. It is named as a tool that can be
  incorporated later, not as something unknown.
- **Constraint:** commercial licensing, a continuous solving daemon, and an explainability gap for a
  coordinator who must justify a placement.
- **Mechanism:** `HeuristicBedAllocationSolver` — **4 hard constraints** filtered first, then **3
  soft weights** scored (specialty alignment **40 points**, cohort consolidation **30**, fall-risk
  proximity **15** — the form's own defaults, rendered as `+40 pts`), two-phase pack-then-batch,
  **computed synchronously inside the request**.
- **Price:** a heuristic is not globally optimal and can be beaten on a large enough ward set;
  multi-hospital cluster optimisation is out of its reach.
- **Seam:** `BedAllocationSolver` is an interface and `TimefoldBedAllocationSolver` is already the
  default-profile bean. Swapping is configuration, not surgery.

| Take | Type | Proves |
| --- | --- | --- |
| `/bmu` → select request → Top-3 with `Score: +N` **and the breakdown lines** (`bmu.tsx:1152-1156`) | Playwright | **Explainability** — a coordinator can read *why* this bed ranked first. This is the trade-off's upside, not decoration |
| `/bmu/config` → change a weight → re-rank | Playwright | Policy lives in configuration, not in code |
| `./mvnw test -Dtest=HeuristicBedAllocationSolverTest` | VHS | The heuristic is pinned by tests, so replacing it is safe |

> **Narration constraint:** the `<50ms` figure in `docs/architecture.md`, `technical-architecture.md`
> and ticket 010 is **prose only — nothing measures it**, so the numeral is dropped (D8). The film
> claims what is architecturally true and visible instead: computed synchronously in the request, no
> solver daemon, no job queue, no polling for a result.

### 5.8 R3 — Safety is not overridable; operations are

- **Layout:** `tiled` (two tiers × their override semantics — not a binary).
- **Rejected alternative:** one flat rule set with an admin override; or "highest acuity wins" as a
  convention in a runbook rather than as code.
- **Mechanism:** **two-tier constraint hierarchy** (ADR-0010) — Tier 1 absolute safety invariants
  (biological gender cohorting, airborne isolation) have **no override affordance in the UI at all**;
  Tier 2 operational constraints (ward class, portable telemetry) permit override **only** with a
  structured institutional reason code. Plus the **safety-first discordance engine** (highest acuity
  wins, telemetry requirements unioned) and the **consensus completion gate** (ADR-0007/0008 — every
  chained consult resolves before BMU dispatch).
- **Price:** absolute invariants will occasionally block a placement a human knows is fine, and the
  system offers that human no escape hatch. That is the intended cost, and it is a real one.
- **Seam:** the tier assignment is data about a constraint, not a branch in a service — re-tiering is
  a policy change.

| Take | Type | Proves |
| --- | --- | --- |
| `HeuristicBedAllocationSolverTest` — display names read *"filters by WardClass, Gender, Telemetry and Infection constraints"*, *"eliminates multi-bed wards with occupants for infectious patients"*, *"eliminates wards without negative pressure for respiratory airborne isolation"* | VHS | Tier 1 is proved by **elimination**: a violating bed never becomes a recommendation. See D19 — there is no honest UI shot for this |
| An operational override → mandatory structured reason-code modal → allocate (`bmu.tsx:1197` fires for any non-rank-1 bed; modal at `:1791-1860`, default `GOVERNMENT_SUBSIDY_CLASS_UPGRADE`, confirm is *"Confirm Override & Allocate"*, description promises an immutable `OVERRIDE_ALLOCATION` audit record) | Playwright | Tier 2 is overridable but never silent |

### 5.9 R4 — Direct state mutation with optimistic locking

- **Layout:** `comparison`.
- **Rejected alternative:** Event Sourcing + CQRS + WebSocket/STOMP — documented as the production
  target in ticket 003 and **deliberately bypassed** in the prototype.
- **Constraint:** the prototype must be readable and runnable by one person on a laptop; event
  sourcing's cost lands entirely on comprehension and operations, not on features.
- **Mechanism:** direct in-place relational mutation; the **4-state bed lifecycle** with strict
  transition gates (`/ward` check-in → vacate → `MUSTARD YELLOW` + 30-minute cleaning SLA → sign-off
  → `WHITE`); `@Version` optimistic locking on exactly **3** entities (`AdmissionRequest:117`,
  `AssessmentBroadcast:26`, `Bed:52`); conflicts surface as **HTTP 409** RFC 7807 `ProblemDetail`.
- **Price stated out loud (this beat leads on the price):** we gave up the free audit trail, the
  temporal queries and the replayability that event sourcing hands you — in a domain where audit is a
  regulatory requirement. The structured MDC audit log (R5) is the compensating control, and it is
  strictly weaker than an event store.
- **Seam:** state transitions are already funnelled through service methods rather than scattered
  across controllers, so the write path is one place.

| Take | Type | Proves |
| --- | --- | --- |
| `/ward` vacate → mustard yellow (30m SLA active) → housekeeping sign-off → white | Playwright | The state machine as plain mutation, with gates |
| `ClinicianControllerTest#testClaimBroadcast_ConflictReturns409` (`:150-151`) and `GlobalExceptionHandlerTest#testOptimisticLockConflict` (`:155-159`) | VHS | Optimistic locking proven where the UI cannot honestly stage it |

> **Why the UI does not prove the 409 (D11):** a genuine concurrent claim needs two browser contexts
> racing; `capture.mjs` drives a single page, and a faked race is a lie on screen. So the test *is*
> the evidence. This makes R4 the most test-heavy and least visual beat, which is exactly why it sits
> fourth — appetite for a quieter beat is highest there — and why it holds only 2 takes.

### 5.10 R5 — Correctness had to be provable

- **Layout:** `process-flow` or `stat-deepdive`.
- **Rejected alternative:** trusting a single extraction pathway; or offering coverage as the sole
  quality claim.
- **Mechanism:** **dual-pathway observability** — every operational KPI derivable *both* from
  relational state (JPA/SQL, for BI) *and* from structured SLF4J/MDC audit logs (for SIEM/real-time
  alerting), with mathematical parity between them. `HospitalKpiSummaryDto` carries **20 scalar
  metrics across 4 epics + 2 categorical breakdown maps**.
- **Price:** two pathways is two things to keep in step, and parity is only as good as the test that
  asserts it. Coverage is **measured, not enforced** — see the ledger.
- **Seam:** the log *format* is profile-independent; only the destination changes (stdout today,
  SIEM + WORM later).

| Take | Type | Proves |
| --- | --- | --- |
| `/analytics` — 4 epic sections, 20 metrics, benchmark evaluations | Playwright | Pathway A (JPA/SQL) |
| `tail logs/app.log` — real structured audit lines with MDC context (`activeUser` in the console pattern, `application.yml:17`) | VHS | The **log format** that makes pathway B possible — the raw material, not the tooling |
| `./mvnw test -Dtest=PathBDualPathwayReconciliationIntegrationTest` | VHS | **Parity is asserted, not eyeballed** |
| JaCoCo + Vitest coverage reports (absolute URLs on `:4173`) | Playwright | The measured number, whatever it turns out to be |

> **`scripts/` never appears on screen** (D12, user instruction). The KPI extraction CLI is therefore
> not shown, and pathway B is proven by the reconciliation *test* plus the raw log format. This is
> arguably stronger: a test that fails when the two pathways disagree beats a shell script whose
> arithmetic a viewer cannot check.

### 5.11 Close — the ledger of what is owed (unroled)

- **Layout:** `tiled`. Title borrows the runner-up film title: *"Eleven decisions and what they
  cost."*
- **Every debt paired with the seam already cut for it:**

| Debt | Seam already in place |
| --- | --- |
| H2 `create-drop` — a restart loses everything | PostgreSQL driver present; JPA mappings unchanged by the swap |
| Polling at 3s will not survive N clients | Ticket 003 documents the STOMP target; the UI already treats state as remote |
| The heuristic is not globally optimal | `BedAllocationSolver` + Timefold bean already wired to the default profile |
| Header-driven personas are **not authentication** | `SecurityConfig` already denies by default; OAuth2/JWT + Singpass is ticket 009 |
| Logs go to stdout, not a SIEM | The MDC format is identical; only the destination changes |
| **Coverage is measured, not enforced — there is no CI** | The suite is the foundation; `jacoco:check` and a workflow are the trivially-added adapter |
| No FHIR, no sister-hospital HTTP, no SMS | Three gateway interfaces with fail-fast production beans |

- **Why the ledger and not a stat wall:** the panel's unspoken question throughout is *"does he know
  what is missing, or does he think this is done?"* The ledger answers it before it can be asked. The
  risk is deflation — ending on absence can read as incompleteness — and the mitigation is that every
  line is paired with its seam, so the register is *a roadmap under control*, not a confession.
- **Method disclosure lives here** (D9), in the author's own division of labour: **AI writes the
  code; the trade-offs, the technical approach, the final shape of the code and the design
  considerations are the author's.** Stated with specificity, because a vague "I used AI" invites the
  ownership question while a precise division of labour answers it pre-emptively.
- **Take:** `ls .wayfinder/tickets` / `ls .scratch/*/issues` / `ls specs` → **11 / 28 / 5** (+ 4
  ADRs), VHS. Proves specs *preceded* code — the exact thing a skeptic doubts — and it is countable
  on screen rather than asserted. Chooses transparency about the agent tooling over polish.

### 5.12 CTA (unroled)

- **Layout:** `hero`.
- **The line:** *every one of these five was a trade, and you have been told what each one cost — if
  you would have traded differently, that is the conversation I want.*
- **Three cards:** the **README** as the map; `docs/adr/` + `.wayfinder/tickets/` as the reasoning;
  the **live Render demo** with its cold-start caveat.
- **Why an invitation to interrogate:** after nine minutes of naming five prices and a ledger of
  debts, the only close that does not undercut the argument is one that invites the counterargument.
  It is a status move that has been *earned* rather than claimed, and it converts passive viewing
  into the exact conversation the interview should be about. Tone must stay plain — no "prove me
  wrong", no "I'd love your thoughts".

---

## 6. Decision Log

Each entry records what was chosen, what was rejected, and the reasoning — in the order decided.

**D1 — The conclusion is judgment, not delivery, domain or rigour.**
*Rejected:* "ships fast" (indistinguishable from having had more time); "knows healthcare"
(irrelevant to the panel); "well-architected system" (a property of the artifact, but panels hire
people). Rigour is retained as the *proof mechanism*. Cost accepted: a less exciting watch than a
capability reel, with no "wow" moment; underperforms if sent cold to non-technical recruiters.

**D2 — Timefold is named as available and declined, not as unknown.**
Explicit user instruction. The film credits the tool and states it can be incorporated later, which
is what distinguishes a decision from an omission.

**D3 — The unit of the spine is a decision; features are the setting.**
*Rejected:* feature-per-beat (28 issues × 20s = 9m30s of list with no argument; forces trade-offs
into footnotes). *Adopted alongside:* `docs/architecture.md`'s 10-dimension table as the **source
inventory** for the reversible half of the spine. Cost accepted: harder to author, since each beat
needs a named rejected alternative and a price, neither of which can be lifted verbatim from docs
that record rationale but not the rejected option's cost.

**D4 — Investment allocated by reversibility is the organising principle.**
*Rejected:* "safety invariants first" (does not explain the stubs) and chronological "how the design
evolved" (a diary, not an argument). The rule simultaneously explains what was built, excuses nothing,
and is falsifiable on screen.

**D5 — `archetype: scr`, enforced; 12 candidate decisions grouped into 5 resolution beats.**
*Rejected:* a free-text archetype with ~12 uncapped beats (nothing stops beat 9 from being as
prominent as beat 1, and a flat list has no argument); and the unroled-slide loophole (compiles
cleanly, defeats the discipline it appears to honour). Grouping does **not** drop decisions — a
mechanism slide carries 3–4 cards, each a named decision, and narration may name a decision in a
clause without granting it a beat. Ordering is **deductive** (R1's meta-decision first), because the
deck's answer slide states the rule anyway and an inductive film would contradict the deck.

**D6 — Evidence tiers 1–4 accepted; tier 5, code on screen, refused.**
Ranked by falsifiability: (1) executable terminal, (2) generated reports, (3) the running app as
decision evidence, (4) one diagram. *Refused:* code screenshots — a viewer cannot compile them, see
their callers, or tell whether that file is the one that runs, so a code shot proves nothing a slide
could not also assert. **Behaviour under two configurations is strictly stronger than source under
one.** Counter-argument acknowledged: some panels want to see code, and a code-free film could read
as evasive to a reviewer suspecting AI generation; mitigated by pointing at code-derived artifacts
(javadoc/tsdoc, coverage) and by sending viewers to the repo. Consequence: **no manual IDE footage,
no `footage/` directory at all.**

**D7 — E2E and Allure are out of scope for this film.**
User instruction. `README.md` states E2E is deferred while `e2e/` and `artifacts/e2e/allure-report`
exist with two dated campaigns; both cannot be true on screen, and the film simply never goes there.

**D8 — No number on screen unless the same shot produces it.**
Numbers come from artifacts (surefire summaries, JaCoCo percentages, the dashboard, dropdowns), never
from a slide asserting them. Slides may carry only **structural cardinalities countable in the
adjacent capture**: 4 hard constraints, 3 soft rules, 4 bed states, 2 tiers, 3 seams, 6 personas, 11
tickets. **Measure first, write narration to the measurement** — if JaCoCo returns 87%, the narration
says 87%. Three specific calls:

- **`<50ms` dropped.** Prose-only in three docs, unmeasured. Measuring it would mean changing the
  product to serve the film, and a laptop micro-benchmark is not a latency claim — a technical panel
  knows that, so the numeral costs more credibility than it buys. Replaced by the architectural claim
  (synchronous, in-request, no daemon), which was all the number was ever a proxy for.
- **KPI count = 20 scalars + 2 breakdowns**, code-derived from `HospitalKpiSummaryDto`. README says
  18 and `.wayfinder/map.md` says 21; the README is to be reconciled as a side task rather than the
  film matching a wrong number.
- **Coverage is measured, not enforced.** JaCoCo has `prepare-agent` + `report` but **no `check`
  goal**, and there is **no `.github/workflows`**. "Quality gates" is therefore not claimable. Framed
  in the author's own terms: the suite is the foundation, CI wiring is the trivially-added adapter —
  a *deferred* debt, not an absence. Counter-argument acknowledged: volunteering the CI gap invites a
  mechanical mark-down; overruled because the ledger already names larger debts (no auth, no real
  database) and inconsistency inside an honesty-based argument is worse than the weakness.

**D9 — AI-augmented development is placed by causal role: constraint in the complication, method in
the close, never a reason the design is good.**
*Rejected:* excluding it (the repo visibly contains `.kiro/skills/`, `.agents/`; undisclosed-then-
discovered is the worst ordering) and giving it a mechanism beat (breaks the cap, and competes with
the engineering argument — AI leverage is not *why the system is good*). The moment narration says
"the AI helped me choose X," thesis D1 collapses. Division of labour stated in the author's words:
AI writes the code; trade-offs, technical approach, final code shape and design considerations are
his.

**D10 — The persona switcher is reframed from ease-of-use to auditability-by-design, and shown
exactly once.**
Six personas → six seeded accounts, no login flows, which is what makes a five-desk workflow
demonstrable at all. Paired with its already-resolved trade-off: the same design is a production
security hole, hence `@Profile("prototype")` and a default `SecurityConfig` that refuses it —
*convenience, deliberately fenced.* Operated on screen only in R1, where it is the subject; every
other capture seeds the role invisibly via `setupScript` (`localStorage['admissions_role_persona']`
before SPA boot), because on-screen switching everywhere would cost ~4s per take and turn the film
into a tour of the topbar.

**D11 — Where the UI cannot prove something honestly, the test is the evidence.**
Applied to R4's HTTP 409. A faked two-tab race would be a lie on screen; `capture.mjs` drives a
single page.

**D12 — `scripts/` never appears on screen.**
User instruction. Removes the SQL-vs-log parity shell take (originally the intended money shot).
Pathway B is instead proven by `PathBDualPathwayReconciliationIntegrationTest` plus a `tail` of the
running JVM's structured audit lines — which requires launching the JAR with **stdout redirected to
`logs/app.log`**.

**D13 — Deliverables staged: plan → `storyline.yml` → compile → slide PNGs → **gate** → spend.**
Slides render free *before* the gate precisely so they can be fixed for nothing. No TTS and no
capture until the PNGs have been reviewed and the runbook prerequisites are actually up.

**D14 — Title *"Build the Foundations, Stub the Rest"*; CTA invites disagreement.**
*Rejected titles:* "Production-Architected, Prototype-Executed" (accurate, unquotable),
"The Seam Is the Deliverable" (over-indexes on R1), "Eleven Decisions and What They Cost" (kept as
the ledger slide's title). Because "foundations" lacks its own criterion, the answer slide must
define it. Role, company and seniority are never named on screen.

**D15 — Crisp register throughout; the domain taught as a data model.**
*Rejected:* warm bookends (this audience reads pathos in a technical argument as padding), a story-
based domain intro (costs 60–90s and drags toward the pitch), and lazy per-beat domain drip (five
mini-tutorials, never a whole model).

**D16 — Runtime target ~9:00–10:30, ceiling 15:00; landscape only; deck co-primary.**
The binding discipline is the **beat count**, not minutes. Vertical is not declared: a 9:16 reframe
of a nine-minute argument is format-mismatched, and `--aspect` reframes without shortening — a short
cut would be a separate storyline.

**D17 — The architecture diagram enters as a captured web page on `:4173`, not as a slide and not as
footage.**
The original problem: `@mermaid-js/mermaid-cli` renders a PNG into `assets/slides/`, but how that PNG
becomes a *scene* is not established, and `footage` validation rejects a generated `src` under
`assets/` because that directory is gitignored. A `marp` slide is also unavailable — `engine` is
chosen **per deck**, and this deck is `html`; mixing renderers inside one deck is what the
one-renderer rule exists to prevent, since it is what keeps deck and film pixel-identical by
construction rather than by inspection.

The resolution avoids all three problems: author the diagram as a **local page served on the `:4173`
origin** (the same origin already carrying the coverage reports) and capture it with Playwright via an
absolute `url`, exactly like any other capture. Consequences, all of them favourable:

- it is an ordinary `capture` entry, so it inherits `assert.visible` / `assert.notVisible` and the
  content-addressed cache with no new machinery;
- it may embed a pre-rendered **mermaid** SVG *if* mermaid suits the shape — mermaid becomes an
  optional authoring convenience rather than a load-bearing pipeline dependency;
- it can **reveal progressively** under Playwright `click` steps, which the reference material
  recommends for anything past ~six nodes, since a full diagram appearing at once is rarely read;
- the deck stays a single `html` renderer, so nothing about pixel-parity changes.

*Rejected:* a native `process-flow` bento slide as the diagram (was the fallback — it survives as the
R1 mechanism slide, but a bento layout cannot express a two-profile fan-out of one interface into two
adapters); a mermaid PNG smuggled in as `footage` (rejected by validation, and would need committing
outside `assets/`).

**D18 — Terminal takes enter as `footage`, because the storyline cannot express a terminal scene.**
Discovered while authoring. `ENTRY_TYPES` is `slide | capture | footage` (`deck.mjs:79`) and
`compileStoryline` hard-maps type → `visualEngine` with **no passthrough** (`deck.mjs:384-430`), so
`vhs_terminal` — a legal *storyboard* engine (`storyboard-schema.md:71`) — is unreachable from
`storyline.yml`. Three ways out were considered:

1. **Hand-author `storyboard.json`.** Rejected: it is the generated lockfile, `compile.mjs` refuses a
   file with no `$generated` marker, and abandoning the storyline would forfeit `in: [deck]`, the SCR
   checks and every bento slide. The deck is co-primary, so this is not a trade worth making.
2. **Render captured terminal output into an HTML page and screenshot it.** Rejected: it looks like a
   terminal but is a re-render of text, which is precisely the "polish indistinguishable from
   correctness" failure C1 accuses others of.
3. **Adopted:** run VHS ourselves against a committed `.tape`, and reference the resulting mp4 as a
   `footage` entry. The tape is the source, the mp4 is the frozen input — the same pattern the skill
   already mandates for generated footage, and legitimate here because non-negotiable #8 constrains
   *generated* footage, while this is *recorded* output of a real command.

Consequence: **`footage/` now exists in this work dir**, superseding §9's "no `footage/`" line. The
directory is deliberately not gitignored, so the mp4s are committed and the film stays re-renderable
on a fresh clone. Note this makes VHS a hard prerequisite rather than a degradable one — without the
binary there is no mp4 to reference, and the pipeline's own "terminal scenes degrade to styled code
slides" fallback does not apply, because from the storyline's point of view these are footage scenes,
not terminal scenes.

**D19 — R3's absolute-tier evidence is a test, not a screenshot, because the UI branch is dead code.**
Discovered while authoring, and it invalidated the plan's original money shot. `bmu.tsx:1173` renders
*"Biological gender or airborne isolation safety violation. Zero override allowed."* — but
`safetyViolationReason` and `isOperationalOverride` are declared on `BedRecommendation` and in the
frontend types and **never populated by any backend code path**. The solver *filters* violating beds
out of the candidate set, so no disabled card is ever produced from real solver output.

Capturing that card would therefore have required stubbing the API to force a state the system cannot
reach — staging dead UI, which is the exact demoware this film argues against. So Tier 1 is proved by
**absence**, and absence is only assertable: `HeuristicBedAllocationSolverTest`'s display names
(*"eliminates wards without negative pressure…"*) are the claim, verbatim, on screen. The narration
says so out loud — *"there is no disabled button to photograph"* — which converts a limitation into an
argument about what constitutes proof.

Side effects: this resolves §8's open item about which test class holds the tier assertions
(`HeuristicBedAllocationSolverTest`, four test methods); and the unpopulated DTO fields plus the
unreachable frontend branch are a genuine small defect, now logged as a repo side task. It is **not**
put on screen — it is an unwired field, not a design trade-off, and the ledger is for debts with
seams.

---

## 7. Build & Pipeline

### 7.1 Rig

| | |
| --- | --- |
| Skill copy | **`.kiro/skills/video-generator`** — the newer copy (has `references/storyline-authoring.md`, `templates/storyline/`). The `.agents/` copy is stale (`deck-authoring.md`, `templates/deck/`) and must not be used |
| Work dir | `video-generation-technical/` — its own `.runtime/`; nothing carries over from `video-generation-product/` |
| Bootstrap | `node .kiro/skills/video-generator/scripts/bootstrap.mjs --workDir=video-generation-technical --with-remotion=false --with-vhs` — Remotion defaults to *on* and its pinned version no longer resolves; nothing here needs it |
| Source / lockfile | `storyline.yml` authored (**filename is load-bearing** — `generate.mjs` hardcodes it and silently skips compilation if absent); `storyboard.json` generated, committed, never hand-edited |
| Deck | `engine: html`, `theme: light`, `aspect: landscape`, `outputs: [landscape]`, `formats: [png, pdf, html]` |
| Determinism | `baseUrl: http://127.0.0.1:8080`; `fixedTime: '2026-01-15T09:00:00.000Z'` (same as the product film) |
| Voice | Kokoro, `gender: female` |
| Ignored / committed | `assets/` gitignored; `storyline.yml`, `storyboard.json`, `scenes/`, `pages/` and **`footage/`** committed (D18 makes `footage/` load-bearing, and it is deliberately not gitignored) |

**Multi-origin capture:** `capture.mjs:155` resolves `new URL(spec.url ?? '/', options.baseUrl)`, so
an **absolute** `url` bypasses `baseUrl` entirely. That makes the coverage reports and the
seam-diagram page (D17) free to capture from a second origin, and the app is left untouched.
*Rejected:* copying those assets into the Spring Boot static resources, which would pollute the
application to serve a video.

### 7.2 Runbook — every prerequisite, in order

1. **You (one time, manual, non-hermetic):** `brew install charmbracelet/tap/vhs ttyd`. macOS has no
   usable upstream `ttyd` binary, so terminal capture cannot be made hermetic here. If skipped,
   `vhs_terminal` scenes **degrade to monospaced syntax-coloured slides** — nothing breaks, but four
   of the strongest takes lose their live output, and the degradation must be reported.
2. `cd backend && ./mvnw clean test` then `jacoco:report` — **the backend coverage HTML does not
   exist yet** (`backend/target/site/jacoco/` is absent). Frontend `coverage/index.html` is present.
3. `./mvnw clean package`; launch the prototype JAR on `:8080` with
   `-Dspring.profiles.active=prototype` **and stdout redirected to `logs/app.log`** (required by
   R5's `tail` take). **This JVM must survive the entire capture run.**
4. Launch a **second JVM with no profile** on `:8081` for R1's fence demo.
5. Serve `backend/target/site/jacoco/`, `frontend/coverage/` **and the seam-diagram page (D17)** on
   `:4173`.
6. **Exercise the workflows before R5 is captured** — the R1–R4 captures already do most of this.

### 7.3 Two ordering constraints that are correctness issues, not preferences

- **Nothing may restart the `:8080` JVM after R1.** H2 is `create-drop`
  (`application-prototype.yml`), so a restart empties the data R5's dashboard reads, and the film
  ships with a dashboard that contradicts the workflows the viewer just watched. This is precisely
  why R1's no-profile demo runs as a **separate process on `:8081`** rather than as a restart.
- **A dashboard of zeros satisfies every assertion we would naturally write.**
  `assert.visible: ['Avg ED Turnaround']` passes on a freshly-seeded, unexercised system. R5's
  captures must therefore assert on a **non-zero rendered value**, or on text that only renders once
  a benchmark evaluates — otherwise the invariant does not bind where it matters most.

### 7.4 Stage order and regeneration

`compile.mjs` → `deck.mjs` (slides, free, no TTS) → **outline gate** → `synthesize.mjs` →
**re-derive every capture's trailing `wait delayMs` from measured audio** → `capture.mjs` →
`compose.mjs` → `verify.mjs`.

- Initial `delayMs` values are derived at ~150 wpm + a 400 ms tail, then **re-derived from measured
  audio after the first `synthesize.mjs`** to keep `ratePct` inside the inaudible band
  (`core.mjs RATE_FIT.INAUDIBLE`). This is the only way to avoid a second, expensive capture pass.
- **Terminal takes are inelastic** — a test suite takes as long as it takes — so narration is fitted
  to them, never the reverse. Budget `sleep` generously; an under-slept tape truncates the output
  being narrated.
- The cache is content-addressed per scene, so editing one sentence costs one scene — **except** that
  capture keys include the app version, so a changed app HEAD or a changed `delayMs` rebuilds that
  clip.
- Assertions are mandatory on every capture: `assert.visible` for the content that must be there,
  `assert.notVisible` for `['Something went wrong', 'No static resource', 'Sign in']`.

---

## 8. Open Items — resolve before or during authoring

**Resolved during authoring (18 Sep):**

- ~~Mermaid path unverified.~~ **D17.** The seam diagram is a captured web page at
  `http://127.0.0.1:4173/seam.html`, authored at `video-generation-technical/pages/seam.html` and
  verified at 1920×1080 with its two-step reveal working. It uses no mermaid, no CDN and no webfont,
  so it renders identically anywhere.
- ~~Which test class holds the two-tier constraint assertions.~~ **`HeuristicBedAllocationSolverTest`**
  — four methods, whose display names are themselves the on-screen claim (D19).
- ~~`/bmu/config` path.~~ Correct as written: `router.tsx:62` declares `/bmu/config` explicitly, even
  though the route file is `bmu-config.tsx`.
- **Soft weights are now exact:** specialty alignment **40**, cubicle consolidation **30**, falls-risk
  proximity **15**, rendered by the form as `+40 pts` (`bmu-config.tsx:89-91`). Narration says points,
  matching the screen, not percentages.
- **Remotion is not installed and is not needed.** `bootstrap.mjs` pins `@remotion/cli@4.0.360`, which
  no longer resolves from the registry, and it defaults `withRemotion` to **true** (`:286`), so the
  bootstrap must be run with `--with-remotion=false`. Nothing in this film needs it: landscape only,
  no PIP, no kinetic captions, so FFmpeg is sufficient by the engine-derivation rule. Unpinning a
  dependency to install software we do not use would be the wrong trade.
- **`process-flow` overflows at five cards** with text of this density. Both five-card slides were cut
  to four (hub plus three nodes), which is also a better read.

**Still open:**

- **403 vs 401** from `SecurityConfig` on the no-profile JVM: an explicit `SecurityFilterChain` with no
  declared authentication mechanism most likely yields **403** via `Http403ForbiddenEntryPoint`, but
  this must be **observed at record time** and the narration written to what the tape shows. The
  narration is currently written to say "denied", which is true either way.
- **Actual coverage numbers** are unknown until runbook step 2 runs. `09d-coverage-capture` asserts
  only on structural text (`Missed Instructions`, `com.hospital.admissions`), and the narration
  deliberately quotes no figure — per D8, the number gets spoken only after it is measured.
- **`09a-analytics-capture` non-zero risk.** It asserts `Target: 100% phone-free`, a benchmark string
  that renders alongside a metric, but a **zeroed dashboard would still satisfy it**. Either exercise
  the workflows first (runbook step 6, which the R1–R4 captures largely do) or tighten the assertion
  to a value-bearing string once the real dashboard has been seen.
- **`05d-persona-fence-capture` selector is a guess.** `header select, header button:has-text("ED")`
  needs confirming against the running header; the switcher's DOM shape was read from `Header.tsx` but
  not exercised.
- **Eight `.tape` files are not yet written** — deliberately, since a beat cut at this gate would waste
  them. They are the first task after approval.
- **README reconciliation** (side task, outside this film): "18 operational KPI metrics" → 20 + 2, and
  the "quality gates" wording against the absence of CI.
- **Dead code side task** (from D19): `BedRecommendation.safetyViolationReason` /
  `isOperationalOverride` are never populated, and `bmu.tsx:1173` is unreachable. Either wire the
  solver to return flagged-but-forbidden beds, or delete the branch and the fields.

---

## 9. Non-Goals

- Re-selling the product, re-explaining the six pain points, or reusing the product film's beats.
- Any patient-journey narrative, emotional framing, or "the wait every family knows" register.
- Code on screen, IDE footage, generated b-roll, background music. (`footage/` does exist, but it holds
  only VHS recordings of real commands — see D18.)
- `scripts/`, `/h2-console` (a third view of the pathway we are least challenged on), E2E/Allure.
- Vertical (9:16) output, PPTX emission, any claim about future pricing or performance that no shot
  produces.
- Naming a target role, company or seniority.

---

## 10. Definition of Done

1. `storyline.yml` compiles clean — SCR skeleton satisfied (2/2/5), no overflow, no unknown icons,
   every capture carries `assert.visible` **and** `assert.notVisible`.
2. Every resolution beat names a **price** in narration *and* carries it as a `detail:` on a card —
   verified by hand, because the compiler only enforces cost-words on complications.
3. Every number spoken is produced by the shot it is spoken over.
4. Slide PNGs reviewed at 1920×1080 **before** any TTS or capture spend.
5. All six runbook prerequisites up, and both ordering constraints in §7.3 observed.
6. `verify.mjs` reports `PASS`; the run report's degraded-feature list is read and accepted (VHS
   absence, if any, appears there).
7. Deck exports as PDF + HTML and reads standalone without the narration.
