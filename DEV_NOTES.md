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

// Connect to dev tools to read std out?

// !OPTIONAL! ability to attach to file and update the window contents
1. Doing so puts our file into read-only.  We cannot modify the monitored file
1. If the cursor is not at the end of the document, the cursor and viewport should stay in the exact same place as the document loads below.  If the cursor is at the end of the document, loading the new contents should keep the cursor at the bottom of the document (so that we can monitor new details as they arrive)
1. Feature should have an on/off toggle in the status bar

// tab / shift tab on selected block performs indentation


// Wrapping and LRECL seem to work, with one bug:
// When LRECL wraps the line, the last character on the line before a wrap is inaccessible.
// Clicking puts the cursor between the second-last and last character.  Right arrow moves to the next line.  That last character is inaccessible.


// Open now has "Open in this window" and "Open in new window" options

// In File Menu, "New" now has a slide-out option "Reopen in new window" that creates a new window, tied to the same parent file (if we're attached to a file).  If file edits are present, additional options are: "keep modifications" or "from original file"


// BUG LIST
 * Replace All breaks the undo stack.  Before a replace all, undo is unlimited.  After a replace all, undo is reset.  Future edits after the replace all should all be avaialble to back out, but nothing prior to the replace all can be backed out.

 * The Windows scaling bug when a 4K monitor is present and scaling is set >100%!  Ugh!  The UI is almost unreadable in that scenario.

 * If accidentally hovering the Line Number gutter when selecting text, all text from the end of the selected range to line 1 character 1 is selected.  Very disruptive if attempting to select multiple lines of text embedded deep in a large document.

 * "Show special characters" does not show the new line, tab, and space characters

 * Despite repeated callouts to correct, searching past the end of the document using F3 / Shift + F3 still does not notify in the task bar!

 * Attempting to open a .pdf resulted in an error.  While I (the human) do not expect to be able to read the contents of non-text files as such, I expect to have their contents loaded and displayed as Notepad would do, as encoded representations of the binary data inside.

 * On ALL dialogues that ask about dropping unsaved changes, Y, N, or ESC should map to "Yes", "No", and "Cancel" respectively.
 
 * Arrow keys should move between the buttons.

 * Closing a document with unsaved changes should prompt, not just disappear.

 * Replace button should instantly replace the next instance.  Replace All does not work.
 
 * When tabbing between fields in the Replace dialog, the new field should enter focus with the text selected
