# Clinical Admission & Bed Capacity Orchestration

Core domain glossary for emergency department clinical intake, inpatient specialist broadcasts, and Bed Management Unit allocation.

## Language

### Clinical Urgency & Discordance

**Effective Acuity Tier**:
The authoritative clinical urgency level determining queue placement priority in the Bed Management Unit (BMU), computed as the highest acuity (lowest numerical tier) between primary and secondary evaluations.
_Avoid_: Priority score, calculated tier, queue rank

**Primary Acuity Tier**:
The initial clinical urgency tier assessed and submitted by the Emergency Department (ED) attending physician.
_Avoid_: Initial tier, ED tier

**Secondary Acuity Tier**:
The clinical urgency tier assessed and submitted by the consulting inpatient specialist during consult review.
_Avoid_: Specialist tier, consult tier

**Acuity Discordance**:
A condition where the Primary Acuity Tier and Secondary Acuity Tier diverge, triggering automatic priority escalation and continuous telemetry enforcement.
_Avoid_: Tier conflict, clinical disagreement, priority dispute

**Clinical Reconciliation**:
The asynchronous alignment process where an ED attending or consulting specialist adjusts their assessment to resolve a flagged acuity or telemetry discordance.
_Avoid_: Dispute resolution, doctor negotiation, override meeting

### Admission Pathways & Lifecycle

**Direct Admission**:
An admission pathway where the ED attending does not seek specialist consultation, completing the clinical assessment immediately and dispatching the bed request directly to the BMU queue in BED_REQUESTED status.
_Avoid_: Fast-track admission, auto-admission, immediate bed request

**Consult-Gated Admission**:
An admission pathway where specialist consultation is requested, holding the admission request in ASSESSMENT_PENDING status until the specialist consult is COMPLETED before dispatching to the BMU queue.
_Avoid_: Blocked admission, specialist hold, delayed intake

**Tentative Bed Allocation**:
A provisional bed assignment for an admission request in BED_ALLOCATED status prior to physical patient arrival, permitting dynamic reallocation for capacity optimization or clinical changes.
_Avoid_: Bed reservation, temporary bed, soft booking

**Admitting Specialty Cluster**:
The operational hospital discipline assigned to an admission request by the BMU coordinator upon reviewing completed clinical consults, directing candidate ward matching during bed allocation.
_Avoid_: Ward type, doctor discipline, admission specialty

### Alternative Care & Diversion Pathways

**Diversion Pathway**:
An alternative subacute inpatient care pathway endorsed by a consulting specialist, directing eligible patients toward community facilities or home hospitalization instead of acute hospital beds.
_Avoid_: Out-of-hospital option, alternative placement, transfer route

**Community Hospital Transfer**:
A subacute diversion pathway transferring stabilized patients to a partner community hospital for rehabilitation, subacute management, or convalescent care.
_Avoid_: Step-down referral, nursing home transfer, rehab trip

**Mobile Inpatient Care at Home (MIC@Home)**:
A diversion pathway providing hospital-level treatment, home visits, and telemonitoring in the patient's residence as a direct substitute for inpatient ward admission.
_Avoid_: Home care, outpatient checkup, home recovery

### ED Clinical Workflow & Boards

**Awaiting Assessment List**:
The active queue of emergency department patients who have not yet undergone initial clinical admission assessment.
_Avoid_: Patient list, intake queue, raw patients

**Assessed Admissions Tracker**:
The dedicated view displaying patients whose admission has been initiated by ED clinicians, showing real-time consult status, discordance alerts, and bed allocation progression.
_Avoid_: History tab, past admissions, archived patients

**Clinical Baseline Override**:
The deliberate modification by an ED clinician of an automated pre-populated recommendation, captured as structured field-level deltas in the audit log.
_Avoid_: AI change, chip edit, suggestion rejection

### Clinical Constraints & Monitoring

**Primary Telemetry Requirement**:
The cardiac telemetry constraint submitted by the Emergency Department attending physician during initial admission intake.
_Avoid_: ED telemetry, initial telemetry

