package tgw.evolution.init;

import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import tgw.evolution.Evolution;
import tgw.evolution.network.*;

public final class EvolutionNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(Evolution.location("main"),
                                                                                  () -> PROTOCOL_VERSION,
                                                                                  PROTOCOL_VERSION::equals,
                                                                                  PROTOCOL_VERSION::equals);

    private static int id;

    private EvolutionNetwork() {
    }

    private static int increaseId() {
        id++;
        return id;
    }

    public static void registerMessages() {
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCUpdateChunkStorage.class,
                                 PacketSCUpdateChunkStorage::encode,
                                 PacketSCUpdateChunkStorage::decode,
                                 PacketSCUpdateChunkStorage::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCHandAnimation.class,
                                 PacketSCHandAnimation::encode,
                                 PacketSCHandAnimation::decode,
                                 PacketSCHandAnimation::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSOpenExtendedInventory.class,
                                 PacketCSOpenExtendedInventory::encode,
                                 PacketCSOpenExtendedInventory::decode,
                                 PacketCSOpenExtendedInventory::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSPlayerAttack.class,
                                 PacketCSPlayerAttack::encode,
                                 PacketCSPlayerAttack::decode,
                                 PacketCSPlayerAttack::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSSetKnappingType.class,
                                 PacketCSSetKnappingType::encode,
                                 PacketCSSetKnappingType::decode,
                                 PacketCSSetKnappingType::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCOpenKnappingGui.class,
                                 PacketSCOpenKnappingGui::encode,
                                 PacketSCOpenKnappingGui::decode,
                                 PacketSCOpenKnappingGui::handle);
        INSTANCE.registerMessage(increaseId(), PacketCSSetProne.class, PacketCSSetProne::encode, PacketCSSetProne::decode, PacketCSSetProne::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSUpdatePuzzle.class,
                                 PacketCSUpdatePuzzle::encode,
                                 PacketCSUpdatePuzzle::decode,
                                 PacketCSUpdatePuzzle::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCOpenMoldingGui.class,
                                 PacketSCOpenMoldingGui::encode,
                                 PacketSCOpenMoldingGui::decode,
                                 PacketSCOpenMoldingGui::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSSetMoldingType.class,
                                 PacketCSSetMoldingType::encode,
                                 PacketCSSetMoldingType::decode,
                                 PacketCSSetMoldingType::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSUpdateSchematicBlock.class,
                                 PacketCSUpdateSchematicBlock::encode,
                                 PacketCSUpdateSchematicBlock::decode,
                                 PacketCSUpdateSchematicBlock::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSPlayerFall.class,
                                 PacketCSPlayerFall::encode,
                                 PacketCSPlayerFall::decode,
                                 PacketCSPlayerFall::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSChangeBlock.class,
                                 PacketCSChangeBlock::encode,
                                 PacketCSChangeBlock::decode,
                                 PacketCSChangeBlock::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketCSImpactDamage.class,
                                 PacketCSImpactDamage::encode,
                                 PacketCSImpactDamage::decode,
                                 PacketCSImpactDamage::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCUpdateCameraTilt.class,
                                 PacketSCUpdateCameraTilt::encode,
                                 PacketSCUpdateCameraTilt::decode,
                                 PacketSCUpdateCameraTilt::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCChangeTickrate.class,
                                 PacketSCChangeTickrate::encode,
                                 PacketSCChangeTickrate::decode,
                                 PacketSCChangeTickrate::handle);
        INSTANCE.registerMessage(increaseId(),
                                 PacketSCRemoveEffect.class,
                                 PacketSCRemoveEffect::encode,
                                 PacketSCRemoveEffect::decode,
                                 PacketSCRemoveEffect::handle);
    }
}
