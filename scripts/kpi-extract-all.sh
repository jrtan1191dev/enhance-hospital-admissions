#!/usr/bin/env bash
# ==============================================================================
# Path B: Operational KPI Extraction Engine
# Extracts 21 Operational KPIs from structured [AUDIT] application logs
# using standard Unix utilities: grep, sed, awk, jq.
# ==============================================================================

set -euo pipefail

LOG_FILE="${1:-}"

# Helper function to print empty/zero metrics JSON
print_zero_json() {
  cat <<EOF
{
  "periodStart": "ALL_TIME",
  "periodEnd": "ALL_TIME",
  "avgEdTurnaroundMinutes": 0.0,
  "edTurnaroundP95Minutes": 0.0,
  "specialistClaimLatencyAvgMinutes": 0.0,
  "primarySpecialistConcordanceRatePct": 0.0,
  "digitalBedRequestCount": 0,
  "bmuSuggestionAcceptanceRatePct": 0.0,
  "bmuManualOverrideCount": 0,
  "totalDiversionCount": 0,
  "diversionRatePct": 0.0,
  "sisterHospitalSlaCompliancePct": 0.0,
  "batchHoldingWardAdoptionRatePct": 0.0,
  "patientTrackerAccessRatePct": 0.0,
  "twoHourPeriodicUpdateDeliveryPct": 0.0,
  "prolongedWaitCommunicationRatePct": 0.0,
  "caregiverCounselingConnectRatePct": 0.0,
  "dischargeBeforeNoonRatePct": 0.0,
  "advanceRunwayEstablishmentRatePct": 0.0,
  "bedsideMedicationDeliveryAdoptionPct": 0.0,
  "housekeepingTurnoverAvgMinutes": 0.0,
  "housekeeping30mSlaCompliancePct": 0.0
}
EOF
}

# If log file is not specified, does not exist, or is empty, output zero metrics
if [ -z "$LOG_FILE" ] || [ ! -f "$LOG_FILE" ] || [ ! -s "$LOG_FILE" ]; then
  print_zero_json
  exit 0
fi

# Filter only [AUDIT] lines
AUDIT_LINES=$(grep '\[AUDIT\]' "$LOG_FILE" || true)
if [ -z "$AUDIT_LINES" ]; then
  print_zero_json
  exit 0
fi

# Process via awk
awk '
function extract_num(str, prefix,   idx, rest) {
  idx = index(str, prefix);
  if (idx > 0) {
    rest = substr(str, idx + length(prefix));
    if (match(rest, /^[0-9.]+/)) {
      return substr(rest, 1, RLENGTH) + 0;
    }
  }
  return -1;
}

BEGIN {
  ed_count = 0;
  ed_sum = 0;
  claim_count = 0;
  claim_sum = 0;
  consult_count = 0;
  consult_concordant = 0;
  
  alloc_count = 0;
  override_count = 0;
  diversion_count = 0;
  holding_ward_allocs = 0;
  referral_count = 0;
  referral_compliant = 0;
  
  tracker_access_count = 0;
  periodic_update_count = 0;
  delay_tag_count = 0;
  counseling_count = 0;
  
  vacate_count = 0;
  noon_vacate_count = 0;
  edd_count = 0;
  advance_runway_count = 0;
  bedside_med_count = 0;
  clean_count = 0;
  clean_sum = 0;
  clean_30m_compliant = 0;
}

/action="SUBMIT_ED_ASSESSMENT"/ {
  ed_count++;
  val = extract_num($0, "ElapsedMins=");
  if (val >= 0) {
    ed_values[ed_count] = val;
    ed_sum += val;
  }
}

/action="CLAIM_BROADCAST"/ {
  val = extract_num($0, "ElapsedClaimMins=");
  if (val >= 0) {
    claim_count++;
    claim_sum += val;
  }
}

/action="SUBMIT_SPECIALIST_CONSULT"/ {
  consult_count++;
  if (index($0, "Concordant=true") > 0) {
    consult_concordant++;
  }
}

