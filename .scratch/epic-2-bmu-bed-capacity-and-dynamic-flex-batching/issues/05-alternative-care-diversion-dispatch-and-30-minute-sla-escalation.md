# 05: Alternative Care Diversion Dispatch & 30-Minute SLA Escalation

**What to build:**
Provide BMU coordinators with digital referral dispatch and SLA lifecycle management for patients clinically endorsed for alternative care diversion (Tier 4 Subacute Community Hospitals or MIC@Home Virtual Wards). The BMU queue surfaces diversion-recommended badges for patients with specialist diversion endorsements. Coordinators can view a pre-populated digital referral packet and dispatch the referral electronically to partner Sister Hospitals (Outram Community Hospital, Alexandra Hospital, St. Andrew's Community Hospital) or MIC@Home. Dispatch initiates an active 30-minute bilateral SLA countdown timer. If the SLA expires without partner acceptance, the BMU console presents an escalation alert with actionable choices ("Recall to Acute Queue", "Extend SLA +15 Mins", "Log Telephone Follow-up"). For MIC@Home admissions, confirmation allocates a virtual bed identifier (`MIC-V...`) and transitions admission status to `DIVERTED_HAH`.

**Blocked by:** 01: Two-Tier Constraint Validation & Override Workflow

**Status:** completed

- [x] BMU queue displays high-visibility diversion indicator badges for patients endorsed for Tier 4 Subacute or MIC@Home care pathways.
- [x] BMU UI provides a digital referral drawer pre-populating patient demographics, vital signs, consult impressions, and destination facility.
- [x] Endpoint `POST /api/v1/bmu/diversion/refer` dispatches the referral, persists external referral metadata, and initiates a 30-minute bilateral SLA countdown.
- [x] Active referrals track remaining SLA time; upon reaching 0 minutes without partner acceptance, the UI displays visual escalation badges with actionable response triggers:
  - "Recall to Acute Queue" (cancels referral and returns request to standard acute queue)
  - "Extend SLA +15 Mins" (extends deadline and logs operational delay note)
  - "Log Telephone Follow-up" (records coordinator communication notes in audit history)
- [x] Dispatching or confirming a MIC@Home diversion allocates a synthetic virtual bed identifier (`MIC-V{sequence}`) and updates admission status to `DIVERTED_HAH`.
- [x] Structured audit log event `DIVERSION_REFERRAL` is emitted with coordinator identity, destination facility, referral ID, and SLA window.
- [x] End-to-end integration tests verify referral creation, SLA tracking, timeout escalation handling, and MIC@Home virtual bed assignment.
