@echo off
rem Double-click to set up (first time) and start EcoSwap on this PC.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\windows-first-run.ps1"
