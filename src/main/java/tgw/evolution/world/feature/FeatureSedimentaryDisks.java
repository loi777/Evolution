package tgw.evolution.world.feature;

import com.mojang.datafixers.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.SphereReplaceConfig;
import tgw.evolution.blocks.BlockUtils;

import java.util.Random;
import java.util.function.Function;

public class FeatureSedimentaryDisks extends Feature<SphereReplaceConfig> {


    public FeatureSedimentaryDisks(Function<Dynamic<?>, ? extends SphereReplaceConfig> config) {
        super(config);
    }

    @Override
    public boolean place(IWorld world, ChunkGenerator<? extends GenerationSettings> generator, Random rand, BlockPos pos, SphereReplaceConfig config) {
        if (!world.getFluidState(pos).isTagged(FluidTags.WATER)) {
            return false;
        }
        int placed = 0;
        int radius = rand.nextInt(config.radius - 2) + 2;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; ++x) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; ++z) {
                int deltaX = x - pos.getX();
                int deltaZ = z - pos.getZ();
                if (deltaX * deltaX + deltaZ * deltaZ <= radius * radius) {
                    for (int y = pos.getY() - config.ySize; y <= pos.getY() + config.ySize; ++y) {
                        mutablePos.setPos(x, y, z);
                        BlockState stateAtPos = world.getBlockState(mutablePos);
                        for (BlockState stateInConfig : config.targets) {
                            if (stateInConfig.getBlock() == stateAtPos.getBlock()) {
                                if (BlockUtils.isTouchingWater(world, mutablePos)) {
                                    world.setBlockState(mutablePos, config.state, 2);
                                    ++placed;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return placed > 0;
    }
}
