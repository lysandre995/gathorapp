@echo off
REM Wrapper: delegate to Python generator for cross-platform behavior
SETLOCAL

REM Run the Python generator script (generate_pdf.py) located in the same folder
SET "PY_SCRIPT=%~dp0generate_pdf.py"
IF NOT EXIST "%PY_SCRIPT%" (
    echo [ERROR] Python generator not found: %PY_SCRIPT%
    exit /b 1
)

REM Allow passing through any args
python "%PY_SCRIPT%" %*
SET RC=%ERRORLEVEL%
ENDLOCAL & exit /b %RC%
