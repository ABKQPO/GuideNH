package com.hfstudio.guidenh.guide.internal.item;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.GuidebookText;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Selection wand that exports a small block region as a GameScene snippet.
 */
public class RegionWandItem extends Item {

    private static final int MAX_EXPORT_BLOCKS = 4096;

    public RegionWandItem() {
        super();
        setUnlocalizedName("region_wand");
        setTextureName(GuideNH.MODID + ":" + "guide");
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
        }
        return stack;
    }

    public static void onLeftClickBlock(ItemStack stack, EntityPlayer player, int x, int y, int z) {
        setPos(stack, /* which= */ 1, x, y, z);
        if (player.worldObj.isRemote) {
            send(player, GuidebookText.RegionWandChatPos, 1, x, y, z);
        }
    }

    private static void setPos(ItemStack stack, int which, int x, int y, int z) {
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

    private static void exportToClipboard(ItemStack stack, EntityPlayer player, World world) {
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

        int total = dx * dy * dz;
        if (total > MAX_EXPORT_BLOCKS) {
            send(player, GuidebookText.RegionWandAreaTooLarge, total);
            return;
        }

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
                            String s = teTag.toString();
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

        String text = sb.toString();
        try {
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
            send(player, GuidebookText.RegionWandCopied, dx, dy, dz, nonAir, teCount);
        } catch (Throwable t) {
            send(player, GuidebookText.RegionWandCopyFailed, getErrorMessage(t));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        int[] p1 = getPos(stack, 1);
        int[] p2 = getPos(stack, 2);
        list.add(GuidebookText.RegionWandTooltipSelect.text());
        list.add(GuidebookText.RegionWandTooltipExport.text());
        if (p1 != null) list.add(GuidebookText.RegionWandTooltipPos.text(1, p1[0], p1[1], p1[2]));
        if (p2 != null) list.add(GuidebookText.RegionWandTooltipPos.text(2, p2[0], p2[1], p2[2]));
    }

    private static void send(EntityPlayer player, GuidebookText key, Object... args) {
        player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
    }

    private static String getErrorMessage(Throwable throwable) {
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
