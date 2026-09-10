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

// The selected artwork is the rounded-square icon in the centre of the supplied
// 1536 x 1024 sheet. Keep the crop here so launcher assets can be regenerated.
const crop = "crop=512:512:512:48";
const densities = [
    ["mdpi", 48],
    ["hdpi", 72],
    ["xhdpi", 96],
    ["xxhdpi", 144],
    ["xxxhdpi", 192],
];

for (const [density, size] of densities) {
    const destination = resolve(projectRoot, `app/src/main/res/mipmap-${density}/ic_launcher.png`);
    mkdirSync(dirname(destination), { recursive: true });

    const result = spawnSync(
        "ffmpeg",
        ["-y", "-i", input, "-vf", `${crop},scale=${size}:${size}:flags=lanczos`, "-frames:v", "1", "-update", "1", destination],
        { stdio: "inherit" },
    );
    if (result.status !== 0) process.exit(result.status ?? 1);
}
