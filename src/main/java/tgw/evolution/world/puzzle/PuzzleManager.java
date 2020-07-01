package tgw.evolution.world.puzzle;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.Structures;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.gen.feature.template.TemplateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tgw.evolution.Evolution;
import tgw.evolution.blocks.BlockPuzzle;
import tgw.evolution.util.MathHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class PuzzleManager {

    public static final PuzzlePatternRegistry REGISTRY = new PuzzlePatternRegistry();
    public static final Logger LOGGER = LogManager.getLogger();

    static {
        REGISTRY.register(PuzzlePattern.EMPTY);
    }

    public static void startGeneration(ResourceLocation spawnPool, int size, PuzzleManager.IPieceFactory pieceFactory, ChunkGenerator<?> chunkGenerator, TemplateManager manager, BlockPos pos, List<StructurePiece> pieces, Random random) {
        Structures.init();
        new PuzzleManager.Assembler(spawnPool, size, pieceFactory, chunkGenerator, manager, pos, pieces, random);
    }

    public interface IPieceFactory {

        PuzzleStructurePiece create(TemplateManager manager, PuzzlePiece puzzlePiece, BlockPos pos, int groundLevelDelta, Rotation rotation, MutableBoundingBox boundingBox);
    }

    static final class Assembler {

        private final int size;
        private final PuzzleManager.IPieceFactory pieceFactory;
        private final ChunkGenerator<?> chunkGenerator;
        private final TemplateManager templateManager;
        private final List<StructurePiece> structurePieces;
        private final Random rand;
        private final Deque<PuzzleManager.Entry> availablePieces = Queues.newArrayDeque();
        private final Set<BlockPos> placedPuzzlesPos;

        public Assembler(ResourceLocation spawnPool, int size, PuzzleManager.IPieceFactory pieceFactory, ChunkGenerator<?> chunkGenerator, TemplateManager manager, BlockPos pos, List<StructurePiece> pieces, Random rand) {
            this.size = size;
            this.pieceFactory = pieceFactory;
            this.chunkGenerator = chunkGenerator;
            this.templateManager = manager;
            this.structurePieces = pieces;
            this.placedPuzzlesPos = new HashSet<>();
            this.rand = rand;
            Rotation chosenRotation = Rotation.randomRotation(rand);
            PuzzlePattern pool = PuzzleManager.REGISTRY.get(spawnPool);
            PuzzlePiece chosenSpawnPiece = pool.getRandomPiece(rand);
            PuzzleStructurePiece structure = pieceFactory.create(manager, chosenSpawnPiece, pos, chosenSpawnPiece.groundLevelDelta(), chosenRotation, chosenSpawnPiece.getBoundingBox(manager, pos, chosenRotation));
            MutableBoundingBox chosenBB = structure.getBoundingBox();
            int middleX = (chosenBB.maxX + chosenBB.minX) / 2;
            int middleZ = (chosenBB.maxZ + chosenBB.minZ) / 2;
            int surfaceY = chunkGenerator.func_222532_b(middleX, middleZ, Heightmap.Type.WORLD_SURFACE_WG);
            pieces.add(structure);
            if (size > 0) {
                AxisAlignedBB limitBB = new AxisAlignedBB(middleX - 80, MathHelper.clampMin(surfaceY - 80, 5), middleZ - 80, middleX + 80 + 1, surfaceY + 80 + 1, middleZ + 80 + 1);
                this.availablePieces.addLast(new PuzzleManager.Entry(structure, new AtomicReference<>(MathHelper.subtract(VoxelShapes.create(limitBB), VoxelShapes.create(AxisAlignedBB.func_216363_a(chosenBB)))), surfaceY + 80, 0));
                while (!this.availablePieces.isEmpty()) {
                    PuzzleManager.Entry managerEntry = this.availablePieces.removeFirst();
                    this.placePuzzlePiece(managerEntry.structurePiece, managerEntry.currentShape, managerEntry.maxHeight, managerEntry.currentSize);
                }
            }
        }

        private void placePuzzlePiece(PuzzleStructurePiece structurePiece, AtomicReference<VoxelShape> currentShape, int maxHeight, int currentSize) {
            PuzzlePiece placingPiece = structurePiece.getPuzzlePiece();
            BlockPos placingPos = structurePiece.getPos();
            Rotation placingRotation = structurePiece.getRotation();
            PuzzlePattern.PlacementBehaviour placingProjection = placingPiece.getPlacementBehaviour();
            boolean isPlacingRigid = placingProjection == PuzzlePattern.PlacementBehaviour.RIGID;
            AtomicReference<VoxelShape> checkingShape = new AtomicReference<>();
            MutableBoundingBox placingBB = structurePiece.getBoundingBox();
            int placingMinY = placingBB.minY;
            List<PuzzlePiece> piecesForConnection = Lists.newArrayList();
            BlockPos.MutableBlockPos matchingCornerPos = new BlockPos.MutableBlockPos();
            Set<PuzzlePiece> failedPieces = Sets.newHashSet();
            placingPuzzleBlocks:
            for (Template.BlockInfo puzzleBlock : placingPiece.getPuzzleBlocks(this.templateManager, placingPos, placingRotation, this.rand)) {
                boolean shouldPuzzleBlockCheckBB = puzzleBlock.nbt.getBoolean("CheckBB");
                Direction puzzleBlockFacing = puzzleBlock.state.get(BlockPuzzle.FACING);
                BlockPos puzzleBlockPos = puzzleBlock.pos;
                BlockPos connectionPos = puzzleBlockPos.offset(puzzleBlockFacing);
                if (this.placedPuzzlesPos.contains(connectionPos)) {
                    continue;
                }
                int relativePuzzleBlockYPos = puzzleBlockPos.getY() - placingMinY;
                //noinspection ObjectAllocationInLoop
                PuzzlePattern targetPool = PuzzleManager.REGISTRY.get(new ResourceLocation(puzzleBlock.nbt.getString("TargetPool")));
                PuzzlePattern fallbackPool = PuzzleManager.REGISTRY.get(targetPool.getFallbackPool());
                if (targetPool != PuzzlePattern.INVALID && (targetPool.getNumberOfPieces() != 0 || targetPool == PuzzlePattern.EMPTY)) {
                    boolean isConnectionPosInside = placingBB.isVecInside(connectionPos);
                    if (isConnectionPosInside) {
                        if (shouldPuzzleBlockCheckBB) {
                            LOGGER.warn("Ignoring Puzzle Block at {} because of the Bounding Box check. This is probably a bug", puzzleBlockPos);
                            continue;
                        }
                    }
                    //noinspection ObjectAllocationInLoop
                    checkingShape.set(shouldPuzzleBlockCheckBB ? currentShape.get() : MathHelper.union(currentShape.get(), VoxelShapes.create(AxisAlignedBB.func_216363_a(placingBB))));
                    piecesForConnection.clear();
                    if (currentSize != this.size) {
                        piecesForConnection.addAll(targetPool.getShuffledPieces(this.rand));
                    }
                    piecesForConnection.addAll(fallbackPool.getShuffledPieces(this.rand));
                    failedPieces.clear();
                    int surfaceYForPuzzleBlock = -1;
                    for (PuzzlePiece candidatePiece : piecesForConnection) {
                        if (candidatePiece == EmptyPuzzlePiece.INSTANCE) {
                            break;
                        }
                        if (failedPieces.contains(candidatePiece)) {
                            continue;
                        }
                        for (Rotation candidateRotation : Rotation.shuffledRotations(this.rand)) {
                            List<Template.BlockInfo> candidatePuzzleBlocks = candidatePiece.getPuzzleBlocks(this.templateManager, BlockPos.ZERO, candidateRotation, this.rand);
                            //not sure yet; has to do with connections inside their own BB
                            //                            int maxPoolHeight;
                            //                            if (candidateBB.getYSize() > 16) {
                            //                                Evolution.LOGGER.debug("candidateBB ySize > 16, maxPoolHeight = 0");
                            //                                maxPoolHeight = 0;
                            //                            }
                            //                            else {
                            //                                //noinspection ObjectAllocationInLoop
                            //                                maxPoolHeight = candidatePuzzleBlocks.stream().mapToInt(candidatePuzzleBlock -> {
                            //                                    //checks if the PuzzleBlock connection is not inside the bounding box of its own piece
                            //                                    if (!candidateBB.isVecInside(candidatePuzzleBlock.pos.offset(candidatePuzzleBlock.state.get(BlockPuzzle.FACING)))) {
                            //                                        return 0;
                            //                                    }
                            //                                    //noinspection ObjectAllocationInLoop
                            //                                    ResourceLocation candidatePoolLocation = new ResourceLocation(candidatePuzzleBlock.nbt.getString("TargetPool"));
                            //                                    PuzzlePattern candidateTargetPool = PuzzleManager.REGISTRY.get(candidatePoolLocation);
                            //                                    PuzzlePattern candidateFallbackPool = PuzzleManager.REGISTRY.get(candidateTargetPool.getFallbackPool());
                            //                                    return Math.max(candidateTargetPool.getMaxHeight(this.templateManager), candidateFallbackPool.getMaxHeight(this.templateManager));
                            //                                }).max().orElse(0);
                            //                                Evolution.LOGGER.debug("maxPoolHeight = {}", maxPoolHeight);
                            //                            }
                            for (Template.BlockInfo candidatePuzzleBlock : candidatePuzzleBlocks) {
                                if (BlockPuzzle.puzzlesMatches(puzzleBlock, candidatePuzzleBlock)) {
                                    BlockPos matchingPuzzleBlockRelativePos = candidatePuzzleBlock.pos;
                                    matchingCornerPos.setPos(connectionPos.getX() - matchingPuzzleBlockRelativePos.getX(), connectionPos.getY() - matchingPuzzleBlockRelativePos.getY(), connectionPos.getZ() - matchingPuzzleBlockRelativePos.getZ());
                                    MutableBoundingBox matchingBB = candidatePiece.getBoundingBox(this.templateManager, matchingCornerPos, candidateRotation);
                                    int matchingMinY = matchingBB.minY;
                                    PuzzlePattern.PlacementBehaviour matchingProjection = candidatePiece.getPlacementBehaviour();
                                    boolean isMatchingRigid = matchingProjection == PuzzlePattern.PlacementBehaviour.RIGID;
                                    int matchingPuzzleBlockRelativePosY = matchingPuzzleBlockRelativePos.getY();
                                    int heightDelta = relativePuzzleBlockYPos - matchingPuzzleBlockRelativePosY + puzzleBlock.state.get(BlockPuzzle.FACING).getYOffset();
                                    int actualMatchingMinY;
                                    if (isPlacingRigid && isMatchingRigid) {
                                        actualMatchingMinY = placingMinY + heightDelta;
                                    }
                                    else {
                                        if (surfaceYForPuzzleBlock == -1) {
                                            surfaceYForPuzzleBlock = this.chunkGenerator.func_222532_b(puzzleBlockPos.getX(), puzzleBlockPos.getZ(), Heightmap.Type.WORLD_SURFACE_WG);
                                        }
                                        actualMatchingMinY = surfaceYForPuzzleBlock - matchingPuzzleBlockRelativePosY;
                                    }
                                    int matchingMinYDelta = actualMatchingMinY - matchingMinY;
                                    //func_215127_b moves placingBB
                                    MutableBoundingBox actualMachingBB = matchingMinYDelta == 0 ? matchingBB : matchingBB.func_215127_b(0, matchingMinYDelta, 0);
                                    BlockPos actualMatchingCornerPos = matchingCornerPos.add(0, matchingMinYDelta, 0);
                                    //                                    if (maxPoolHeight > 0) {
                                    //                                        Evolution.LOGGER.debug("maxPoolHeight > 0");
                                    //                                        int k2 = Math.max(maxPoolHeight + 1, actualMachingBB.maxY - actualMachingBB.minY);
                                    //                                        Evolution.LOGGER.debug("k2 = {}", k2);
                                    //                                        actualMachingBB.maxY = actualMachingBB.minY + k2;
                                    //                                        Evolution.LOGGER.debug("actualMachingBB.maxY = {}", actualMachingBB.maxY);
                                    //                                    }
                                    if (this.canPlace(candidatePiece, actualMachingBB, checkingShape)) {
                                        currentShape.set(MathHelper.subtract(currentShape.get(), VoxelShapes.create(AxisAlignedBB.func_216363_a(actualMachingBB))));
                                        int placingGroundLevelDelta = structurePiece.getGroundLevelDelta();
                                        int matchingGroundLevelDelta;
                                        if (isMatchingRigid) {
                                            matchingGroundLevelDelta = placingGroundLevelDelta - heightDelta;
                                        }
                                        else {
                                            matchingGroundLevelDelta = candidatePiece.groundLevelDelta();
                                        }
                                        PuzzleStructurePiece matchingStructure = this.pieceFactory.create(this.templateManager, candidatePiece, actualMatchingCornerPos, matchingGroundLevelDelta, candidateRotation, actualMachingBB);
                                        int junctionPosY;
                                        if (isPlacingRigid) {
                                            junctionPosY = placingMinY + relativePuzzleBlockYPos;
                                        }
                                        else if (isMatchingRigid) {
                                            junctionPosY = actualMatchingMinY + matchingPuzzleBlockRelativePosY;
                                        }
                                        else {
                                            if (surfaceYForPuzzleBlock == -1) {
                                                surfaceYForPuzzleBlock = this.chunkGenerator.func_222532_b(puzzleBlockPos.getX(), puzzleBlockPos.getZ(), Heightmap.Type.WORLD_SURFACE_WG);
                                            }
                                            junctionPosY = surfaceYForPuzzleBlock + heightDelta / 2;
                                        }
                                        structurePiece.addJunction(new PuzzleJunction(connectionPos.getX(), junctionPosY - relativePuzzleBlockYPos + placingGroundLevelDelta, connectionPos.getZ(), heightDelta, matchingProjection));
                                        matchingStructure.addJunction(new PuzzleJunction(puzzleBlockPos.getX(), junctionPosY - matchingPuzzleBlockRelativePosY + matchingGroundLevelDelta, puzzleBlockPos.getZ(), -heightDelta, placingProjection));
                                        this.structurePieces.add(matchingStructure);
                                        if (currentSize + 1 <= this.size) {
                                            //noinspection ObjectAllocationInLoop
                                            this.availablePieces.addLast(new Entry(matchingStructure, currentShape, maxHeight, currentSize + 1));
                                        }
                                        this.placedPuzzlesPos.add(puzzleBlockPos);
                                        continue placingPuzzleBlocks;
                                    }
                                }
                            }
                        }
                        failedPieces.add(candidatePiece);
                    }
                }
                else {
                    LOGGER.warn("Empty or none existent pool: {}", puzzleBlock.nbt.getString("TargetPool"));
                }
            }
        }

        private boolean canPlace(PuzzlePiece piece, MutableBoundingBox pieceBB, AtomicReference<VoxelShape> checkingShape) {
            if (piece instanceof ForcedPuzzlePiece) {
                switch (((ForcedPuzzlePiece) piece).getForceType()) {
                    case HARD:
                        return true;
                    case SOFT:
                        return !MathHelper.isShapeTotallyOutside(VoxelShapes.create(AxisAlignedBB.func_216363_a(pieceBB).shrink(0.25)), checkingShape.get());
                    default:
                        throw new IllegalStateException("Missing ForceType");
                }
            }
            if (piece instanceof UndergroundPuzzlePiece) {
                Evolution.LOGGER.debug("piece is underground {}", piece);
                int middleX = (pieceBB.maxX + pieceBB.minX) / 2;
                Evolution.LOGGER.debug("middleX = {}", middleX);
                int middleZ = (pieceBB.maxZ + pieceBB.minZ) / 2;
                Evolution.LOGGER.debug("middleZ = {}", middleZ);
                int surfaceY = this.chunkGenerator.func_222532_b(middleX, middleZ, Heightmap.Type.OCEAN_FLOOR_WG);
                Evolution.LOGGER.debug("surfaceY = {}", surfaceY);
                if (pieceBB.maxY > surfaceY - 5) {
                    Evolution.LOGGER.debug("piece failed");
                    return false;
                }
                Evolution.LOGGER.debug("piece passed height check");
            }
            return MathHelper.isShapeTotallyInside(VoxelShapes.create(AxisAlignedBB.func_216363_a(pieceBB).shrink(0.25)), checkingShape.get());
        }
    }

    static final class Entry {

        private final PuzzleStructurePiece structurePiece;
        private final AtomicReference<VoxelShape> currentShape;
        private final int maxHeight;
        private final int currentSize;

        private Entry(PuzzleStructurePiece structurePiece, AtomicReference<VoxelShape> currentShape, int maxHeight, int currentSize) {
            this.structurePiece = structurePiece;
            this.currentShape = currentShape;
            this.maxHeight = maxHeight;
            this.currentSize = currentSize;
        }
    }
}