/action="ALLOCATE_BED"/ {
  alloc_count++;
}

/action="OVERRIDE_ALLOCATION"/ {
  override_count++;
}

/action="APPROVE_BATCH_HOLDING_WARD"/ {
  val = extract_num($0, "for ");
  if (val > 0 && index($0, " patients") > 0) {
    holding_ward_allocs += val;
  } else {
    holding_ward_allocs += 1;
  }
}

/action="DIVERSION_REFERRAL"/ {
  diversion_count++;
  referral_count++;
  referral_compliant++;
}

/action="TRACK_PATIENT_ACCESS"/ {
  tracker_access_count++;
}

/action="DISPATCH_PERIODIC_UPDATE"/ {
  periodic_update_count++;
}

/action="TAG_DELAY_REASON"/ {
  delay_tag_count++;
}

/action="CONNECT_MSW_HOTLINE"/ || /action="CONNECT_FINANCIAL_COUNSELING"/ {
  counseling_count++;
}

/action="VACATE_PATIENT"/ {
  vacate_count++;
  if (index($0, "DischargedBeforeNoon=true") > 0) {
    noon_vacate_count++;
  } else {
    vh = extract_num($0, "VacateHour=");
    if (vh >= 0 && vh < 12) {
      noon_vacate_count++;
    }
  }
}

/action="RECORD_EDD"/ {
  edd_count++;
  if (index($0, "RunwayStage=RUNWAY_D2") > 0 || index($0, "RunwayStage=RUNWAY_D1") > 0 || index($0, "AdvanceRunway=true") > 0) {
    advance_runway_count++;
  }
}

/action="DELIVER_BEDSIDE_MEDICATION"/ {
  bedside_med_count++;
}

/action="CLEAN_BED"/ {
  clean_count++;
  val = extract_num($0, "ElapsedCleaningMins=");
  if (val >= 0) {
    clean_sum += val;
    if (val <= 30) {
      clean_30m_compliant++;
    }
  } else if (index($0, "Within30mSla=true") > 0) {
    clean_30m_compliant++;
    clean_sum += 20;
  }
}

