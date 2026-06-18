$packFile = "C:\reminecraft\resourcepack\reminecraft-java.zip"
$port = 8080

$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://+:$port/")
try { $listener.Start() } catch {
    exit 1
}

while ($listener.IsListening) {
    try {
        $ctx  = $listener.GetContext()
        $bytes = [System.IO.File]::ReadAllBytes($packFile)
        $ctx.Response.ContentType        = "application/zip"
        $ctx.Response.ContentLength64    = $bytes.Length
        $ctx.Response.Headers["Cache-Control"] = "public, max-age=3600"
        $ctx.Response.OutputStream.Write($bytes, 0, $bytes.Length)
        $ctx.Response.Close()
    } catch { }
}
