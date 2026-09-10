#!/usr/bin/env node

import { existsSync, mkdirSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const input = process.argv[2];

if (!input || !existsSync(input)) {
    console.error("Usage: node scripts/generate-launcher-icons.mjs /path/to/tacita-icon.png");
    process.exit(1);
}

const drawableDirectory = resolve(projectRoot, "app/src/main/res/drawable-nodpi");
mkdirSync(drawableDirectory, { recursive: true });

const outputs = [
    {
        name: "ic_launcher_foreground.png",
        // Android's central safe zone is 66dp within a 108dp adaptive-icon canvas.
        // The 620px art board leaves the full transparent illustration inside it.
        filter: "scale=620:620:flags=lanczos,pad=1024:1024:202:202:color=0x00000000",
    },
    {
        name: "tacita_header_icon.png",
        // Separate presentation asset for the in-app title; no launcher mask is baked in.
        filter: "scale=256:256:flags=lanczos",
    },
];

for (const output of outputs) {
    const result = spawnSync(
        "ffmpeg",
        ["-y", "-i", input, "-vf", output.filter, "-frames:v", "1", "-update", "1", resolve(drawableDirectory, output.name)],
        { stdio: "inherit" },
    );
    if (result.status !== 0) process.exit(result.status ?? 1);
}
