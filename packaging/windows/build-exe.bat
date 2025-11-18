@echo off


REM SwingNote Windows Executable Build Script
REM Automates the complete build process from source to executable

echo ==========================================
echo SwingNote Windows Executable Build Script
echo ==========================================
echo.

REM Navigate to project root (two levels up from packaging/windows)
cd /d "%~dp0..\.."

REM Check if we're in the correct project root
if not exist "pom.xml" (
    echo Error: Could not find pom.xml in project root.
    echo Make sure this script is in packaging/windows/ directory.
    pause
    exit /b 1
)

REM Step 1: Clean build
echo [1/4] Building JAR with Maven...
mvn clean package
if errorlevel 1 (
    echo [1/4] Building JAR with Maven Daemon...
    mvnd clean package
    if errorlevel 1 (
        echo ERROR: Maven build failed!
        pause
        exit /b 1
    )
)
echo + JAR build successful

REM Step 2: Remove old executable
echo [2/4] Cleaning previous executable build...
if exist dist (
    rmdir /s /q dist
    echo + Removed old dist directory
) else (
    echo + No previous build found
)

REM Step 3: Verify icon exists
if not exist "src\main\resources\SwingNote.ico" (
    echo ERROR: Icon file not found at src\main\resources\SwingNote.ico
    echo Please ensure the icon resources are present.
    pause
    exit /b 1
)
echo + Icon file verified

REM Step 4: Create executable
echo [3/4] Creating Windows executable with jpackage...
jpackage --input target --name SwingNote --main-jar jsnote-1.0-SNAPSHOT.jar --main-class org.foss.apocylberry.jsnote.MainApp --type app-image --dest dist --icon src\main\resources\SwingNote.ico
if errorlevel 1 (
    echo ERROR: jpackage failed!
    echo Make sure you have JDK 14+ with jpackage tool.
    pause
    exit /b 1
)
echo + Executable created successfully

REM Step 5: Verify executable
echo [4/4] Verifying executable...
if exist "dist\SwingNote\SwingNote.exe" (
    echo + SwingNote.exe created successfully
) else (
    echo ERROR: SwingNote.exe was not created
    pause
    exit /b 1
)

REM Calculate package size (simplified)
set "SIZE=~150 MB"

echo.
echo ==========================================
echo BUILD COMPLETE!
echo ==========================================
echo.
echo Executable location - dist\SwingNote\SwingNote.exe
echo Package size - %SIZE%
echo.
echo The executable includes -
echo - Native Windows .exe with custom purple icon
echo - Bundled Java runtime (no Java installation required)  
echo - Full "Open With" integration support
echo - Command line file argument support
echo.

REM Optional installation step
echo Would you like to install SwingNote to a specific location? (y/n)
set /p "INSTALL_CHOICE=Enter choice (y/n) - "

if /i "%INSTALL_CHOICE%"=="y" goto :install
goto :skip_install

:install
echo.
echo Enter installation directory (or press Enter for default - %APPDATA%\SwingNote):
echo NOTE: Do not use quotes around the path
set /p "CUSTOM_DIR=Install to - "

REM Remove any quotes the user might have added (only if not empty)
if not "%CUSTOM_DIR%"=="" (
    set "CUSTOM_DIR=%CUSTOM_DIR:"=%"
)

if "%CUSTOM_DIR%"=="" (
    set "INSTALL_DIR=%APPDATA%\SwingNote"
) else (
    set "INSTALL_DIR=%CUSTOM_DIR%"
)

echo.
echo Installing SwingNote to - %INSTALL_DIR%

REM Create installation directory
if not exist "%INSTALL_DIR%" (
    mkdir "%INSTALL_DIR%"
    if errorlevel 1 (
        echo ERROR: Could not create directory %INSTALL_DIR%
        echo You can manually copy dist\SwingNote\ to your preferred location.
        goto :end
    )
)

REM Copy the entire SwingNote folder
echo Copying SwingNote application...
xcopy /E /Y "dist\SwingNote\*" "%INSTALL_DIR%\"

if errorlevel 1 (
    echo ERROR: Failed to copy files to %INSTALL_DIR%
    echo You can manually copy dist\SwingNote\ to your preferred location.
    goto :end
)

echo.
echo [SUCCESS] SwingNote installed successfully!
echo [SUCCESS] Executable location - %INSTALL_DIR%\SwingNote.exe
echo.
echo To use with "Open With" -
echo 1. Right-click any text file
echo 2. Select "Open with" ^> "Choose another app"  
echo 3. Click "Look for another app on this PC"
echo 4. Navigate to - %INSTALL_DIR%\SwingNote.exe
echo 5. Leave "Always use this app" UNCHECKED to keep it as option only
echo.

REM Offer to test the installation
echo Would you like to test the installation? (y/n)
set /p "TEST_CHOICE=Test now (y/n) - "

if /i "%TEST_CHOICE%"=="y" (
    echo Testing SwingNote...
    "%INSTALL_DIR%\SwingNote.exe"
)
goto :end

:skip_install
echo.
echo Skipping installation. You can -
echo - Use SwingNote directly from - dist\SwingNote\SwingNote.exe
echo - Manually copy dist\SwingNote\ folder to any location
echo - Use "Open With" ^> "Choose another app" to browse to SwingNote.exe

:end
echo.
pause