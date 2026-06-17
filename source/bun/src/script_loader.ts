import fs from "fs";
import path from "path";

const scriptsDir = path.resolve(process.cwd(), "../../scripts");
const loadedScripts = new Map<string, any>();

export async function loadAllScripts(): Promise<void> {
    if (!fs.existsSync(scriptsDir)) {
        fs.mkdirSync(scriptsDir, { recursive: true });
        console.log(`[ReMinecraft|BUN|] Created scripts folder at: ${scriptsDir}`);
        fs.writeFileSync(path.join(scriptsDir, "welcome.js"), `console.log("[ReMinecraft|SCRIPT|] welcome.js loaded");\n`);
    }

    const files = fs.readdirSync(scriptsDir);
    for (const file of files) {
        if (file.endsWith(".js") || file.endsWith(".ts")) {
            await loadScript(file);
        }
    }
}

export async function loadScript(filename: string): Promise<void> {
    const fullPath = path.join(scriptsDir, filename);
    try {
        const importPath = `${fullPath}?update=${Date.now()}`;
        const module = await import(importPath);
        loadedScripts.set(filename, module);
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
            console.log(`[ReMinecraft|SCRIPT|] Changed: ${filename} - reloading...`);
            unloadScript(filename);
            await loadScript(filename);
        } else {
            console.log(`[ReMinecraft|SCRIPT|] Deleted: ${filename} - unloading...`);
            unloadScript(filename);
        }
    });
    console.log(`[ReMinecraft|BUN|] Watching scripts folder for changes...`);
}
