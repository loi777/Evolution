package tgw.evolution.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import tgw.evolution.init.EvolutionBlocks;
import tgw.evolution.init.EvolutionHitBoxes;
import tgw.evolution.init.EvolutionItems;
import tgw.evolution.util.HarvestLevel;
import tgw.evolution.util.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockClimbingHook extends Block implements IReplaceable, IRopeSupport {

    public static final DirectionProperty ROPE_DIRECTION = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;

    public BlockClimbingHook() {
        super(Properties.create(Material.IRON)
                        .sound(SoundType.METAL)
                        .hardnessAndResistance(0.0f)
                        .doesNotBlockMovement()
                        .harvestLevel(HarvestLevel.UNBREAKABLE));
        this.setDefaultState(this.getDefaultState().with(ROPE_DIRECTION, Direction.NORTH).with(ATTACHED, false));
    }

    public static void checkSides(BlockState state, World world, BlockPos pos) {
        Direction direction = state.get(ROPE_DIRECTION);
        BlockState stateTest = world.getBlockState(pos.offset(direction));
        if (stateTest.getBlock() == EvolutionBlocks.GROUND_ROPE.get()) {
            if (stateTest.get(BlockRopeGround.ORIGIN).getOpposite() == direction) {
                return;
            }
        }
        else if (BlockUtils.isReplaceable(stateTest)) {
            stateTest = world.getBlockState(pos.offset(direction).down());
            if (stateTest.getBlock() == EvolutionBlocks.ROPE.get()) {
                if (stateTest.get(BlockRope.DIRECTION).getOpposite() == direction) {
                    return;
                }
            }
        }
        world.setBlockState(pos, state.with(ATTACHED, false));
    }

    @Override
    public boolean canBeReplacedByLiquid(BlockState state) {
        return false;
    }

    @Override
    public boolean canBeReplacedByRope(BlockState state) {
        return false;
    }

    @Override
    public boolean canSupport(BlockState state, Direction direction) {
        return state.get(ROPE_DIRECTION) == direction;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(ROPE_DIRECTION, ATTACHED);
    }

    @Override
    public ItemStack getDrops(World world, BlockPos pos, BlockState state) {
        return new ItemStack(EvolutionItems.climbing_hook.get());
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getRopeLength() {
        return 5;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        switch (state.get(ROPE_DIRECTION)) {
            case NORTH:
                return EvolutionHitBoxes.HOOK_NORTH;
            case EAST:
                return EvolutionHitBoxes.HOOK_EAST;
            case SOUTH:
                return EvolutionHitBoxes.HOOK_SOUTH;
            case WEST:
                return EvolutionHitBoxes.HOOK_WEST;
        }
        throw new IllegalStateException("Invalid horizontal direction " + state.get(ROPE_DIRECTION));
    }

    @Override
    public boolean isReplaceable(BlockState state) {
        return true;
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        BlockPos posOffset = pos.offset(Direction.DOWN);
        BlockState stateFace = worldIn.getBlockState(posOffset);
        return Block.hasSolidSide(stateFace, worldIn, posOffset, Direction.UP);
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        if (!worldIn.isRemote) {
            if (!state.isValidPosition(worldIn, pos)) {
                spawnDrops(state, worldIn, pos);
                worldIn.removeBlock(pos, false);
            }
            checkSides(state, worldIn, pos);
        }
    }

    @Override
    public void onBlockClicked(BlockState state, World worldIn, BlockPos pos, PlayerEntity player) {
        if (!worldIn.isRemote) {
            int rope = this.removeRope(state, worldIn, pos);
            BlockUtils.dropItemStack(worldIn, pos, new ItemStack(EvolutionItems.rope.get(), rope));
        }
        worldIn.removeBlock(pos, true);
        spawnDrops(state, worldIn, pos);
        worldIn.playSound(player, pos, SoundEvents.BLOCK_METAL_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!worldIn.isRemote && !isMoving && state.getBlock() != newState.getBlock()) {
            BlockUtils.scheduleBlockTick(worldIn, pos.down().offset(state.get(ROPE_DIRECTION)), 2);
        }
    }

    public int removeRope(BlockState state, World world, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos);
        Direction direction = state.get(ROPE_DIRECTION);
        mutablePos.setPos(pos);
        Direction movement = direction;
        List<BlockPos> toRemove = new ArrayList<>();
        int count = 0;
        for (int removingRope = 1; removingRope <= this.getRopeLength(); removingRope++) {
            mutablePos.move(movement);
            BlockState temp = world.getBlockState(mutablePos);
            if (movement != Direction.DOWN && temp.getBlock() == EvolutionBlocks.GROUND_ROPE.get()) {
                if (temp.get(BlockRopeGround.ORIGIN) == movement.getOpposite()) {
                    count++;
                    toRemove.add(mutablePos.toImmutable());
                    continue;
                }
                break;
            }
            if (movement != Direction.DOWN && BlockUtils.isReplaceable(temp)) {
                movement = Direction.DOWN;
                mutablePos.move(Direction.DOWN);
                temp = world.getBlockState(mutablePos);
                if (temp.getBlock() == EvolutionBlocks.ROPE.get()) {
                    if (temp.get(BlockRope.DIRECTION) == direction.getOpposite()) {
                        count++;
                        toRemove.add(mutablePos.toImmutable());
                        continue;
                    }
                }
                break;
            }
            if (movement == Direction.DOWN && temp.getBlock() == EvolutionBlocks.ROPE.get()) {
                if (temp.get(BlockRope.DIRECTION) == direction.getOpposite()) {
                    count++;
                    toRemove.add(mutablePos.toImmutable());
                    continue;
                }
            }
            break;
        }
        MathHelper.iterateReverse(toRemove, removing -> world.removeBlock(removing, false));
        return count;
    }

    @Override
    public void tick(BlockState state, World worldIn, BlockPos pos, Random random) {
        if (!worldIn.isRemote) {
            checkSides(state, worldIn, pos);
        }
    }
}
