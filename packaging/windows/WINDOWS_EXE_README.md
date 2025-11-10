
# SwingNote Windows Executable Build Guide

## 🎯 **Purpose**
This guide provides step-by-step instructions for building SwingNote from source code into a distributable Windows executable. It is designed for AI assistants (Copilot) to automate the packaging process for releases.

## 📋 **Prerequisites**
- Java 21+ JDK installed
- Maven Daemon (mvnd) or Maven (mvn) available
- Windows 10/11 development environment
- jpackage tool (included with JDK 14+)

## 🛠️ **Automated Build Process**

### Single-Command Build
```bash
# Run the automated build script from anywhere in the project
.\packaging\windows\build-exe.bat

# OR navigate to the packaging directory
cd packaging\windows
.\build-exe.bat
```

This script handles the complete process:
1. **Clean and Build JAR** with Maven Daemon
2. **Create Windows Executable** with jpackage  
3. **Optional Installation** to custom location
4. **Testing and Verification**

### Manual Build Steps (if needed)

#### Step 1: Clean and Build JAR
```bash
mvnd clean package
```

#### Step 2: Create Windows Executable  
```bash
jpackage --input target \
         --name SwingNote \
         --main-jar jsnote-1.0-SNAPSHOT.jar \
         --main-class org.foss.apocylberry.jsnote.MainApp \
         --type app-image \
         --dest dist \
         --icon src/main/resources/SwingNote.ico
```

### Generated Icon Resources
The project includes pre-generated icon files:
- `src/main/resources/SwingNote.ico` - Windows ICO format
- `src/main/resources/SwingNote-256.png` - Large PNG icon  
- `src/main/resources/SwingNote-32.png` - Small PNG icon

**Icon Details:**
- Purple background (#673AB7) with rounded corners
- Light purple musical note symbol (#EDE7F6)
- Generated from `createApplicationIcon()` method in MainApp.java

### Build Output Structure
```
dist/SwingNote/
├── SwingNote.exe          # Native Windows executable (~500KB)
├── app/                   # Application files and resources
│   ├── jsnote-1.0-SNAPSHOT.jar
│   ├── SwingNote.cfg
│   └── classes/           # Icon resources and compiled classes
└── runtime/               # Bundled Java runtime (~150MB)
    ├── bin/
    ├── lib/
    └── [other JRE components]
```

### Installation Options
The `build-exe.bat` script offers three options:

1. **Skip Installation** - Use directly from `dist\SwingNote\SwingNote.exe`
2. **Default Location** - Install to `%USERPROFILE%\SwingNote`  
3. **Custom Location** - Install to user-specified directory (no quotes needed for paths with spaces)

## 📦 **Distribution Package**

### For End Users:
1. **Zip the entire `dist/SwingNote/` folder**
2. **Provide Windows "Open With" setup instructions**
3. **No additional installation required** - self-contained executable

### Distribution Size:
- **Complete package**: ~150MB (includes bundled Java runtime)
- **Executable only**: ~500KB (requires runtime folder)
- **Runtime cannot be separated** - jpackage creates integrated bundle

### Windows "Open With" Setup:
1. Right-click any text file
2. Select "Open with" → "Choose another app"
3. Click "Look for another app on this PC"  
4. Navigate to `SwingNote.exe` (wherever installed)
5. **Leave "Always use this app" UNCHECKED** to keep as option only

## 🔍 **Verification Steps**

### Test Basic Functionality:
```bash
# Test executable launches
./dist/SwingNote/SwingNote.exe

# Test file argument handling
./dist/SwingNote/SwingNote.exe "sample.txt"
```

### Verify Icon Integration:
1. Right-click any text file
2. Select "Open with" → "Choose another app"
3. Browse to `SwingNote.exe`
4. Verify purple musical note icon appears in dialog

## 🏗️ **Build Automation Script (build-exe.bat)**

The project includes a fully automated build script that handles:
- ✅ Maven build with dependency shading
- ✅ jpackage executable creation with custom icon  
- ✅ Build verification and size reporting
- ✅ Optional installation to custom location
- ✅ Windows "Open With" setup instructions
- ✅ Error handling and user guidance

**Usage:**
```cmd
# From project root:
.\packaging\windows\build-exe.bat

# From packaging\windows directory:
.\build-exe.bat
```

**Interactive Options:**
1. **Skip installation** (type `n`) - Use executable from build directory
2. **Install to default** (type `y` + Enter) - Installs to `%USERPROFILE%\SwingNote`  
3. **Custom installation** (type `y` + custom path) - Installs to specified location

**Features:**
- Handles paths with spaces (no quotes needed)
- Strips quotes automatically if user adds them
- Provides testing option after installation
- Clear error messages and recovery instructions

## 📝 **Key Technical Notes**

### Application Features:
- **Command line support**: Accepts file path as first argument
- **Custom icon**: Purple musical note design from MainApp.createApplicationIcon()
- **Self-contained**: No external Java installation required
- **Windows integration**: Full "Open With" and file association support

### Build Requirements:
- **Java 21+**: Required for jpackage and application compilation
- **Windows platform**: jpackage creates platform-specific executables
- **Icon format**: Must be .ICO for Windows jpackage integration

### Code Changes for Executable:
- MainApp.main() enhanced to handle file arguments
- Added openFile(File) overload for programmatic file opening
- Icon resources embedded in JAR for jpackage extraction

This guide ensures consistent, reproducible builds of SwingNote.exe from source code for distribution.