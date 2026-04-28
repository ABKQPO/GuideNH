package com.hfstudio.guidenh.guide.internal.item;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.structure.GuideStructureVolume;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Selection wand that exports a small block region either as an inline {@code <GameScene>} snippet
 * (legacy mode) or as a structure SNBT compatible with {@code <ImportStructure>} (default).
 */
public class RegionWandItem extends Item {

    public static final int MAX_EXPORT_BLOCKS = 4096;

    /** New default: SNBT structure compatible with {@code <ImportStructure>}. */
    public static final String MODE_SNBT = "snbt";
    /** Legacy: inline {@code <GameScene>} with {@code <Block>} children. */
    public static final String MODE_BLOCKS = "blocks";

    public RegionWandItem() {
        super();
        setUnlocalizedName("region_wand");
        setTextureName(GuideNH.MODID + ":" + "region_wand");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            exportToClipboard(stack, player, world);
            return true;
        }
        setPos(stack, /* which= */ 2, x, y, z);
        if (world.isRemote) {
            send(player, GuidebookText.RegionWandChatPos, 2, x, y, z);
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            exportToClipboard(stack, player, world);
        } else {
            // Plain right-click in air toggles the export mode.
            String next = nextMode(getExportMode(stack));
            setExportMode(stack, next);
            if (world.isRemote) {
                send(player, GuidebookText.RegionWandModeSwitched, modeDisplay(next));
            }
        }
        return stack;
    }

    public static void onLeftClickBlock(ItemStack stack, EntityPlayer player, int x, int y, int z) {
        setPos(stack, /* which= */ 1, x, y, z);
        if (player.worldObj.isRemote) {
            send(player, GuidebookText.RegionWandChatPos, 1, x, y, z);
        }
    }

    public static void setPos(ItemStack stack, int which, int x, int y, int z) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        var nbt = stack.getTagCompound();
        var sub = new NBTTagCompound();
        sub.setInteger("X", x);
        sub.setInteger("Y", y);
        sub.setInteger("Z", z);
        nbt.setTag(which == 1 ? "Pos1" : "Pos2", sub);
    }

    @Nullable
    public static int[] getPos(ItemStack stack, int which) {
        if (stack == null || !stack.hasTagCompound()) return null;
        var nbt = stack.getTagCompound();
        String key = which == 1 ? "Pos1" : "Pos2";
        if (!nbt.hasKey(key)) return null;
        var sub = nbt.getCompoundTag(key);
        return new int[] { sub.getInteger("X"), sub.getInteger("Y"), sub.getInteger("Z") };
    }

    public static String getExportMode(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return MODE_SNBT;
        var nbt = stack.getTagCompound();
        if (!nbt.hasKey("ExportMode")) return MODE_SNBT;
        String v = nbt.getString("ExportMode");
        return MODE_BLOCKS.equals(v) ? MODE_BLOCKS : MODE_SNBT;
    }

    public static boolean hasCompleteSelection(ItemStack stack) {
        return getPos(stack, 1) != null && getPos(stack, 2) != null;
    }

    public static void setExportMode(ItemStack stack, String mode) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setString("ExportMode", mode);
    }

    public static String nextMode(String current) {
        return MODE_SNBT.equals(current) ? MODE_BLOCKS : MODE_SNBT;
    }

    public static String modeDisplay(String mode) {
        return MODE_BLOCKS.equals(mode) ? "blocks" : "snbt";
    }

    @Nullable
    public static String exportSelectionAsStructureSnbt(World world, ItemStack stack) {
        if (world == null || stack == null) {
            return null;
        }
        int[] p1 = getPos(stack, 1);
        int[] p2 = getPos(stack, 2);
        if (p1 == null || p2 == null) {
            return null;
        }

        int minX = Math.min(p1[0], p2[0]);
        int minY = Math.min(p1[1], p2[1]);
        int minZ = Math.min(p1[2], p2[2]);
        int maxX = Math.max(p1[0], p2[0]);
        int maxY = Math.max(p1[1], p2[1]);
        int maxZ = Math.max(p1[2], p2[2]);
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        return exportRegionAsStructureSnbt(world, minX, minY, minZ, dx, dy, dz);
    }

    @Nullable
    public static String exportSelectionAsStructureSnbt(GuidebookLevel level, ItemStack stack) {
        if (level == null || stack == null) {
            return null;
        }
        int[] p1 = getPos(stack, 1);
        int[] p2 = getPos(stack, 2);
        if (p1 == null || p2 == null) {
            return null;
        }

        int minX = Math.min(p1[0], p2[0]);
        int minY = Math.min(p1[1], p2[1]);
        int minZ = Math.min(p1[2], p2[2]);
        int maxX = Math.max(p1[0], p2[0]);
        int maxY = Math.max(p1[1], p2[1]);
        int maxZ = Math.max(p1[2], p2[2]);
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;
        return exportRegionAsStructureSnbt(level, minX, minY, minZ, dx, dy, dz);
    }

    @Nullable
    public static String exportRegionAsStructureSnbt(World world, int minX, int minY, int minZ, int sizeX, int sizeY,
        int sizeZ) {
        if (world == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return null;
        }
        if (GuideStructureVolume.exceedsLimit(sizeX, sizeY, sizeZ, MAX_EXPORT_BLOCKS)) {
            return null;
        }
        int maxX = minX + sizeX - 1;
        int maxY = minY + sizeY - 1;
        int maxZ = minZ + sizeZ - 1;
        return exportSnbt(
            new WorldStructureExportAccess(world),
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            sizeX,
            sizeY,
            sizeZ).text();
    }

    @Nullable
    public static String exportRegionAsStructureSnbt(GuidebookLevel level, int minX, int minY, int minZ, int sizeX,
        int sizeY, int sizeZ) {
        if (level == null || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return null;
        }
        if (GuideStructureVolume.exceedsLimit(sizeX, sizeY, sizeZ, MAX_EXPORT_BLOCKS)) {
            return null;
        }
        int maxX = minX + sizeX - 1;
        int maxY = minY + sizeY - 1;
        int maxZ = minZ + sizeZ - 1;
        return exportSnbt(
            new GuidebookLevelStructureExportAccess(level),
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            sizeX,
            sizeY,
            sizeZ).text();
    }

    public static void exportToClipboard(ItemStack stack, EntityPlayer player, World world) {
        int[] p1 = getPos(stack, 1);
        int[] p2 = getPos(stack, 2);
        if (p1 == null || p2 == null) {
            if (world.isRemote) {
                send(player, GuidebookText.RegionWandNeedTwoCorners);
            }
            return;
        }
        if (!world.isRemote) return;

        int minX = Math.min(p1[0], p2[0]);
        int minY = Math.min(p1[1], p2[1]);
        int minZ = Math.min(p1[2], p2[2]);
        int maxX = Math.max(p1[0], p2[0]);
        int maxY = Math.max(p1[1], p2[1]);
        int maxZ = Math.max(p1[2], p2[2]);
        int dx = maxX - minX + 1;
        int dy = maxY - minY + 1;
        int dz = maxZ - minZ + 1;

        long total = GuideStructureVolume.blockCount(dx, dy, dz);
        if (total > MAX_EXPORT_BLOCKS) {
            send(player, GuidebookText.RegionWandAreaTooLarge, total);
            return;
        }

        String mode = getExportMode(stack);
        ExportResult result;
        if (MODE_BLOCKS.equals(mode)) {
            result = exportBlocks(world, minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            result = exportSnbt(world, minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz);
        }

        try {
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(result.text), null);
            GuidebookText msg = MODE_BLOCKS.equals(mode) ? GuidebookText.RegionWandCopied
                : GuidebookText.RegionWandCopiedSnbt;
            send(player, msg, dx, dy, dz, result.nonAir, result.teCount);
        } catch (Throwable t) {
            send(player, GuidebookText.RegionWandCopyFailed, getErrorMessage(t));
        }
    }

    public static ExportResult exportBlocks(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<GameScene zoom={4} interactive={true}>\n");
        int nonAir = 0;
        int teCount = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Block block = world.getBlock(x, y, z);
                    if (block == null || block == Blocks.air) continue;
                    String regName = Block.blockRegistry.getNameForObject(block);
                    if (regName == null || regName.isEmpty()) continue;
                    int meta = world.getBlockMetadata(x, y, z);
                    int rx = x - minX;
                    int ry = y - minY;
                    int rz = z - minZ;

                    String nbtSnbt = null;
                    TileEntity te = world.getTileEntity(x, y, z);
                    if (te != null) {
                        try {
                            NBTTagCompound teTag = new NBTTagCompound();
                            te.writeToNBT(teTag);
                            teTag.removeTag("x");
                            teTag.removeTag("y");
                            teTag.removeTag("z");
                            String s = GuideTextNbtCodec.writeTextSafeCompound(teTag);
                            nbtSnbt = s.replace("'", "\\'");
                        } catch (Throwable t) {
                            nbtSnbt = null;
                        }
                    }

                    sb.append("    <Block id=\"")
                        .append(regName)
                        .append('"');
                    if (rx != 0) sb.append(" x=\"")
                        .append(rx)
                        .append('"');
                    if (ry != 0) sb.append(" y=\"")
                        .append(ry)
                        .append('"');
                    if (rz != 0) sb.append(" z=\"")
                        .append(rz)
                        .append('"');
                    if (meta != 0) sb.append(" meta=\"")
                        .append(meta)
                        .append('"');
                    if (nbtSnbt != null) {
                        sb.append(" nbt='")
                            .append(nbtSnbt)
                            .append('\'');
                        teCount++;
                    }
                    sb.append(" />\n");
                    nonAir++;
                }
            }
        }
        sb.append("</GameScene>\n");
        return new ExportResult(sb.toString(), nonAir, teCount);
    }

    public static ExportResult exportSnbt(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
        int dx, int dy, int dz) {
        return exportSnbt(new WorldStructureExportAccess(world), minX, minY, minZ, maxX, maxY, maxZ, dx, dy, dz);
    }

    public static ExportResult exportSnbt(StructureExportAccess access, int minX, int minY, int minZ, int maxX,
        int maxY, int maxZ, int dx, int dy, int dz) {
        Map<String, Integer> paletteIndex = new HashMap<>();
        NBTTagList paletteList = new NBTTagList();
        NBTTagList blocksList = new NBTTagList();
        int nonAir = 0;
        int teCount = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Block block = access.getBlock(x, y, z);
                    if (block == null || block == Blocks.air) continue;
                    String regName = access.getBlockId(x, y, z, block);
                    if (regName == null || regName.isEmpty()) continue;
                    int meta = access.getBlockMetadata(x, y, z);

                    Integer idx = paletteIndex.get(regName);
                    if (idx == null) {
                        idx = paletteList.tagCount();
                        var entry = new NBTTagCompound();
                        entry.setString("Name", regName);
                        paletteList.appendTag(entry);
                        paletteIndex.put(regName, idx);
                    }

                    var blockTag = new NBTTagCompound();
                    blockTag.setIntArray("pos", new int[] { x - minX, y - minY, z - minZ });
                    blockTag.setInteger("state", idx);
                    if (meta != 0) blockTag.setInteger("meta", meta);

                    TileEntity te = access.getTileEntity(x, y, z);
                    if (te != null) {
                        try {
                            NBTTagCompound teTag = new NBTTagCompound();
                            te.writeToNBT(teTag);
                            teTag.removeTag("x");
                            teTag.removeTag("y");
                            teTag.removeTag("z");
                            blockTag.setTag("nbt", teTag);
                            teCount++;
                        } catch (Throwable ignored) {}
                    }
                    blocksList.appendTag(blockTag);
                    nonAir++;
                }
            }
        }

        var root = new NBTTagCompound();
        root.setIntArray("size", new int[] { dx, dy, dz });
        root.setTag("palette", paletteList);
        root.setTag("blocks", blocksList);
        return new ExportResult(GuideTextNbtCodec.writeStructureSnbt(root), nonAir, teCount);
    }

    private interface StructureExportAccess {

        Block getBlock(int x, int y, int z);

        int getBlockMetadata(int x, int y, int z);

        @Nullable
        TileEntity getTileEntity(int x, int y, int z);

        @Nullable
        String getBlockId(int x, int y, int z, Block block);
    }

    public static class WorldStructureExportAccess implements StructureExportAccess {

        private final World world;

        private WorldStructureExportAccess(World world) {
            this.world = world;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return world.getBlock(x, y, z);
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return world.getBlockMetadata(x, y, z);
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return world.getTileEntity(x, y, z);
        }

        @Override
        public String getBlockId(int x, int y, int z, Block block) {
            return Block.blockRegistry.getNameForObject(block);
        }
    }

    public static class GuidebookLevelStructureExportAccess implements StructureExportAccess {

        private final GuidebookLevel level;

        private GuidebookLevelStructureExportAccess(GuidebookLevel level) {
            this.level = level;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return level.getBlock(x, y, z);
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return level.getBlockMetadata(x, y, z);
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return level.getTileEntity(x, y, z);
        }

        @Override
        public String getBlockId(int x, int y, int z, Block block) {
            String explicitBlockId = level.getExplicitBlockId(x, y, z);
            return explicitBlockId != null && !explicitBlockId.isEmpty() ? explicitBlockId
                : Block.blockRegistry.getNameForObject(block);
        }
    }

    @Desugar
    private record ExportResult(String text, int nonAir, int teCount) {}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        int[] p1 = getPos(stack, 1);
        int[] p2 = getPos(stack, 2);
        list.add(GuidebookText.RegionWandTooltipSelect.text());
        list.add(GuidebookText.RegionWandTooltipExport.text());
        list.add(GuidebookText.RegionWandTooltipMode.text(modeDisplay(getExportMode(stack))));
        if (p1 != null) list.add(GuidebookText.RegionWandTooltipPos.text(1, p1[0], p1[1], p1[2]));
        if (p2 != null) list.add(GuidebookText.RegionWandTooltipPos.text(2, p2[0], p2[1], p2[2]));
    }

    public static void send(EntityPlayer player, GuidebookText key, Object... args) {
        player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    public static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage()
            : throwable.getClass()
                .getSimpleName();
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) return;
        EntityPlayer player = event.entityPlayer;
        if (player == null) return;
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof RegionWandItem)) return;

        RegionWandItem.onLeftClickBlock(held, player, event.x, event.y, event.z);
        event.setCanceled(true);
    }
}
