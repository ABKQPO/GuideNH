package com.hfstudio.guidenh.guide.compiler;

import java.net.URI;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;

public class IdUtils {

    public static final Logger LOG = LoggerFactory.getLogger(IdUtils.class);

    private IdUtils() {}

    public static ResourceLocation resolveId(String idText, String defaultNamespace) {
        if (!idText.contains(":")) {
            return new ResourceLocation(defaultNamespace, idText);
        }
        return new ResourceLocation(idText);
    }

    /**
     * A parsed {@code modid:name[:meta][:snbt]} reference. {@link #meta} is
     * {@link OreDictionary#WILDCARD_VALUE} when the source asked for the wildcard, or 0 when the
     * meta segment is absent. {@link #nbt} is {@code null} when no SNBT tail was provided or when
     * SNBT parsing failed (a warning is logged in that case).
     */
    @Desugar
    public record ParsedItemRef(ResourceLocation id, int meta, @Nullable NBTTagCompound nbt) {

        public boolean isWildcardMeta() {
            return meta == OreDictionary.WILDCARD_VALUE;
        }

        /**
         * The meta used when building a concrete ItemStack: wildcard collapses to 0 since stacks
         * must carry a real damage value.
         */
        public int concreteMeta() {
            return isWildcardMeta() ? 0 : meta;
        }
    }

    /**
     * Parses an item reference of the form {@code name}, {@code modid:name}, {@code modid:name:meta}
     * or {@code modid:name:meta:{snbt}}. The meta segment defaults to {@code 0} when absent; it is
     * interpreted as {@link OreDictionary#WILDCARD_VALUE} when it equals {@code "*"}, {@code "32767"},
     * or any sequence of uppercase letters (e.g. {@code "ANY"}, {@code "W"}). The SNBT tail, if
     * present, is delimited by its leading {@code '{'} so colons inside the compound are preserved.
     * Returns {@code null} only when the input is blank; other malformed forms surface as an
     * {@link IllegalArgumentException} from {@link ResourceLocation}'s constructor.
     */
    @Nullable
    public static ParsedItemRef parseItemRef(String idText, String defaultNamespace) {
        if (idText == null || idText.isEmpty()) return null;
        String head;
        NBTTagCompound nbt = null;
        int brace = idText.indexOf('{');
        if (brace >= 0) {
            head = idText.substring(0, brace);
            if (!head.isEmpty() && head.charAt(head.length() - 1) == ':') {
                head = head.substring(0, head.length() - 1);
            }
            String snbt = idText.substring(brace);
            try {
                NBTBase parsed = JsonToNBT.func_150315_a(snbt);
                if (parsed instanceof NBTTagCompound tc) nbt = tc;
            } catch (Throwable t) {
                LOG.warn("Failed to parse SNBT tail '{}' for id '{}'; ignoring NBT", snbt, idText, t);
            }
        } else {
            head = idText;
        }

        ResourceLocation id;
        int meta = 0;
        // head is name | modid:name | modid:name:meta
        int firstColon = head.indexOf(':');
        if (firstColon < 0) {
            id = new ResourceLocation(defaultNamespace, head);
        } else {
            int secondColon = head.indexOf(':', firstColon + 1);
            if (secondColon < 0) {
                id = new ResourceLocation(head);
            } else {
                id = new ResourceLocation(head.substring(0, secondColon));
                String metaStr = head.substring(secondColon + 1);
                meta = parseMeta(metaStr);
            }
        }
        return new ParsedItemRef(id, meta, nbt);
    }

    public static int parseMeta(String metaStr) {
        if (metaStr.isEmpty()) return 0;
        if (metaStr.equals("*")) return OreDictionary.WILDCARD_VALUE;
        if (isAllAsciiUpper(metaStr)) return OreDictionary.WILDCARD_VALUE;
        if (isNonNegativeInt(metaStr)) {
            int v = Integer.parseInt(metaStr);
            return v == 32767 ? OreDictionary.WILDCARD_VALUE : v;
        }
        // Unknown meta spelling: treat as 0 per the "default meta is 0" rule rather than crashing.
        return 0;
    }

    public static boolean isAllAsciiUpper(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'Z') return false;
        }
        return !s.isEmpty();
    }

    /**
     * Resolves the item-index key for {@code idText}. Returns {@code null} when the referenced
     * item isn't registered or the input is blank.
     */
    @Nullable
    public static ItemId resolveItemId(String idText, String defaultNamespace) {
        ParsedItemRef ref = parseItemRef(idText, defaultNamespace);
        if (ref == null) return null;
        Item item = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (item == null) return null;
        return ItemId.createNoCopy(item, ref.meta(), ref.nbt());
    }

    /**
     * Builds a concrete {@link ItemStack} from {@code idText}. Wildcard meta collapses to 0 because
     * a stack needs a real damage value; NBT is copied onto the stack when present.
     */
    @Nullable
    public static ItemStack resolveItemStack(String idText, String defaultNamespace) {
        ParsedItemRef ref = parseItemRef(idText, defaultNamespace);
        if (ref == null) return null;
        Item item = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) stack.stackTagCompound = (NBTTagCompound) ref.nbt()
            .copy();
        return stack;
    }

    public static boolean isNonNegativeInt(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        // Bound to avoid overflow for silly inputs; item meta realistically fits short.
        return s.length() <= 9;
    }

    public static ResourceLocation resolveLink(String idText, ResourceLocation anchor) throws IllegalArgumentException {
        if (idText.startsWith("/")) {
            return new ResourceLocation(anchor.getResourceDomain(), idText.substring(1));
        } else if (!idText.contains(":")) {
            URI uri = URI.create(anchor.getResourcePath());
            uri = uri.resolve(idText);
            return new ResourceLocation(anchor.getResourceDomain(), uri.toString());
        }
        return new ResourceLocation(idText);
    }
}
