#!/usr/bin/env node

import { existsSync, mkdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const input = process.argv[2];

if (!input || !existsSync(input)) {
    console.error("Usage: node scripts/generate-launcher-icons.mjs /path/to/icon-sheet.png");
    process.exit(1);
}

// The upper-left artwork in the supplied 1536 x 1024 sheet has no baked-in
// launcher shape. Scale it to sit wholly inside Android's 66/108-dp safe zone.
const destination = resolve(projectRoot, "app/src/main/res/drawable-nodpi/ic_launcher_foreground.png");
mkdirSync(dirname(destination), { recursive: true });

const result = spawnSync(
    "ffmpeg",
    [
        "-y", "-i", input,
        "-vf", "crop=520:600:0:10,pad=600:600:0:0:color=0x00000000,scale=700:700:flags=lanczos,pad=1024:1024:162:162:color=0x00000000",
        "-frames:v", "1", "-update", "1", destination,
    ],
    { stdio: "inherit" },
);
if (result.status !== 0) process.exit(result.status ?? 1);
