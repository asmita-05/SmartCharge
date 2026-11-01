$url = 'http://localhost:8080/'
$deadline = (Get-Date).AddMinutes(2)

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-WebRequest -Uri $url -Method Get -TimeoutSec 3 -UseBasicParsing
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
            Start-Process $url
            exit 0
        }
    }
    catch {
        # The backend is still starting. Retry briefly without opening a broken tab.
    }

    Start-Sleep -Seconds 2
}

exit 1
