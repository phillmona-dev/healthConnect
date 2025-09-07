# PowerShell script to fix missing columns
Write-Host "========================================" -ForegroundColor Green
Write-Host "   Fix All Missing Columns" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "This script will fix ALL missing column errors:" -ForegroundColor Yellow
Write-Host ""
Write-Host "PAYERS TABLE:" -ForegroundColor Cyan
Write-Host "- Add is_cbhi column (BOOLEAN, default false)"
Write-Host "- Add index for performance"
Write-Host ""
Write-Host "MEDICATION_DISPENSING TABLE:" -ForegroundColor Cyan
Write-Host "- Add attachment_file_name column (VARCHAR 255)"
Write-Host "- Add attachment_content_type column (VARCHAR 100)"
Write-Host "- Add attachment_data column (BYTEA)"
Write-Host ""

$continue = Read-Host "Do you want to continue? (y/n)"
if ($continue -ne "y") {
    Write-Host "Operation cancelled." -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "Looking for PostgreSQL installation..." -ForegroundColor Yellow

# Find PostgreSQL installation
$psqlPaths = @(
    "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files\PostgreSQL\15\bin\psql.exe",
    "C:\Program Files\PostgreSQL\14\bin\psql.exe",
    "C:\Program Files\PostgreSQL\13\bin\psql.exe",
    "C:\Program Files (x86)\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files (x86)\PostgreSQL\15\bin\psql.exe"
)

$psqlPath = $null
foreach ($path in $psqlPaths) {
    if (Test-Path $path) {
        $psqlPath = $path
        break
    }
}

if ($psqlPath) {
    Write-Host "Found PostgreSQL at: $psqlPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "Running SQL script..." -ForegroundColor Yellow
    
    # Run the SQL script
    & $psqlPath -h localhost -p 5432 -U postgres -d healthConnect -f "scripts/fix-all-missing-columns.sql"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "   ✅ SUCCESS! All columns fixed!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "🚀 NEXT STEPS:" -ForegroundColor Yellow
        Write-Host "1. Restart your HealthConnect application"
        Write-Host "2. Test the failing endpoints"
        Write-Host "3. Both errors should now be resolved"
    } else {
        Write-Host ""
        Write-Host "❌ Error occurred. Please check the output above." -ForegroundColor Red
    }
} else {
    Write-Host "PostgreSQL not found. Please run manually:" -ForegroundColor Red
    Write-Host ""
    Write-Host "QUICK MANUAL FIX - Run these in your PostgreSQL client:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "-- Fix payers table" -ForegroundColor Cyan
    Write-Host "ALTER TABLE payers ADD COLUMN IF NOT EXISTS is_cbhi BOOLEAN DEFAULT false NOT NULL;"
    Write-Host ""
    Write-Host "-- Fix medication_dispensing table" -ForegroundColor Cyan
    Write-Host "ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255);"
    Write-Host "ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(100);"
    Write-Host "ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_data BYTEA;"
}

Write-Host ""
Read-Host "Press Enter to continue"
