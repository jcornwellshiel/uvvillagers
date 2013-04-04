package net.uvnode.uvvillagers;

import java.util.ArrayList;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A UV village event
 *
 * @author James Cornwell-Shiel
 */
public class UVVillageEvent extends Event {

    private static final HandlerList _handlers = new HandlerList();
    private UVVillage _village;
    private UVVillageEventType _type;
    private String _key;
    private Object _data;

    /**
     * Base constructor
     *
     * @param village
     * @param villageKey
     * @param type
     */
    public UVVillageEvent(UVVillage village, String villageKey, UVVillageEventType type) {
        _village = village;
        _key = villageKey;
        _type = type;
    }

    /**
     * Extra data constructor
     *
     * @param village Village object
     * @param villageKey String unique village ID key
     * @param type UVVillageEventType
     * @param data Object containing additional data
     */
    public UVVillageEvent(UVVillage village, String villageKey, UVVillageEventType type, Object data) {
        _village = village;
        _key = villageKey;
        _type = type;
        _data = data;
    }

    /**
     * Get the human readable message
     *
     * @return message
     */
    public String getMessage() {
        return "Village " + _key + " " + _type.toString();
    }

    /**
     * Gets siege messages associated with the SIEGE_BEGAN type
     *
     * @return message arraylist
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getSiegeMessage() {
        if (_data instanceof ArrayList<?>) {
            return (ArrayList<String>) _data;
        } else {
            return null;
        }
    }
    
    /**
     * Gets message associated with the MERGED type
     *
     * @return message arraylist
     */
    @SuppressWarnings("unchecked")
    public String getMergeMessage() {
        if (_data instanceof String) {
            return (String) _data;
        } else {
            return null;
        }
    }

    /**
     * Get the old name related to a RENAMED event
     *
     * @return old name
     */
    public String getOldName() {
        if (_data instanceof String) {
            return (String) _data;
        } else {
            return null;
        }
    }

    /**
     * Get event type
     *
     * @return event type
     */
    public UVVillageEventType getType() {
        return _type;
    }

    /**
     * Get unique village key
     *
     * @return key
     */
    public String getKey() {
        return _key;
    }

    /**
     * Get village object
     *
     * @return village object
     */
    public UVVillage getVillage() {
        return _village;
    }

    /**
     * Get handlers
     *
     * @return handlers list
     */
    public HandlerList getHandlers() {
        return _handlers;
    }

    /**
     * Get handlers
     *
     * @return handlers list
     */
    public static HandlerList getHandlerList() {
        return _handlers;
    }
}
