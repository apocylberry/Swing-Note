# Swing Note

(A Java Swing-based note-taking application.)

Do you miss the original Windows Notepad application, before Microsoft started forcing non-configurable "smart" features and AI into it?  Swing Note attempts to recreate that original functionality: basic, but with intent.  A basic text editor is not the tool for every job, but it is the only tool that will do for some jobs.

 * Swing Note will never attempt to reload the active file when it gets changed on disk.
 * Swing Note will never implement AI features
 * Swing Note will never implement auto-correct features
 * Swing Note will never second guess you and your intent
 * Swing Note will never hide basic expectations, such as word wrap and launch with an empty document, deep inside settings menus

 Please note the following:
 
 * I will do my very best to **NEVER** force new features as the shiny new default -- standard Notepad behavior is the target default experience.  However, vibe coding without unit tests may lead to broken basic behaviors or overridden defaults.  Contact me if you have an issue with a default setting getting modified without your consent, because the entire philosphy of this application is to start with the basic default and let **YOU** choose where you want your experience to go from there.
 * I am an experienced .NET programmer, but I am still learning Java
 * I will hand-tune parts that need it, but the majority of this application is being written by Claude Sonnet 3.5
 * My goal in creating this application is not the process of creating the application, it's to get a usable tool to help me in my primary job where old Notepad functionality is required.
 * I am aware that old Notepad is still avaialble on Windows 11 systems.  However, no attempt to make it the default text editor has stuck.  Reboots and Windows Updates revert **MY CHOICE** back to Microsoft's preference on New Notepad.  So to avoid that ugly dance, I'm building an entirely new, ground-up text editor that I can set as default and not have to worry about Windows choosing the "wrong version".
 * I reserve the right to add new features that the basic Notepad application does not support (such as "tab templates" to assist writing JCL or Cobol), but they will be optional and disabled by default.

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run:

```bash
mvn clean package
```

## Running the Application

After building, you can run the application using:

```bash
java -jar target/jsnote-1.0-SNAPSHOT.jar
```

## Development

This is a Maven project with the following structure:

- `src/main/java` - Application source code
- `src/test/java` - Test source code
- `pom.xml` - Maven project configuration
