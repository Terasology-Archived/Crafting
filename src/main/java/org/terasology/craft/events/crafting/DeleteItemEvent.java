
package org.terasology.craft.events.crafting;

import org.terasology.entitySystem.event.AbstractConsumableEvent;

/**
 * @author Small-Jeeper
 */
public class DeleteItemEvent extends AbstractConsumableEvent {
    private float percent;

    public DeleteItemEvent() {
        this(0);
    }

    public DeleteItemEvent(float percent) {
        this.percent = percent;
    }

    public float getPercent() {
        return percent;
    }
}