package tgw.evolution.capabilities;

import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public class SimpleCapabilityProvider<HANDLER> implements ICapabilityProvider {

    /**
     * The {@link Capability} instance to provide the handler for.
     */
    protected final Capability<HANDLER> capability;

    /**
     * The {@link Direction} to provide the handler for.
     */
    protected final Direction facing;

    /**
     * The handler instance to provide.
     */
    protected final HANDLER instance;

    /**
     * A lazy optional containing handler instance to provide.
     */
    protected final LazyOptional<HANDLER> lazyOptional;

    public SimpleCapabilityProvider(Capability<HANDLER> capability, @Nullable Direction facing, @Nullable HANDLER instance) {
        this.capability = capability;
        this.facing = facing;

        this.instance = instance;

        if (this.instance != null) {
            this.lazyOptional = LazyOptional.of(() -> this.instance);
        }
        else {
            this.lazyOptional = LazyOptional.empty();
        }
    }

    /**
     * Retrieves the handler for the capability requested on the specific side.
     * The return value CAN be null if the object does not support the capability.
     * The return value CAN be the same for multiple faces.
     *
     * @param capability The capability to check
     * @param facing     The Side to check from:
     *                   CAN BE NULL. Null is defined to represent 'internal' or 'self'
     * @return A lazy optional containing the handler, if this object supports the capability.
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        return this.capability.orEmpty(capability, this.lazyOptional);
    }

    /**
     * Get the {@link Capability} instance to provide the handler for.
     *
     * @return The Capability instance
     */
    public final Capability<HANDLER> getCapability() {
        return this.capability;
    }

    /**
     * Get the {@link Direction} to provide the handler for.
     *
     * @return The Direction to provide the handler for
     */
    @Nullable
    public Direction getFacing() {
        return this.facing;
    }

    /**
     * Get the handler instance.
     *
     * @return A lazy optional containing the handler instance
     */
    @Nullable
    public final HANDLER getInstance() {
        return this.instance;
    }
}