**Secondary Telemetry Requirement**:
The cardiac telemetry constraint submitted by the consulting inpatient specialist during consult review.
_Avoid_: Specialist telemetry, consult telemetry

**Effective Telemetry Constraint**:
The enforced cardiac monitoring requirement for bed allocation, active if either the ED attending or consulting specialist indicates telemetry is required.
_Avoid_: Final telemetry, resolved telemetry

### Specialist Consult Broadcast Pool

**Assessment Broadcast**:
An asynchronous consultation request published to a specialty cluster feed for on-call inpatient specialists to review and claim.
_Avoid_: Consult ticket, specialist ping, alert

**Case Claiming**:
The atomic transition of an open Assessment Broadcast to claimed status by an authenticated specialist, establishing exclusive ownership of the consult review.
_Avoid_: Case locking, consult assignment, doctor grab

**Broadcast SLA**:
The maximum allowable elapsed duration before an unclaimed Assessment Broadcast breaches its service level agreement, determined by clinical acuity tier.
_Avoid_: Broadcast timeout, consult timer, wait limit

**Auto-Escalation**:
The automated assignment of an overdue Assessment Broadcast to the designated Default On-Call Specialist for that specialty cluster upon SLA breach.
_Avoid_: Fallback assignment, timeout grab, forced claim

**Default On-Call Specialist**:
The designated clinician for a specialty cluster configured to receive auto-escalated broadcasts when cluster members do not claim within the SLA.
_Avoid_: Backup doctor, fallback specialist, cluster lead

**Multi-Broadcast Consult Pool**:
The set of concurrent specialist consultation requests emitted to different specialty clusters for a single admission episode.
_Avoid_: Multi-doctor ticket, group consult

**Consult Chaining**:
The workflow enabling an active consulting specialist to publish additional broadcast requests to other specialty clusters before concluding their review.
_Avoid_: Specialist forwarding, secondary referral, re-broadcast

**Consensus Completion Gate**:
The domain invariant ensuring an admission request remains in ASSESSMENT_PENDING until all active and chained broadcasts reach COMPLETED before advancing to BED_REQUESTED in the BMU queue.
_Avoid_: Consult lock, multi-gate, all-done check

**Consult Amendment**:
An update to a completed specialist consult allowing appended impressions, revised secondary acuity, or telemetry modifications while preserving the admission's active position in the BMU queue.
_Avoid_: Consult edit, assessment rollback, second opinion

**Clinical Condition Updated Alert**:
A high-priority notification in the BMU queue signaling that a consulting specialist has amended clinical parameters on an active bed request.
_Avoid_: Change ping, edit badge, doctor update

### BMU Bed Capacity & Orchestration

**Flex Ward**:
A multi-bed ward maintained in an empty, cleaned state without active cohort locks, preserved to batch-admit waiting surge clusters.
_Avoid_: Flex-cubicle, holding room, swing cubicle, reserve room

**Ward Cohort Lock**:
The administrative constraint applied to an entire ward restricting it to a single biological gender and infection status upon placing an initial patient.
_Avoid_: Cubicle lock, room lock, cohort lockout, single-gender cubicle lockout

**Deallocation**:
The programmatic reversion of a bed from EMPTY_ASSIGNED back to EMPTY_CLEANED when an allocation is cancelled prior to physical patient arrival.
_Avoid_: Unassign, bed drop, soft unreserve

**Dynamic Cohort Swap**:
The atomic reassignment of an isolated assigned patient from a flex ward to an equivalent bed in a partially occupied ward to liberate the flex ward for a surge cluster.
_Avoid_: Patient shuffle, bed trade, cohort reshuffle

**Batch Holding Ward**:
A flex ward converted to admit an identified surge cluster of three or more matching patients in a single coordinated allocation.
_Avoid_: Surge cubicle, batch room, holding unit

