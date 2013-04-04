package net.uvnode.uvvillagers;

/**
 *
 * @author James Cornwell-Shiel
 */
public enum UVVillageEventType {

    /**
     * A new village was discovered.
     */
    DISCOVERED,
    /**
     * A village was abandoned.
     */
    ABANDONED,
    /**
     * A siege has begun in a village.
     */
    SIEGE_BEGAN,
    /**
     * A siege has ended in a village.
     */
    SIEGE_ENDED,
    /**
     * A player visited a village.
     */
    VISITED,
    /**
     * Updates were found in a village.
     */
    UPDATED,
    /**
     * A village was renamed.
     */
    RENAMED,
    /**
     * A village was merged into another village.
     */
    MERGED
}
