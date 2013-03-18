package net.uvnode.uvvillagers;


import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UVTimeEvent extends Event {
    private static final HandlerList _handlers = new HandlerList();
    private World _world;
    private UVTimeEventType _type;
 
    public UVTimeEvent(World world, UVTimeEventType type) {
        _world = world;
        _type = type;
    }

    public String getMessage() {
    	return _type.toString() + " arrives in " + _world.getName();
    }
    public UVTimeEventType getType() {
        return _type;
    }
    
    public World getWorld() {
        return _world;
    }
 
    public HandlerList getHandlers() {
        return _handlers;
    }
 
    public static HandlerList getHandlerList() {
        return _handlers;
    }
}