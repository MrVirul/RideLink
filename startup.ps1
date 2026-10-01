$ErrorActionPreference = "Stop"

Write-Host "Starting RideLink Microservices..." -ForegroundColor Cyan

function Start-Service {
    param(
        [string]$Name,
        [string]$Path,
        [int]$Port
    )
    Write-Host "Starting $Name on port $Port..." -ForegroundColor Yellow
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd $Path && ..\mvnw.cmd spring-boot:run" -WindowStyle Normal
}

# 1. Service Registry
Start-Service -Name "Eureka Server" -Path "service-registry" -Port 8761
Start-Sleep -Seconds 10 # Wait for Eureka

# 2. API Gateway
Start-Service -Name "API Gateway" -Path "api-gateway" -Port 8080
Start-Sleep -Seconds 5

# 3. Microservices
Start-Service -Name "Account Service" -Path "account-service" -Port 8081

Write-Host "All necessary services (Registry, Gateway, Account) are starting in separate windows!" -ForegroundColor Green
Write-Host "To stop them, close the opened command prompt windows." -ForegroundColor Yellow
