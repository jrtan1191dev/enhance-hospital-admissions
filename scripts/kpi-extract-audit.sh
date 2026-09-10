#!/usr/bin/env bash
# ==============================================================================
# Path B: Human-Readable Operational KPI Dashboard Extractor
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="${1:-}"

if [ -z "$LOG_FILE" ]; then
  echo "Usage: $0 <path-to-application-log-file>"
  exit 1
fi

JSON_OUTPUT=$("$SCRIPT_DIR/kpi-extract-all.sh" "$LOG_FILE")

echo "================================================================================"
echo "          HOSPITAL OPERATIONAL KPI SUMMARY (PATH B LOG EXTRACTION)             "
echo "================================================================================"
echo "Source Log File: $LOG_FILE"
echo "--------------------------------------------------------------------------------"
echo "1. ED Clinical Intake & Specialist Collaboration"
echo "   - Avg ED Turnaround:             $(echo "$JSON_OUTPUT" | jq -r '.avgEdTurnaroundMinutes') mins"
echo "   - P95 ED Turnaround:             $(echo "$JSON_OUTPUT" | jq -r '.edTurnaroundP95Minutes') mins"
echo "   - Specialist Claim Latency:      $(echo "$JSON_OUTPUT" | jq -r '.specialistClaimLatencyAvgMinutes') mins"
echo "   - Concordance Rate:              $(echo "$JSON_OUTPUT" | jq -r '.primarySpecialistConcordanceRatePct') %"
echo "   - Digital Bed Requests:          $(echo "$JSON_OUTPUT" | jq -r '.digitalBedRequestCount')"
echo "--------------------------------------------------------------------------------"
echo "2. BMU Capacity Orchestration & Diversions"
echo "   - Suggestion Acceptance Rate:    $(echo "$JSON_OUTPUT" | jq -r '.bmuSuggestionAcceptanceRatePct') %"
echo "   - Manual Overrides:              $(echo "$JSON_OUTPUT" | jq -r '.bmuManualOverrideCount')"
echo "   - Total Diversions:              $(echo "$JSON_OUTPUT" | jq -r '.totalDiversionCount') ($(echo "$JSON_OUTPUT" | jq -r '.diversionRatePct') %)"
echo "   - Sister Hospital 30m SLA:       $(echo "$JSON_OUTPUT" | jq -r '.sisterHospitalSlaCompliancePct') %"
echo "   - Batch Holding Ward Adoption:   $(echo "$JSON_OUTPUT" | jq -r '.batchHoldingWardAdoptionRatePct') %"
echo "--------------------------------------------------------------------------------"
echo "3. Patient & Family Milestone Tracking"
echo "   - Patient Tracker Access Rate:   $(echo "$JSON_OUTPUT" | jq -r '.patientTrackerAccessRatePct') %"
echo "   - 2h Periodic Push Delivery:     $(echo "$JSON_OUTPUT" | jq -r '.twoHourPeriodicUpdateDeliveryPct') %"
echo "   - Prolonged Wait Delay Tagging:  $(echo "$JSON_OUTPUT" | jq -r '.prolongedWaitCommunicationRatePct') %"
echo "   - Caregiver Counseling Connect:  $(echo "$JSON_OUTPUT" | jq -r '.caregiverCounselingConnectRatePct') %"
echo "--------------------------------------------------------------------------------"
echo "4. Inpatient Discharge Runway & Rapid Turnover"
echo "   - Discharges Before 12:00 PM:    $(echo "$JSON_OUTPUT" | jq -r '.dischargeBeforeNoonRatePct') %"
echo "   - Advance Runway (48h EDD):      $(echo "$JSON_OUTPUT" | jq -r '.advanceRunwayEstablishmentRatePct') %"
echo "   - Bedside Meds Delivery Rate:    $(echo "$JSON_OUTPUT" | jq -r '.bedsideMedicationDeliveryAdoptionPct') %"
echo "   - Housekeeping Turnover Avg:     $(echo "$JSON_OUTPUT" | jq -r '.housekeepingTurnoverAvgMinutes') mins"
echo "   - Housekeeping 30m SLA Rate:     $(echo "$JSON_OUTPUT" | jq -r '.housekeeping30mSlaCompliancePct') %"
echo "================================================================================"
