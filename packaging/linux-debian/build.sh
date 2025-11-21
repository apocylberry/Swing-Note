#!/bin/bash
# Run with:     `./packaging/linux-debian/build.sh`
# Installs to:  `/opt/swingnote`
#
# To uninstall,
#   FIND THE PACKAGE NAME
#   > `dpkg -l | grep swingnote`
#   > ii  swingnote                                                1.0-SNAPSHOT                               amd64        SwingNote
#     the package name is the second value, "swingnote" from the sample response above
#
#   CHOOSE:
#   > `sudo dpkg -r swingnote`   (or whatever the package name is listed as in step 1)  # UNINSTALL JUST THE BINARY
#   > `sudo dpkg -P swingnote`   (or whatever the package name is listed as in step 1)  # UNINSTALL AND REMOVE CONFIGURATION DETAILS
#
#   REMOVE THE SYMBOLIC LINK
#   > `sudo rm /usr/local/bin/SwingNote`


# METHOD DECLARATIONS:
doBuild() {
    # Step 1 — Build the JAR
    # =======================================================
    echo "[2/5] Building JAR with mvn..."
    if ! mvn clean package; then
        echo "Maven failed — trying mvnd..."
        if ! mvnd clean package; then
            echo "ERROR: Build failed!"
            exit 1
        fi
    fi
    echo "+ Build successful"
    echo


    # Step 2 — Clean previous build
    # =======================================================
    echo "[3/5] Cleaning old dist folder..."
    if [[ -d "dist" ]]; then
        rm -rf dist
        echo "+ Removed old dist directory"
    else
        echo "+ No previous build found"
    fi
    echo


    # Step 3 — Verify icon exists
    # =======================================================
    echo "[4/5] Checking icon..."
    if [[ ! -f "$ICON_PATH" ]]; then
        echo "ERROR: Icon not found at $ICON_PATH"
        exit 1
    fi
    echo "+ Icon exists"
    echo


    # Step 4 — Create DEB package
    # =======================================================
    echo "[5/5] Creating DEB package with jpackage..."
    if [[ ! -f "target/$MAIN_JAR" ]]; then
        echo "ERROR: Expected JAR missing: target/$MAIN_JAR"
        echo "Did the build complete? Check your JAR naming."
        exit 1
    fi

    jpackage \
        --linux-deb-maintainer "apocylberry@github" \
        --type deb \
        --input target \
        --dest dist \
        --name SwingNote \
        --app-version "$VERSION" \
        --main-jar "$MAIN_JAR" \
        --main-class org.foss.apocylberry.jsnote.MainApp \
        --icon "$ICON_PATH" \
        --install-dir /opt

    echo "+ DEB package created"
    echo

    if [[ ! -f "$DEB_FILE" ]]; then
        echo "ERROR: DEB package was not created!"
        exit 1
    fi

    echo "=========================================="
    echo " BUILD COMPLETE!"
    echo "=========================================="
    echo
    echo "DEB Package Location:"
    echo "  $DEB_FILE"
    echo
}



set -e  # Stop on first error

echo "=========================================="
echo " SwingNote Linux DEB Build Script"
echo "=========================================="
echo

# Navigate to project root (two levels up from packaging/linux-debian)
cd "$(dirname "$0")/../.."

# Check pom.xml exists
if [[ ! -f "pom.xml" ]]; then
    echo "ERROR: pom.xml not found in project root!"
    echo "Make sure this script is inside packaging/linux-debian/"
    exit 1
fi

# Extract version
echo "[1/5] Detecting version from pom.xml..."
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if [[ -z "$VERSION" ]]; then
    echo "ERROR: Could not read version from pom.xml"
    exit 1
fi
echo
echo
echo "BUILD DETAILS"
echo "+ Version: $VERSION"

# SESSION VARIABLES
MAIN_JAR="jsnote-$VERSION.jar"
DEB_FILE="dist/swingnote_${VERSION}_amd64.deb"
ICON_PATH="src/main/resources/SwingNote-256.png"
BUILD_CHOICE=""
INSTALL_CHOICE=""
echo "+ MainJar: $MAIN_JAR"
echo "+ Package: $DEB_FILE"
echo "+ IconPth: $ICON_PATH"
echo

read -p "Build the DEB package now? (y/n) " BUILD_CHOICE
if [[ "$BUILD_CHOICE" == "y" || "$BUILD_CHOICE" == "Y" ]]; then
    doBuild
else
    echo "Skipping package."
fi


# =======================================================
# Optional — ask user to install the DEB
read -p "Install the DEB package now? (y/n) " INSTALL_CHOICE

if [[ "$INSTALL_CHOICE" == "y" || "$INSTALL_CHOICE" == "Y" ]]; then
    if [[ ! -f "$DEB_FILE" ]]; then
        read -p "ERROR: DEB package not found!  Build it? (y/n) " BUILD_CHOICE
        if [[ "$BUILD_CHOICE" == "y" || "$BUILD_CHOICE" == "Y" ]]; then
            doBuild
        else
            exit 1
        fi
    fi

    echo
    echo "Installing package..."
    sudo dpkg -i "$DEB_FILE"
    sudo ln -sf /opt/SwingNote/bin/SwingNote /usr/local/bin/SwingNote

    echo
    echo "Installation complete."
else
    echo "Skipping installation."
    echo "You can install manually using:"
    echo "  sudo dpkg -i \"$DEB_FILE\""
fi

echo
echo "Done."
