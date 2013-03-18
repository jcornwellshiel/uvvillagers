package net.uvnode.uvvillagers;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * A time event thrown by UVVillagers.
 *
 * @author James Cornwell-Shiel
 */
public class UVTimeEvent extends Event {

    private static final HandlerList _handlers = new HandlerList();
    private World _world;
    private UVTimeEventType _type;

    /**
     * Default constructor
     *
     * @param world the world this event is for
     * @param type the type of event
     */
    public UVTimeEvent(World world, UVTimeEventType type) {
        _world = world;
        _type = type;
    }

    /**
     * Returns a text description of the event
     *
     * @return string message
     */
    public String getMessage() {
        return _type.toString() + " arrives in " + _world.getName();
    }

    /**
     * Returns the type
     *
     * @return UVTimeEventType
     */
    public UVTimeEventType getType() {
        return _type;
    }

    /**
     * Returns the event world
     *
     * @return World
     */
    public World getWorld() {
        return _world;
    }

    /**
     * Handler list
     *
     * @return HandlerList
     */
    public HandlerList getHandlers() {
        return _handlers;
    }

    /**
     * Handler list
     *
     * @return HandlerList
     */
    public static HandlerList getHandlerList() {
        return _handlers;
    }
}