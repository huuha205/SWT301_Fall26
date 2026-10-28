@echo off
chcp 65001 > nul
echo ========================================================
echo   SWT301 - RUNNING AUTOMATED TESTS (HEADLESS)
echo ========================================================
call mvn test -Dheadless=true -Dmaven.test.failure.ignore=true
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Test run encountered failures.
)

echo.
echo ========================================================
echo   GENERATING HTML REPORT
echo ========================================================
call mvn surefire-report:report-only -DlinkXRef=false

if exist target\reports\surefire.html (
    echo Opening report: target\reports\surefire.html
    start target\reports\surefire.html
) else if exist target\site\surefire-report.html (
    echo Opening report: target\site\surefire-report.html
    start target\site\surefire-report.html
) else (
    echo [INFO] Check target\reports\ or target\site\ for generated report.
)
