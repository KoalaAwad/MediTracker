@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Export PostgreSQL database from Docker container to a SQL file next to this script (../db-backup)

REM Resolve directories relative to this script
set SCRIPT_DIR=%~dp0
set BACKUP_DIR=%SCRIPT_DIR%..\db-backup
set BACKUP_FILE=%BACKUP_DIR%\meditracker-backup.sql

REM Ensure backup directory exists
if not exist "%BACKUP_DIR%" (
  mkdir "%BACKUP_DIR%"
)

REM Try common container names
set CONTAINER=
for /f %%A in ('docker ps --format "{{.Names}}" ^| findstr /R /C:"^meditracker-db$"') do set CONTAINER=meditracker-db
if "%CONTAINER%"=="" (
  for /f %%A in ('docker ps --format "{{.Names}}" ^| findstr /R /C:"^meditracker-postgres-1$"') do set CONTAINER=meditracker-postgres-1
)

if "%CONTAINER%"=="" (
  echo [ERROR] Could not find a running PostgreSQL container. Tried: meditracker-db, meditracker-postgres-1
  echo Start your stack first (e.g., "docker-compose up -d") and try again.
  exit /b 1
)

REM Perform the dump
set PGPASSWORD=meditracker_pass

echo Exporting database from container: %CONTAINER%
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TS=%%i
set BACKUP_FILE_TS=%BACKUP_DIR%\meditracker-backup-%TS%.sql

REM Use docker exec to dump; write to timestamped file and also update latest copy
 docker exec -e PGPASSWORD=%PGPASSWORD% %CONTAINER% pg_dump -U myuser -d meditracker > "%BACKUP_FILE_TS%"
if errorlevel 1 (
  echo [ERROR] pg_dump failed.
  exit /b 1
)
copy /Y "%BACKUP_FILE_TS%" "%BACKUP_FILE%" >nul

echo [OK] Backup written:
 echo   %BACKUP_FILE_TS%
 echo   %BACKUP_FILE% (latest)
exit /b 0

