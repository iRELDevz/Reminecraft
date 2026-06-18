type Handler = (data: any) => void;

const listeners = new Map<string, Set<Handler>>();

export function on(event: string, handler: Handler): void {
    if (!listeners.has(event)) listeners.set(event, new Set());
    listeners.get(event)!.add(handler);
}

export function off(event: string, handler: Handler): void {
    listeners.get(event)?.delete(handler);
}

export function emit(event: string, data?: any): void {
    listeners.get(event)?.forEach(handler => {
        try {
            handler(data);
        } catch (e) {
            console.error(`[ReMinecraft|SCRIPT|] Error in "${event}" handler:`, e);
        }
    });
}
