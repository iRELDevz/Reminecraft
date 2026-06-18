import fs from "fs";
import path from "path";
import { on, off, emit } from "./scripting/events.ts";
import { getStats } from "./stats.ts";

const scriptsDir = path.resolve(process.cwd(), "../../scripts");
const loadedScripts = new Map<string, any>();

export function setupScriptingGlobals(): void {
    (globalThis as any).$on   = on;
    (globalThis as any).$off  = off;
    (globalThis as any).$emit = emit;
    (globalThis as any).$stats = getStats;
    (globalThis as any).$log  = (msg: string) =>
        console.log(`[ReMinecraft|SCRIPT|] ${msg}`);

    console.log("[ReMinecraft|BUN|] Scripting globals registered: $on, $off, $emit, $stats, $log");
}

export async function loadAllScripts(): Promise<void> {
    if (!fs.existsSync(scriptsDir)) {
        fs.mkdirSync(scriptsDir, { recursive: true });
        console.log(`[ReMinecraft|BUN|] Created scripts/ folder at: ${scriptsDir}`);
    }

    const files = fs.readdirSync(scriptsDir).filter(
        f => f.endsWith(".js") || f.endsWith(".ts")
    );

    for (const file of files) {
        await loadScript(file);
    }

    if (files.length === 0) {
        console.log("[ReMinecraft|BUN|] No scripts found in scripts/ folder.");
    }
}

export async function loadScript(filename: string): Promise<void> {
    const fullPath = path.join(scriptsDir, filename);
    try {
        const mod = await import(`${fullPath}?t=${Date.now()}`);
        loadedScripts.set(filename, mod);
        console.log(`[ReMinecraft|SCRIPT|] Loaded: ${filename}`);
    } catch (error) {
        console.error(`[ReMinecraft|SCRIPT|] Failed to load ${filename}:`, error);
    }
}

export function unloadScript(filename: string): void {
    if (loadedScripts.has(filename)) {
        loadedScripts.delete(filename);
        console.log(`[ReMinecraft|SCRIPT|] Unloaded: ${filename}`);
    }
}

export function startScriptWatcher(): void {
    fs.watch(scriptsDir, async (eventType, filename) => {
        if (!filename) return;
        if (!filename.endsWith(".js") && !filename.endsWith(".ts")) return;

        const fullPath = path.join(scriptsDir, filename);
        if (fs.existsSync(fullPath)) {
            console.log(`[ReMinecraft|SCRIPT|] Changed: ${filename} — reloading...`);
            unloadScript(filename);
            await loadScript(filename);
        } else {
            unloadScript(filename);
        }
    });
    console.log(`[ReMinecraft|BUN|] Watching scripts/ for changes (hot reload active).`);
}
