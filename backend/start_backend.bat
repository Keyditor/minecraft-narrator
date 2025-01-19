@echo off
call setup_windows.bat
python -m poetry run uvicorn "src.main:app" --port 5000
pause
