export interface PlayerInfo {
    name: string;
    ping: number;
}

export interface ServerStats {
    tps: number;
    online: number;
    max_players: number;
    ram_used_mb: number;
    ram_max_mb: number;
    avg_ping: number;
    players: PlayerInfo[];
    timestamp: number;
}

const HISTORY_SIZE = 60;

let current: ServerStats = {
    tps: 0,
    online: 0,
    max_players: 0,
    ram_used_mb: 0,
    ram_max_mb: 0,
    avg_ping: 0,
    players: [],
    timestamp: Date.now(),
};

const tpsHistory: number[] = [];

export function updateStats(data: Omit<ServerStats, "timestamp">): void {
    current = { ...data, timestamp: Date.now() };
    tpsHistory.push(data.tps);
    if (tpsHistory.length > HISTORY_SIZE) tpsHistory.shift();
}

export function getStats(): ServerStats {
    return current;
}

export function getTpsHistory(): number[] {
    return [...tpsHistory];
}
