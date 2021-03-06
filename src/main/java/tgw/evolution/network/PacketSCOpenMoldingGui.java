package tgw.evolution.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import tgw.evolution.client.gui.ScreenMolding;

import java.util.function.Supplier;

public class PacketSCOpenMoldingGui implements IPacket {

    private final BlockPos pos;

    public PacketSCOpenMoldingGui(BlockPos pos) {
        this.pos = pos;
    }

    public static PacketSCOpenMoldingGui decode(PacketBuffer buffer) {
        return new PacketSCOpenMoldingGui(buffer.readBlockPos());
    }

    public static void encode(PacketSCOpenMoldingGui packet, PacketBuffer buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static void handle(PacketSCOpenMoldingGui packet, Supplier<NetworkEvent.Context> context) {
        if (IPacket.checkSide(packet, context)) {
            context.get().enqueueWork(() -> ScreenMolding.open(packet.pos));
            context.get().setPacketHandled(true);
        }
    }

    @Override
    public LogicalSide getDestinationSide() {
        return LogicalSide.CLIENT;
    }
}
