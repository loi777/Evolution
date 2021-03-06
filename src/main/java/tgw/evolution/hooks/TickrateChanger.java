package tgw.evolution.hooks;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.PacketDistributor;
import tgw.evolution.Evolution;
import tgw.evolution.init.EvolutionNetwork;
import tgw.evolution.network.PacketSCChangeTickrate;

public final class TickrateChanger {
    public static final float DEFAULT_TICKRATE = 20.0f;
    public static final float MIN_TICKRATE = 0.1F;
    public static final float MAX_TICKRATE = 1_000.0f;
    private static float currentTickrate = 20.0f;
    private static long mspt = 50L;

    private TickrateChanger() {
    }

    public static float getCurrentTickrate() {
        return currentTickrate;
    }

    /**
     * Hooks from {@link MinecraftServer#run()}
     */
    @EvolutionHook
    public static long getMSPT() {
        return mspt;
    }

    public static boolean updateServerTickrate(float tickrate) {
        if (tickrate == currentTickrate) {
            return false;
        }
        Evolution.LOGGER.info("Updating server tickrate to " + tickrate);
        currentTickrate = tickrate;
        mspt = (long) (1_000L / tickrate);
        EvolutionNetwork.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSCChangeTickrate(tickrate));
        return true;
    }
}
