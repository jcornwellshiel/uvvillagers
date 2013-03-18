package net.uvnode.uvvillagers;

import java.util.ArrayList;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UVVillageEvent extends Event {

    private static final HandlerList _handlers = new HandlerList();
    private UVVillage _village;
    private UVVillageEventType _type;
    private String _key;
    private Object _data;

    public UVVillageEvent(UVVillage village, String villageKey, UVVillageEventType type) {
        _village = village;
        _key = villageKey;
        _type = type;
    }

    public UVVillageEvent(UVVillage village, String villageKey, UVVillageEventType type, Object data) {
        _village = village;
        _key = villageKey;
        _type = type;
        _data = data;
    }

    public String getMessage() {
        return "Village " + _key + " " + _type.toString();
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getSiegeMessage() {
        if (_data instanceof ArrayList<?>) {
            return (ArrayList<String>) _data;
        } else {
            return null;
        }
    }

    public String getOldName() {
        if (_data instanceof String) {
            return (String) _data;
        } else {
            return null;
        }
    }

    public UVVillageEventType getType() {
        return _type;
    }

    public String getKey() {
        return _key;
    }

    public UVVillage getVillage() {
        return _village;
    }

    public HandlerList getHandlers() {
        return _handlers;
    }

    public static HandlerList getHandlerList() {
        return _handlers;
    }
}
