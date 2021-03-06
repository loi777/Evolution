package tgw.evolution.entities.event;

import net.minecraft.entity.MobEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import tgw.evolution.entities.EntityGenericAgeable;

import javax.annotation.Nullable;

@Cancelable
public class BabyEntitySpawnEvent extends Event {
    private final MobEntity father;
    private final MobEntity mother;
    private EntityGenericAgeable child;

    public BabyEntitySpawnEvent(MobEntity father, MobEntity mother, @Nullable EntityGenericAgeable proposedChild) {
        this.father = father;
        this.mother = mother;
        this.child = proposedChild;
    }

    public MobEntity getFather() {
        return this.father;
    }

    public MobEntity getMother() {
        return this.mother;
    }

    @Nullable
    public EntityGenericAgeable getChild() {
        return this.child;
    }

    public void setChild(EntityGenericAgeable proposedChild) {
        this.child = proposedChild;
    }
}