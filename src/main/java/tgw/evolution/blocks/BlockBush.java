package tgw.evolution.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

public class BlockBush extends Block implements IPlantable, IReplaceable {

    protected BlockBush(Properties builder) {
        super(builder);
    }

    /**
     * Returns whether the blockstate can sustain the bush.
     */
    public static boolean isValidGround(BlockState state) {
        Block block = state.getBlock(); //TODO proper farmlad
        return block instanceof BlockGrass || block instanceof BlockDirt || block instanceof BlockDryGrass;
    }

    @Override
    public boolean canBeReplacedByLiquid(BlockState state) {
        return true;
    }

    @Override
    public boolean canBeReplacedByRope(BlockState state) {
        return true;
    }

    @Override
    public ItemStack getDrops(World world, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    public int getOpacity(BlockState state, IBlockReader worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public BlockState getPlant(IBlockReader world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != this) {
            return this.getDefaultState();
        }
        return state;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isReplaceable(BlockState state) {
        return true;
    }

    @Override
    public boolean isSolid(BlockState state) {
        return false;
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        BlockPos blockpos = pos.down();
        if (state.getBlock() == this) {
            return BlockUtils.canSustainSapling(worldIn.getBlockState(blockpos), this);
        }
        return isValidGround(worldIn.getBlockState(blockpos));
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn,
                                          Direction facing,
                                          BlockState facingState,
                                          IWorld worldIn,
                                          BlockPos currentPos,
                                          BlockPos facingPos) {
        return !stateIn.isValidPosition(worldIn, currentPos) ?
               Blocks.AIR.getDefaultState() :
               super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}