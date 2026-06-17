import mysql from "mysql2/promise";
import fs from "fs";
import path from "path";

let pool: mysql.Pool | null = null;

export async function initializeDatabase(): Promise<void> {
    try {
        const configPath = path.resolve(process.cwd(), "../../config.json");
        if (!fs.existsSync(configPath)) {
            console.warn("[ReMinecraft|BUN|] config.json not found. Database skipped.");
            return;
        }

        const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
        const dbConfig = config.database;

        if (!dbConfig || dbConfig.provider !== "mysql") {
            console.log("[ReMinecraft|BUN|] MySQL not enabled in config.json.");
            return;
        }

        pool = mysql.createPool({
            host: dbConfig.host || "localhost",
            port: dbConfig.port || 3306,
            database: dbConfig.name || "reminecraft",
            user: dbConfig.username || "root",
            password: dbConfig.password || "",
            connectionLimit: dbConfig["pool-size"] || 10,
            waitForConnections: true,
            queueLimit: 0
        });

        const connection = await pool.getConnection();
        console.log("[ReMinecraft|BUN|] Connected to MySQL.");
        connection.release();

    } catch (error) {
        console.error("[ReMinecraft|BUN|] MySQL pool init failed:", error);
    }
}

export function getDatabase(): mysql.Pool {
    if (!pool) {
        throw new Error("MySQL pool not initialized.");
    }
    return pool;
}

export async function query<T>(sql: string, params?: any[]): Promise<T> {
    const db = getDatabase();
    const [results] = await db.execute(sql, params);
    return results as T;
}
