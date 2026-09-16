#!/usr/bin/env node
/**
 * render-marp.mjs — fills a gap in the video-generator pipeline.
 *
 * The skill declares `marp_slides` as a valid visualEngine and probes for the
 * Marp CLI, but no bundled stage actually renders slide markdown into the
 * per-scene MP4 that capture.mjs expects at assets/recordings/{id}.mp4.
 * (capture.mjs literally says "produced by render-assets.mjs", which does not
 * exist in this build.) This script produces those assets deterministically:
 *
 *   scenes/<id>.md  --marp-->  assets/slides/<id>.png  --ffmpeg-->  assets/recordings/<id>.mp4
 *
 * Each still is held for the scene's resolvedTiming.sceneMs (audio-driven), so
 * a slide never drifts against its narration. Run AFTER synthesise (so audio
 * durations exist) and BEFORE compose.
 */
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync } from 'node:fs';
import path from 'node:path';

const workDir = path.resolve('video-generation');
const runtime = path.join(workDir, '.runtime', 'node_modules');
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
}
