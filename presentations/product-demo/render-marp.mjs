#!/usr/bin/env node
/**
 * render-marp.mjs — RETIRED. Kept as historical grounding only.
 *
 * Superseded by the skill's native marp engine (`presentation.engine: marp`),
 * which emits real Markdown and shares one `slideCss()` generator and one
 * Chromium with the HTML engine. This script predates that: it existed because
 * the old skill declared `marp_slides` as a visualEngine, probed for the Marp
 * CLI, and then shipped no stage that rendered slide markdown into the per-scene
 * MP4 capture.mjs expects at assets/recordings/{id}.mp4.
 *
 *   scenes/<id>.md  --marp-->  assets/slides/<id>.png  --ffmpeg-->  assets/recordings/<id>.mp4
 *
 * It no longer runs against the live storyboard: this presentation compiles with
 * `engine: html`, so no scene carries `visualEngine: marp_slides` and the script
 * exits early by design. Its inputs — `scenes/*.md` and
 * `storyboard.marp-old.json` — are retained as the pre-skill reference the
 * narration plan cites, not as build inputs.
 *
 * Paths below were updated for the presentations/<slug>/ layout: the work dir is
 * this file's own directory, and `.runtime/` is now shared at the root above it.
 */
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import path from 'node:path';

const workDir = import.meta.dirname;
const runtime = path.join(workDir, '..', '.runtime', 'node_modules');
const marpCli = path.join(runtime, '@marp-team', 'marp-cli', 'marp-cli.js');
const ffmpeg = path.join(runtime, 'ffmpeg-static', 'ffmpeg');
const slidesDir = path.join(workDir, 'assets', 'slides');
const recordingsDir = path.join(workDir, 'assets', 'recordings');
mkdirSync(slidesDir, { recursive: true });
mkdirSync(recordingsDir, { recursive: true });

const manifest = JSON.parse(readFileSync(path.join(workDir, 'storyboard.json'), 'utf8'));
const fps = manifest.meta?.fps ?? 30;

const marpScenes = manifest.scenes.filter((s) => s.visualEngine === 'marp_slides');
if (marpScenes.length === 0) {
  console.log('no marp_slides scenes; nothing to render');
  process.exit(0);
}

for (const scene of marpScenes) {
  const md = scene.actionSpec?.markdown;
  if (!md) {
    console.error(`${scene.id}: no actionSpec.markdown`);
    process.exit(1);
  }
  const mdPath = path.resolve(workDir, md);
  if (!existsSync(mdPath)) {
    console.error(`${scene.id}: markdown not found at ${mdPath}`);
    process.exit(1);
  }

  const png = path.join(slidesDir, `${scene.id}.png`);
  execFileSync(
    process.execPath,
    [marpCli, '--image', 'png', '--image-scale', '1', '--allow-local-files', '-o', png, mdPath],
    { stdio: 'inherit' }
  );

  // sceneMs is set by synthesise (audio dictates for a still). Fall back to
  // audio duration, then to a safe default only if neither exists yet.
  const sceneMs = scene.resolvedTiming?.sceneMs ?? scene.resolvedAudio?.durationMs ?? 6000;
  const out = path.join(recordingsDir, `${scene.id}.mp4`);

  // Held still, upscaled from Marp's 1280x720 deck to the 1920x1080 frame
  // (same 16:9 ratio, clean 1.5x), exact CFR — matching captureStills.
  execFileSync(
    ffmpeg,
    [
      '-y', '-loop', '1', '-framerate', String(fps), '-i', png,
      '-t', (sceneMs / 1000).toFixed(3),
      '-vf', `scale=1920:1080:flags=lanczos,fps=${fps},format=yuv420p`,
      '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '18', '-fps_mode', 'cfr',
      '-movflags', '+faststart', out,
    ],
    { stdio: 'inherit' }
  );
  console.log(`${scene.id}: rendered slide held ${Math.round(sceneMs)}ms -> ${path.relative(workDir, out)}`);

  // Write back resolvedVisual so compose.mjs (which reads scene.resolvedVisual
  // .filePath) includes this marp scene instead of skipping it as
  // "no captured visual". Mirrors what capture.mjs does for playwright scenes.
  scene.resolvedVisual = {
    filePath: path.relative(workDir, out),
    durationMs: sceneMs,
    roi: null,
    hash: createHash('sha256')
      .update(readFileSync(png))
      .update(String(sceneMs))
      .digest('hex')
      .slice(0, 12),
  };
}

// Persist the resolvedVisual write-backs so compose can find the marp visuals.
writeFileSync(
  path.join(workDir, 'storyboard.json'),
  `${JSON.stringify(manifest, null, 2)}\n`
);
console.log('updated storyboard.json with marp resolvedVisual entries');
