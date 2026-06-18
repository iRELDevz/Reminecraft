// ============================================================
// example-hud.ts — contoh script ReMinecraft Scripting Engine
//
// Global yang tersedia dari engine:
//   $on(event, handler) — subscribe ke event
//   $off(event, handler) — unsubscribe dari event
//   $stats()            — ambil stats server saat ini
//   $log(msg)           — cetak pesan ke console
// ============================================================

// Jalankan saat stats baru diterima dari server Java (tiap ~1 detik)
$on("stats", (stats) => {
    const tps  = stats.tps.toFixed(1);
    const ping = stats.avg_ping;
    const ram  = `${stats.ram_used_mb}MB/${stats.ram_max_mb}MB`;
    const pl   = `${stats.online}/${stats.max_players}`;

    $log(`TPS: ${tps} | Ping: ${ping}ms | RAM: ${ram} | Players: ${pl}`);
});

// Peringatan khusus saat TPS turun di bawah 18
$on("tps_warning", (stats) => {
    $log(`⚠ LOW TPS: ${stats.tps.toFixed(1)} — server sedang lag!`);
});

$log("example-hud.ts aktif — mendengarkan stats server...");
