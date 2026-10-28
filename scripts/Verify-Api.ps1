#requires -Version 7.0
[CmdletBinding()]
param(
    [string] $BaseUri = 'http://localhost:8080',
    [string] $OutputPath
)
$ErrorActionPreference = 'Stop'
$checks = [System.Collections.Generic.List[string]]::new()

function Assert-Condition([bool] $Condition, [string] $Message) {
    if (-not $Condition) { throw "Verification failed: $Message" }
    $checks.Add($Message)
}

function Invoke-Api([string] $Method, [string] $Path, [object] $Body, [string] $Token,
                    [string] $Key, [int] $Expected = 200) {
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    if ($Key) { $headers['Idempotency-Key'] = $Key }
    $request = @{ Method=$Method; Uri="$BaseUri$Path"; Headers=$headers; SkipHttpErrorCheck=$true; TimeoutSec=20 }
    if ($null -ne $Body) {
        $request.Body = ConvertTo-Json -InputObject $Body -Compress
        $request.ContentType = 'application/json'
    }
    $response = Invoke-WebRequest @request
    if ($response.StatusCode -ne $Expected) {
        throw "Unexpected HTTP status for $Method $Path : $($response.StatusCode), expected $Expected"
    }
    $content = if ($response.Content -is [byte[]]) { [Text.Encoding]::UTF8.GetString($response.Content) } else { [string]$response.Content }
    if ($response.Headers['Content-Type'] -match 'json') { return $content | ConvertFrom-Json }
    return $content
}

$runId = [Guid]::NewGuid().ToString('N')
$password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$health = Invoke-Api -Method GET -Path '/actuator/health'
Assert-Condition ($health.status -eq 'UP') 'Application and database health are UP'
$openapi = Invoke-Api -Method GET -Path '/v3/api-docs'
Assert-Condition ($null -ne $openapi.paths.'/api/v1/transactions/transfers') 'Live OpenAPI includes transfer endpoint'
Assert-Condition ($null -ne $openapi.security[0].bearerAuth) 'OpenAPI declares bearer authentication for protected operations'
$users = @()
foreach ($label in @('sender', 'recipient')) {
    $email = "$label-$runId@example.test"
    $null = Invoke-Api -Method POST -Path '/api/v1/auth/register' -Expected 201 -Body @{
        email=$email; password=$password; displayName="Demo $label"
    }
    $login = Invoke-Api -Method POST -Path '/api/v1/auth/login' -Body @{email=$email; password=$password}
    $account = Invoke-Api -Method POST -Path '/api/v1/accounts' -Expected 201 -Token $login.accessToken -Body @{name="Demo $label"}
    $users += @{Token=$login.accessToken; Account=$account}
}
$sender = $users[0]; $recipient = $users[1]
$null = Invoke-Api -Method POST -Path '/api/v1/transactions/deposits' -Expected 201 -Token $sender.Token -Key "deposit-$runId" -Body @{
    accountId=$sender.Account.id; amount='100.00'; category='SALARY'; description='Simulated initial funds'
}
$transferRequest = @{sourceAccountId=$sender.Account.id; targetAccountId=$recipient.Account.id; amount='35.25'; category='OTHER'; description='Internal demo transfer'}
$operation = Invoke-Api -Method POST -Path '/api/v1/transactions/transfers' -Expected 201 -Token $sender.Token -Key "transfer-$runId" -Body $transferRequest
$replay = Invoke-Api -Method POST -Path '/api/v1/transactions/transfers' -Expected 201 -Token $sender.Token -Key "transfer-$runId" -Body $transferRequest
Assert-Condition ($operation.id -eq $replay.id -and $operation.createdAt -eq $replay.createdAt) 'Idempotent retry returns original operation and timestamp'
$sourceBalance = Invoke-Api -Method GET -Path "/api/v1/accounts/$($sender.Account.id)" -Token $sender.Token
$targetBalance = Invoke-Api -Method GET -Path "/api/v1/accounts/$($recipient.Account.id)" -Token $recipient.Token
Assert-Condition ([decimal]$sourceBalance.balance -eq [decimal]'64.75' -and [decimal]$targetBalance.balance -eq [decimal]'35.25') 'Cross-user transfer preserves total balance of BRL 100.00'
$null = Invoke-Api -Method GET -Path "/api/v1/accounts/$($sender.Account.id)" -Token $recipient.Token -Expected 404
$null = Invoke-Api -Method GET -Path '/api/v1/accounts' -Expected 401
Assert-Condition $true 'Unauthorized requests and foreign account access are rejected'
$null = Invoke-Api -Method POST -Path '/api/v1/transactions/withdrawals' -Token $sender.Token -Expected 201 -Key "withdraw-$runId" -Body @{
    accountId=$sender.Account.id; amount='10.00'; category='FOOD'; description='=simulated spreadsheet formula'
}
$summary = Invoke-Api -Method GET -Path "/api/v1/accounts/$($sender.Account.id)/summary" -Token $sender.Token
Assert-Condition ([decimal]$summary.income -eq 100 -and [decimal]$summary.expenses -eq [decimal]'45.25') 'Summary reconciles credits and debits including transfer outflow'
$csv = Invoke-Api -Method GET -Path "/api/v1/accounts/$($sender.Account.id)/statement.csv" -Token $sender.Token
Assert-Condition ($csv.Contains("'=simulated spreadsheet formula")) 'CSV export neutralizes formula-like descriptions'
$entries = Invoke-Api -Method GET -Path "/api/v1/accounts/$($recipient.Account.id)/entries" -Token $recipient.Token
Assert-Condition ($entries.totalElements -eq 1 -and $entries.content[0].direction -eq 'CREDIT') 'Recipient sees incoming posting in their own history'
$null = Invoke-Api -Method PATCH -Path "/api/v1/accounts/$($sender.Account.id)/status" -Token $sender.Token -Body @{status='INACTIVE'}
$null = Invoke-Api -Method POST -Path '/api/v1/transactions/deposits' -Expected 409 -Token $sender.Token -Key "inactive-$runId" -Body @{
    accountId=$sender.Account.id; amount='1.00'; category='OTHER'; description='Must be rejected'
}
Assert-Condition $true 'Inactive accounts reject new postings'
$report = [ordered]@{
    executedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
    baseUri = $BaseUri
    result = 'PASS'
    checks = @($checks)
    example = @{
        operation = $operation
        senderSummary = $summary
        recipientBalance = $targetBalance.balance
        csv = $csv
    }
}
$serialized = ConvertTo-Json -InputObject $report -Depth 8
if ($OutputPath) { [IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), $serialized, [Text.UTF8Encoding]::new($false)) }
Write-Output $serialized
