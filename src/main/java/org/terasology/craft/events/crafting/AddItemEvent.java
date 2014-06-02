
package org.terasology.craft.events.crafting;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.AbstractConsumableEvent;

/**
 * @author Small-Jeeper
 */
public class AddItemEvent extends AbstractConsumableEvent {
    private EntityRef target;
    private EntityRef instigator;
    private float percent;

    public AddItemEvent(EntityRef target, EntityRef instigator) {
        this(target, instigator, 0);
    }

    public AddItemEvent(EntityRef target, EntityRef instigator, float percent) {
        this.target = target;
        this.instigator = instigator;
        this.percent = percent;
    }

    public EntityRef getTarget() {
        return target;
    }

    public EntityRef getInstigator() {
        return instigator;
    }

    public float getPercent() {
        return percent;
    }


}