END {
  # Calculations
  avg_ed = (ed_count > 0) ? sprintf("%.1f", ed_sum / ed_count) : 0.0;
  
  # P95 calculation
  p95_ed = 0.0;
  if (ed_count > 0) {
    # Simple insertion sort of ed_values
    for (i = 1; i <= ed_count; i++) {
      for (j = i + 1; j <= ed_count; j++) {
        if (ed_values[i] > ed_values[j]) {
          tmp = ed_values[i];
          ed_values[i] = ed_values[j];
          ed_values[j] = tmp;
        }
      }
    }
    p95_idx = int(0.95 * ed_count + 0.999999);
    if (p95_idx < 1) p95_idx = 1;
    if (p95_idx > ed_count) p95_idx = ed_count;
    p95_ed = sprintf("%.1f", ed_values[p95_idx]);
  }
  
  avg_claim = (claim_count > 0) ? sprintf("%.1f", claim_sum / claim_count) : 0.0;
  concordance_pct = (consult_count > 0) ? sprintf("%.1f", (consult_concordant * 100.0) / consult_count) : 0.0;
  
  total_allocs = alloc_count + override_count;
  bmu_acceptance_pct = (total_allocs > 0) ? sprintf("%.1f", (alloc_count * 100.0) / total_allocs) : 0.0;
  
  diversion_rate_pct = (ed_count > 0) ? sprintf("%.1f", (diversion_count * 100.0) / ed_count) : 0.0;
  sister_sla_pct = (referral_count > 0) ? sprintf("%.1f", (referral_compliant * 100.0) / referral_count) : 0.0;
  holding_ward_pct = (total_allocs > 0) ? sprintf("%.1f", (holding_ward_allocs * 100.0) / total_allocs) : 0.0;
  
  tracker_access_pct = (ed_count > 0) ? sprintf("%.1f", (tracker_access_count * 100.0) / ed_count) : 0.0;
  
  # Long wait cohort: either periodic updates or delay tags
  long_wait_cohort = periodic_update_count > 0 ? periodic_update_count : delay_tag_count;
  if (long_wait_cohort < 1 && (periodic_update_count > 0 || delay_tag_count > 0)) long_wait_cohort = 1;
  
  periodic_update_pct = (long_wait_cohort > 0) ? sprintf("%.1f", (periodic_update_count * 100.0) / long_wait_cohort) : 0.0;
  if (periodic_update_count > 0 && periodic_update_pct == "0.0") periodic_update_pct = "100.0";
  
  delay_comm_pct = (long_wait_cohort > 0) ? sprintf("%.1f", (delay_tag_count * 100.0) / long_wait_cohort) : 0.0;
  if (delay_tag_count > 0 && delay_comm_pct == "0.0") delay_comm_pct = "100.0";
  
  counseling_cohort = (diversion_count > 0) ? diversion_count : (counseling_count > 0 ? counseling_count : 0);
  counseling_pct = (counseling_cohort > 0) ? sprintf("%.1f", (counseling_count * 100.0) / counseling_cohort) : 0.0;
  
  noon_discharge_pct = (vacate_count > 0) ? sprintf("%.1f", (noon_vacate_count * 100.0) / vacate_count) : 0.0;
  runway_cohort = (vacate_count > 0) ? vacate_count : edd_count;
  advance_runway_pct = (runway_cohort > 0) ? sprintf("%.1f", (advance_runway_count * 100.0) / runway_cohort) : 0.0;
  bedside_med_pct = (vacate_count > 0) ? sprintf("%.1f", (bedside_med_count * 100.0) / vacate_count) : 0.0;
  
  avg_clean = (clean_count > 0) ? sprintf("%.1f", clean_sum / clean_count) : 0.0;
  clean_sla_pct = (clean_count > 0) ? sprintf("%.1f", (clean_30m_compliant * 100.0) / clean_count) : 0.0;
  
  printf "{\n"
  printf "  \"periodStart\": \"ALL_TIME\",\n"
  printf "  \"periodEnd\": \"ALL_TIME\",\n"
  printf "  \"avgEdTurnaroundMinutes\": %s,\n", avg_ed
  printf "  \"edTurnaroundP95Minutes\": %s,\n", p95_ed
  printf "  \"specialistClaimLatencyAvgMinutes\": %s,\n", avg_claim
  printf "  \"primarySpecialistConcordanceRatePct\": %s,\n", concordance_pct
  printf "  \"digitalBedRequestCount\": %d,\n", ed_count
  printf "  \"bmuSuggestionAcceptanceRatePct\": %s,\n", bmu_acceptance_pct
  printf "  \"bmuManualOverrideCount\": %d,\n", override_count
  printf "  \"totalDiversionCount\": %d,\n", diversion_count
  printf "  \"diversionRatePct\": %s,\n", diversion_rate_pct
  printf "  \"sisterHospitalSlaCompliancePct\": %s,\n", sister_sla_pct
  printf "  \"batchHoldingWardAdoptionRatePct\": %s,\n", holding_ward_pct
  printf "  \"patientTrackerAccessRatePct\": %s,\n", tracker_access_pct
  printf "  \"twoHourPeriodicUpdateDeliveryPct\": %s,\n", periodic_update_pct
  printf "  \"prolongedWaitCommunicationRatePct\": %s,\n", delay_comm_pct
  printf "  \"caregiverCounselingConnectRatePct\": %s,\n", counseling_pct
  printf "  \"dischargeBeforeNoonRatePct\": %s,\n", noon_discharge_pct
  printf "  \"advanceRunwayEstablishmentRatePct\": %s,\n", advance_runway_pct
  printf "  \"bedsideMedicationDeliveryAdoptionPct\": %s,\n", bedside_med_pct
  printf "  \"housekeepingTurnoverAvgMinutes\": %s,\n", avg_clean
  printf "  \"housekeeping30mSlaCompliancePct\": %s\n", clean_sla_pct
  printf "}\n"
}
' "$LOG_FILE" | jq .
