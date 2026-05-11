package com.hfstudio.structurelibexport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerCandidate;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerDiscoveryIntegration;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerIntegrationRegistry;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.BuildContext;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.ResolvedController;

public class StructureLibControllerDiscovery {

    public List<StructureLibControllerSpec> resolveControllers(StructureLibExportOptions options)
        throws CommandException {
        if (options.getController() != null) {
            ArrayList<StructureLibControllerSpec> controllers = new ArrayList<>();
            try {
                controllers.add(StructureLibControllerSpec.parse(options.getController()));
                return controllers;
            } catch (IllegalArgumentException e) {
                throw new CommandException(e.getMessage());
            }
        }
        return discoverAllControllers();
    }

    public List<StructureLibControllerSpec> discoverAllControllers() {
        ArrayList<StructureLibControllerSpec> controllers = new ArrayList<>();
        Set<DiscoveredControllerKey> discoveredKeys = new HashSet<>();
        BuildContext context = new BuildContext();
        try {
            for (StructureLibControllerSpec candidate : discoverControllerCandidates()) {
                ResolvedConstructableController resolved = resolveConstructableController(candidate, context);
                if (resolved != null && discoveredKeys.add(resolveDiscoveredKey(candidate, resolved))) {
                    controllers.add(candidate);
                }
            }
        } finally {
            context.clear();
            StructureLibRuntimeFacade.CONTROL_ANALYSIS_CACHE.clear();
        }
        controllers.sort(
            Comparator.comparing(StructureLibControllerSpec::getBlockId)
                .thenComparingInt(StructureLibControllerSpec::getMeta));
        return controllers;
    }

    public List<StructureLibControllerSpec> discoverControllerCandidates() {
        LinkedHashMap<String, StructureLibControllerSpec> candidates = new LinkedHashMap<>();
        for (Object candidate : Block.blockRegistry) {
            if (!(candidate instanceof Block block) || block == Blocks.air) {
                continue;
            }
            Object name = Block.blockRegistry.getNameForObject(block);
            if (name == null) {
                continue;
            }
            String blockId = name.toString();
            Item item = Item.getItemFromBlock(block);
            List<ItemStack> subItems = discoverSubItems(block, item);
            boolean discoveredSubItem = false;
            ArrayList<StructureLibControllerCandidate> integratedCandidates = new ArrayList<>();
            for (StructureLibControllerDiscoveryIntegration integration : StructureLibControllerIntegrationRegistry
                .global()
                .discoveryIntegrations()) {
                integration.appendCandidates(blockId, block, item, subItems, integratedCandidates);
            }
            for (StructureLibControllerCandidate integratedCandidate : integratedCandidates) {
                putCandidate(candidates, integratedCandidate);
                discoveredSubItem = true;
            }
            for (ItemStack stack : subItems) {
                int meta = Math.max(0, stack.getItemDamage());
                if (mightBeController(block, meta)) {
                    putCandidate(candidates, new StructureLibControllerCandidate(blockId, block, meta, stack));
                    discoveredSubItem = true;
                }
            }
            if (discoveredSubItem) {
                continue;
            }
            for (int meta = 0; meta <= 15; meta++) {
                if (mightBeController(block, meta)) {
                    putCandidate(candidates, new StructureLibControllerCandidate(blockId, block, meta, null));
                }
            }
        }
        return new ArrayList<>(candidates.values());
    }

    private List<ItemStack> discoverSubItems(Block block, Item item) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        if (item == null) {
            return stacks;
        }
        try {
            block.getSubBlocks(item, null, stacks);
        } catch (Throwable ignored) {}
        try {
            item.getSubItems(item, CreativeTabs.tabAllSearch, stacks);
        } catch (Throwable ignored) {}
        return stacks;
    }

    private void putCandidate(Map<String, StructureLibControllerSpec> candidates,
        StructureLibControllerCandidate candidate) {
        candidates.put(
            candidate.getControllerArgument(),
            new StructureLibControllerSpec(
                candidate.getBlockId(),
                candidate.getBlock(),
                candidate.getMeta(),
                candidate.getDisplayStack()));
    }

    private boolean mightBeController(Block block, int meta) {
        try {
            return block.hasTileEntity(meta);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private ResolvedConstructableController resolveConstructableController(StructureLibControllerSpec controller,
        BuildContext context) {
        context.clear();
        try {
            TileEntity tile = StructureLibRuntimeFacade.placeControllerDirectly(
                context.getLevel(),
                context.getWorld(),
                new ResolvedController(controller.getBlockId(), controller.getBlock(), controller.getMeta()),
                new ArrayList<>());
            if (tile == null) {
                return null;
            }
            IConstructable constructable = StructureLibRuntimeFacade.resolveConstructable(tile);
            return constructable != null ? new ResolvedConstructableController(tile, constructable) : null;
        } catch (Throwable ignored) {
            return null;
        } finally {
            context.clear();
        }
    }

    private DiscoveredControllerKey resolveDiscoveredKey(StructureLibControllerSpec controller,
        ResolvedConstructableController resolved) {
        for (StructureLibControllerDiscoveryIntegration integration : StructureLibControllerIntegrationRegistry.global()
            .discoveryIntegrations()) {
            Object identity = integration
                .resolveIdentity(toCandidate(controller), resolved.tile, resolved.constructable);
            if (identity != null) {
                return DiscoveredControllerKey.of(controller.getBlockId(), identity);
            }
        }
        return DiscoveredControllerKey.of(
            controller.getBlockId(),
            new DefaultControllerIdentity(controller.getDisplayName(), resolved.constructable.getClass()));
    }

    private StructureLibControllerCandidate toCandidate(StructureLibControllerSpec controller) {
        return new StructureLibControllerCandidate(
            controller.getBlockId(),
            controller.getBlock(),
            controller.getMeta(),
            controller.getDisplayStack());
    }

    public static class ResolvedConstructableController {

        private final TileEntity tile;
        private final IConstructable constructable;

        public ResolvedConstructableController(TileEntity tile, IConstructable constructable) {
            this.tile = tile;
            this.constructable = constructable;
        }
    }

    public static class DiscoveredControllerKey {

        private final String blockId;
        private final Object identity;

        public DiscoveredControllerKey(String blockId, Object identity) {
            this.blockId = blockId;
            this.identity = identity;
        }

        public static DiscoveredControllerKey of(String blockId, Object identity) {
            return new DiscoveredControllerKey(blockId, identity);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DiscoveredControllerKey other)) {
                return false;
            }
            return Objects.equals(blockId, other.blockId) && Objects.equals(identity, other.identity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockId, identity);
        }
    }

    public static class DefaultControllerIdentity {

        private final String displayName;
        private final Class<?> constructableType;

        public DefaultControllerIdentity(String displayName, Class<?> constructableType) {
            this.displayName = displayName;
            this.constructableType = constructableType;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DefaultControllerIdentity other)) {
                return false;
            }
            return Objects.equals(displayName, other.displayName)
                && Objects.equals(constructableType, other.constructableType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(displayName, constructableType);
        }
    }
}
