---
marp: true
theme: default
paginate: false
backgroundColor: #0f172a
color: #e2e8f0
style: |
  section { font-family: Arial, 'Liberation Sans', sans-serif; padding: 80px; justify-content: center; }
  h1 { color: #38bdf8; font-size: 46px; line-height: 1.15; }
  h2 { color: #7dd3fc; font-size: 30px; font-weight: 500; }
  strong { color: #fbbf24; }
  ol { font-size: 28px; line-height: 1.55; }
  p { font-size: 26px; line-height: 1.5; }
---

# How ghost capacity is actually recovered

## Two phases, run in order

1. **Phase 1 — Consolidation packing.** Pack waiting patients into partly filled cubicles that already match their cohort, protecting all-clean rooms from a premature single-patient lock.
2. **Phase 2 — Holding-room batching.** Only then designate an all-clean cubicle for a surge cluster of three or more.

Pack first, then batch. That ordering is what turns beds that were empty on paper into beds you can actually use.
