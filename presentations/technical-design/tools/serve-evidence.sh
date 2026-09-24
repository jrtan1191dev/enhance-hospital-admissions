#!/usr/bin/env bash
# Serve every non-application artifact the film captures, on one origin: :4173.
#
# An absolute `url` in storyline.yml bypasses video.baseUrl (capture.mjs:155), so the
# coverage report, the seam diagram and the terminal takes all come from here and the
# application is left untouched — nothing is copied into Spring Boot's static
# resources to serve a video.
#
# Run from the repository root:  bash presentations/technical-design/tools/serve-evidence.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
WORK="$ROOT/presentations/technical-design"
SERVE="$WORK/.serve"

if [[ ! -f "$ROOT/backend/target/site/jacoco/index.html" ]]; then
  echo "!! backend/target/site/jacoco/index.html is missing."
  echo "   Run: (cd backend && ./mvnw test jacoco:report)   — WITHOUT 'clean', which deletes it."
  exit 1
fi

rm -rf "$SERVE"; mkdir -p "$SERVE"
ln -s "$WORK/pages/seam.html"              "$SERVE/seam.html"
ln -s "$WORK/pages/term.html"              "$SERVE/term.html"
ln -s "$WORK/pages/runs"                   "$SERVE/runs"
ln -s "$ROOT/backend/target/site/jacoco"   "$SERVE/jacoco"
ln -s "$ROOT/frontend/coverage"            "$SERVE/frontend-coverage"

if lsof -ti:4173 >/dev/null 2>&1; then
  echo "-- :4173 already in use; leaving the existing server alone"
else
  (cd "$SERVE" && nohup python3 -m http.server 4173 >/tmp/serve4173.log 2>&1 &)
  sleep 1.5
fi

fail=0
for u in /seam.html /term.html /jacoco/index.html /frontend-coverage/index.html /runs/05b-consensus-gate.json; do
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:4173$u")
  printf '  %-40s %s\n' "$u" "$code"
  [[ "$code" == "200" ]] || fail=1
done
exit $fail
