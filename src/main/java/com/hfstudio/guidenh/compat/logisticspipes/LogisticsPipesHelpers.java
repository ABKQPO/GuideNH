package com.hfstudio.guidenh.compat.logisticspipes;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.mixins.late.compat.logisticspipes.AccessorLogisticsTileGenericPipe;

import cpw.mods.fml.common.Optional;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.renderer.state.PipeRenderState;

public final class LogisticsPipesHelpers {

    private LogisticsPipesHelpers() {}

    /**
     * Resolves the display {@link ItemStack} for a LogisticsPipes pipe at the given position.
     * Returns the pipe-type-specific item so the tooltip shows the correct pipe name.
     * Returns {@code null} when the block is not a LogisticsPipes pipe block, the tile is
     * absent, or the pipe object has not been bound.
     */
    @Nullable
    @Optional.Method(modid = "LogisticsPipes")
    public static ItemStack resolveDisplayStack(GuidebookLevel level, Block block, int x, int y, int z) {
        if (!(block instanceof LogisticsBlockGenericPipe)) {
            return null;
        }
        TileEntity te = level.getTileEntity(x, y, z);
        if (!(te instanceof AccessorLogisticsTileGenericPipe)) {
            return null;
        }
        CoreUnroutedPipe pipe = ((AccessorLogisticsTileGenericPipe) te).getPipe();
        if (pipe == null || pipe.item == null) {
            return null;
        }
        return new ItemStack(pipe.item);
    }

    /**
     * Iterates all tile entities in the level and initialises the render state of each
     * LogisticsPipes pipe so that the pipe body and connection arms are visible in the
     * guide preview. This must be called before rendering begins.
     */
    @Optional.Method(modid = "LogisticsPipes")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (!(te instanceof AccessorLogisticsTileGenericPipe tile)) {
                continue;
            }
            initializePipeState(tile, (TileEntity) tile, level);
        }
    }

    @Optional.Method(modid = "LogisticsPipes")
    private static void initializePipeState(AccessorLogisticsTileGenericPipe tile, TileEntity te,
        GuidebookLevel level) {
        CoreUnroutedPipe pipe = tile.getPipe();
        PipeRenderState renderState = tile.getRenderState();
        if (pipe == null || renderState == null) {
            return;
        }
        // Set per-direction icon indices consumed by the old block renderer
        for (int i = 0; i < 7; i++) {
            ForgeDirection dir = ForgeDirection.getOrientation(i);
            try {
                renderState.textureMatrix.setIconIndex(dir, pipe.getIconIndex(dir));
            } catch (Throwable ignored) {}
        }
        // Refresh texture state for the new renderer; may throw for routed pipes that
        // require a fully-initialised router, so this is best-effort only
        try {
            renderState.textureMatrix.refreshStates(pipe);
        } catch (Throwable ignored) {}
        // Compute connections from adjacent tiles. The tile's canPipeConnect() always
        // returns false on the client, so we delegate to the pipe directly instead
        int x = te.xCoord;
        int y = te.yCoord;
        int z = te.zCoord;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity adj = level.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
            boolean connected = canConnect(pipe, adj, dir);
            renderState.pipeConnectionMatrix.setConnected(dir, connected);
        }
        // Clear dirty flags and invalidate the cached render list so the new renderer
        // rebuilds it from the connection state we just set
        renderState.clean();
    }

    @Optional.Method(modid = "LogisticsPipes")
    private static boolean canConnect(CoreUnroutedPipe pipe, @Nullable TileEntity adj, ForgeDirection dir) {
        if (adj == null) {
            return false;
        }
        try {
            return pipe.canPipeConnect(adj, dir);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
