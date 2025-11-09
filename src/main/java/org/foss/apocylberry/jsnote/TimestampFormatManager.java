package org.foss.apocylberry.jsnote;

import java.util.*;
import java.util.prefs.*;

public class TimestampFormatManager {
    private static final String DEFAULT_FORMAT = "HH:mm M/d/yyyy";
    private static final String PREF_FORMATS_KEY = "timestampFormats";
    private static final String PREF_SEPARATOR = "||";
    private static final int MAX_RECENT_FORMATS = 10;
    
    private final Preferences prefs;
    private final LinkedList<String> recentFormats;
    
    public TimestampFormatManager(Preferences prefs) {
        this.prefs = prefs;
        this.recentFormats = new LinkedList<>();
        loadFormats();
    }
    
    private void loadFormats() {
        String saved = prefs.get(PREF_FORMATS_KEY, "");
        if (!saved.isEmpty()) {
            String[] formats = saved.split("\\Q" + PREF_SEPARATOR + "\\E");
            recentFormats.addAll(Arrays.asList(formats));
        }
    }
    
    private void saveFormats() {
        String joined = String.join(PREF_SEPARATOR, recentFormats);
        prefs.put(PREF_FORMATS_KEY, joined);
    }
    
    public void addFormat(String format) {
        // Remove if already exists (to move to front)
        recentFormats.remove(format);
        
        // Add to front
        recentFormats.addFirst(format);
        
        // Trim to max size
        while (recentFormats.size() > MAX_RECENT_FORMATS) {
            recentFormats.removeLast();
        }
        
        saveFormats();
    }
    
    public List<String> getRecentFormats() {
        return Collections.unmodifiableList(recentFormats);
    }
    
    public void clearFormats() {
        recentFormats.clear();
        prefs.remove(PREF_FORMATS_KEY);
    }
    
    public String getDefaultFormat() {
        return DEFAULT_FORMAT;
    }
    
    // Utility method to process newline escape sequences
    public static String processEscapeSequences(String format) {
        return format.replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r");
    }
    
    // Method to display escape sequences in human-readable form
    public static String displayEscapeSequences(String format) {
        return format.replace("\n", "\\n")
                    .replace("\t", "\\t")
                    .replace("\r", "\\r");
    }
}
