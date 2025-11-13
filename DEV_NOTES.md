// TAB KEY TEMPLATES
I want to implement customizable tab key templates.
 * Tab key action should be customizable.  By default, it should inject the 0x09 (\t) character.  But it should be able to inject spaces as well
 * If "spaces" are selected instead of "Tab Character", the number of spaces inserted by a tab keypress should be calculated to match "where are we right now?" to "where would the next issue of tab put us?
 * By default, space tabbing should be locked at 4, with every tab keypress aligned to the next 4 spaces.  For example, pressing tab 3 times on a new line should put the cursor on position 12; typing five characters and pressing tab two times should put the cursor on position 12..
 * A "tab template" allows the user to specify how many spaces get injected each time tab is pressed.
 * 4,8,4 would inject up to four spaces until the cursor is at position 4, up to 8 spaces until the cursor is at position 12, up to 4 spaces until at position 16, and after that, pressing tab uses the default spacing
 * a dot "." should represent "the standard number of spaces", so `.,.,8,.`:
    * with default tab spaces set to 2 should equal `2, 2, 8, 2` and
    * with default tab spaces set to 4 should equal `4, 4, 8, 4`

Tab key templates should be saved by a user-provided name (such as JCL or Cobol) and an editor should be available to allow editing the template after it's been saved.

> When no tab key template is in use, the status bar should state "Tab" or "Spaces (n)"
> When a tab key template is in use, the status bar should simply refer to the template name, such as "JCL".
> Clicking the tab key template from the status bar should give a menu that lets the user pick tabs, spaces, or a custom template name.
> The number of spaces should be configurable from a file menu option, not from the picker




// New document title bar should be the first line of text (up to 16 characters, otherwise 13 characters + `...`)

// • beside file name if unsaved changes are present
// Actually, • does not appear when an existing document gets modified.  This behavior was added and worked...
1. When the document is entirely new and unsaved, put "Unsaved Document" in the title bar.  When saved, change the title bar to the document name.
1. When the document has unsaved changes, put a • to the right of the document name in the title bar.  When saved, remove the •

// Connect to dev tools to read std out?

// !OPTIONAL! ability to attach to file and update the window contents
1. Doing so puts our file into read-only.  We cannot modify the monitored file
1. If the cursor is not at the end of the document, the cursor and viewport should stay in the exact same place as the document loads below.  If the cursor is at the end of the document, loading the new contents should keep the cursor at the bottom of the document (so that we can monitor new details as they arrive)
1. Feature should have an on/off toggle in the status bar

// tab / shift tab on selected block performs indentation


// Wrapping and LRECL seem to work, with one bug:
// When LRECL wraps the line, the last character on the line before a wrap is inaccessible.
// Clicking puts the cursor between the second-last and last character.  Right arrow moves to the next line.  That last character is inaccessible.






// BUG LIST
 * Despite repeated callouts to correct, searching for basic text such as `ipsum` will sometimes highlight the matched text (expected), and will sometimes highlight text following the match (unexpected)

 * "Show special characters" does not show the new line, tab, and space characters

 * Disabling line numbers should remove the line number bar on the left margin

 * Line numbers still do not scroll with text when word wrap is enabled

 * Despite multiple dives in to fix, the project icon.svg still does not show anywhere.  The application still uses the basic Java icon.

 * Despite repeated callouts to correct, searching past the end of the document using F3 / Shift + F3 still does not notify in the task bar!

 * Attempting to open a .pdf resulted in an error.  While I (the human) do not expect to be able to read the contents of non-text files as such, I expect to have their contents loaded and displayed as Notepad would do, as encoded representations of the binary data inside.

 * On ALL dialogues that ask about dropping unsaved changes, Y, N, or ESC should map to "Yes", "No", and "Cancel" respectively.
 
 * Arrow keys should move between the buttons.

 * Closing a document with unsaved changes should prompt, not just disappear.

 * Ctrl + N should open a new instance of Swing Note

 * Ctrl + H deletes a character, it does not display the replace form