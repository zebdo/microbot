package net.runelite.client.plugins.microbot.util.cache.util;

/**
 * Defines where cache logging output should be directed.
 * 
 * @author Vox
 * @version 1.0
 */
public enum LogOutputMode {
    /**
     * Log only to console/logger
     */
    CONSOLE_ONLY,
    
    /**
     * Log only to file
     */
    FILE_ONLY,
    
    /**
     * Log to both console and file
     */
    BOTH
}