**All-Clean Ward Reset**:
The automatic removal of a ward's cohort locks occurring strictly when every bed in the ward reaches EMPTY_CLEANED status following patient discharges.
_Avoid_: Empty reset, zero occupancy release, dirty unlock

**Operational Delay Tag**:
A structured bottleneck classification attached exclusively by BMU coordinators to prolonged-wait admission requests, automatically archived upon bed allocation.
_Avoid_: Delay reason, nurse delay note, queue blocker tag

**Static Telemetry Bed**:
An inpatient bed permanently equipped with continuous cardiac telemetry monitoring hardware, pruned by the allocation solver unless overridden by clinical operations.
_Avoid_: Telemetry bay, monitored bed, portable telemetry unit

**Absolute Safety Invariant**:
A clinical infection or cohorting constraint (biological gender cohorting in multi-bed wards, airborne infection negative pressure isolation) that software strictly forbids overriding under any circumstance.
_Avoid_: Hard rule, unskippable constraint, mandatory lock

**Overridable Operational Constraint**:
A capacity or equipment constraint (ward class subsidy tier, static telemetry equipping) that permits coordinator override when accompanied by a structured institutional justification code.
_Avoid_: Soft constraint, bypassable filter, flexible rule

### Patient & Family Public Tracking

**Public Tracking Token**:
An unguessable opaque identifier generated upon admission request creation that grants unauthenticated, read-only access to a patient's admission journey without exposing clinical charts or PII.
_Avoid_: Patient URL, tracking password, auth token, patient session

**Patient Milestone Stepper**:
The 3-stage progression on the public tracker indicating current boarding phase (Admission Confirmed -> Bed Assigned & Preparing Room -> Admitted to Inpatient Ward Bed), held in a quiescent state during ED assessment.
_Avoid_: Journey steps, admission progress bar, tracker timeline, 4-stage stepper

**Matching Ward Category**:
The classification bucket (specifically the patient's requested ward class: Class A, B1, B2, or C) within which pending bed requests are ranked by effective acuity tier and timestamp to determine queue position.
_Avoid_: Bed category, queue bucket, ward type

**Financial & Care Advisory (FYI Insights)**:
Informational non-binding guidance displayed on the public tracker detailing estimated daily co-pay ranges, government subsidy percentages, and step-down care options without collecting deposits or signatures.
_Avoid_: Cost calculator, bill estimate, payment portal

### Inpatient Discharge Runway & Bed Turnover

**Discharge Runway**:
The multi-day operational timeline leading up to a patient's expected departure, established during morning ward rounds via an Estimated Date of Discharge (EDD) to forecast bed turnover.
_Avoid_: Departure pipeline, discharge queue, checkout timer

**Estimated Date of Discharge (EDD)**:
The projected calendar date of inpatient discharge assigned by the clinical care team, paired with a confidence rating (High, Medium, Low) to drive forward bed capacity planning.
_Avoid_: Target discharge, planned checkout, estimated departure

**Morning Discharge Sign-Off**:
The formal clinical authorization executed during morning ward rounds (targeted before 09:30 AM) that initiates day-of-discharge medication preparation and prepares the bed for daytime turnover.
_Avoid_: Discharge order, checkout approval, doctor release

**Terminal Cleaning SLA**:
The enforced 30-minute operational time window during which Environmental Services (EVS) must sanitize an empty, vacated bed (in EMPTY_PENDING_CLEANING status) to return it to EMPTY_CLEANED status for live BMU allocation.
_Avoid_: Housekeeping timer, clean window, turnover limit

### Operational KPI & Audit Logging Architecture

**Dual-Pathway Extraction Architecture**:
The operational analytics paradigm that simultaneously persists indexed relational state transitions on auditable entities (Path A) and emits machine-parseable structured SLF4J audit events (Path B) to support both BI reporting and real-time observability.
_Avoid_: Double logging, dual tracking, secondary telemetry

**Audit Action Tag**:
A standardized uppercase identifier emitted within structured audit logs representing a discrete clinical or administrative milestone transition.
_Avoid_: Event type, log label, audit code
