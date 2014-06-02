package org.terasology.craft.events.crafting;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.AbstractConsumableEvent;

public class ChangeLevelEvent extends AbstractConsumableEvent {
    private float nextLevel;
    private EntityRef instigator;

    public ChangeLevelEvent(float nextLevel, EntityRef instigator) {
        this.nextLevel = nextLevel;
        this.instigator = instigator;
    }

    public boolean isDecreaseEvent() {
        return nextLevel < 0;
    }

}
