<!-- diagram-design-profile
name: presentation-producer (light)
slug: presentation-producer-light
source-url: none
notes: GENERATED from the built-in palette by presentation-producer. Do not edit; regenerate by compiling the presentation.
-->
# Style Guide

Generated for a presentation-producer slide. Use this as the effective style guide (diagram-design onboarding option: local design system) — never the installed default and never a URL onboarding.

## Tokens

### Semantic roles

| Role | Purpose | Default (light) |
|---|---|---|
| `paper` | Page background, default node fill | `#F1F5F9` |
| `paper-2` | Container bg, secondary fill | `#F8FAFC` |
| `ink` | Primary text, primary stroke | `#0F172A` |
| `ink-strong` | Text on accent fills | `#0F172A` |
| `muted` | Secondary text, default arrow stroke | `#64748B` |
| `soft` | Sublabels, boundary labels | `#64748B` |
| `rule` | Hairline borders | `#E2E8F0` |
| `rule-solid` | Stronger borders, baselines | `#E2E8F0` |
| `accent` | Focal: the element declared weight: primary (1-2 max) | `#10B981` |
| `accent-tint` | Fill for accent-bordered boxes | `color-mix(in srgb, #10B981 12%, transparent)` |
| `link` | HTTP/API calls, external arrows | `#64748B` |

## Typography

Sizes are slide canvas pixels at the drawn size, not diagram-design's doc ramp. Do not shrink type to fit: cut nodes or split the diagram.

| Role | Family | Size | Weight | Usage |
|---|---|---|---|---|
| `title` | Inter | 34px | 700 | Group or entity heading only — the slide title is the diagram title |
| `node-name` | Inter | 24px | 600 | Human-readable labels |
| `sublabel` | ui-monospace | 24px | 400 | Port, protocol, URL, field type (mixed case) |
| `eyebrow` | ui-monospace | 19px | 500, tracked 0.12em, UPPERCASE ONLY | Type tags, axis labels, index numbers |
| `arrow-label` | ui-monospace | 19px | 500, UPPERCASE ONLY | Arrow annotations |
| `callout` | Inter *italic* | 24px | 400 | Annotation callouts (max 2) |

### Font source

Font source: `system`

The slide embeds Inter; inline SVG text inherits it. Emit no font link.
