package com.hfstudio.guidenh.compat.forgemultipart;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;

public final class ForgeMultipartHelpers {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ForgeMultipart");
    private static final ConcurrentMap<String, Boolean> ONCE_KEYS = new ConcurrentHashMap<>();

    /** Sentinel placed in {@link #CLASS_CACHE} when a class name is not loadable on this side. */
    private static final Class<?> CLASS_NOT_FOUND = Void.class;
    private static final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    // Cached singleton MODULE$ instances and hot-path Methods. Volatile for safe lazy init.
    private static volatile Object MICROBLOCK_GENERATOR_MODULE;
    private static volatile Method MICROBLOCK_GENERATOR_CREATE;
    private static volatile Object SCALA_JAVA_CONVERSIONS_MODULE;
    private static volatile Method SCALA_AS_SCALA_BUFFER;

    public static final String BLOCK_MULTIPART_CLASS = "codechicken.multipart.BlockMultipart";
    public static final String TILE_MULTIPART_CLASS = "codechicken.multipart.TileMultipart";
    public static final String TILE_MULTIPART_CLIENT_CLASS = "codechicken.multipart.TileMultipartClient";
    public static final String MULTIPART_HELPER_CLASS = "codechicken.multipart.MultipartHelper";
    public static final String MULTIPART_HELPER_OBJECT_CLASS = "codechicken.multipart.MultipartHelper$";
    public static final String MULTIPART_GENERATOR_CLASS = "codechicken.multipart.MultipartGenerator";
    public static final String MULTIPART_GENERATOR_OBJECT_CLASS = "codechicken.multipart.MultipartGenerator$";
    public static final String MULTIPART_RENDERER_CLASS = "codechicken.multipart.MultipartRenderer";
    public static final String MULTIPART_RENDERER_OBJECT_CLASS = "codechicken.multipart.MultipartRenderer$";
    public static final String SAVED_MULTIPART_ID = "savedMultipart";
    public static final Object INVOCATION_MISSING = new Object();

    private ForgeMultipartHelpers() {}

    public static boolean isForgeMultipartBlock(@Nullable Block block) {
        return Mods.ForgeMultipart.isModLoaded() && isInstanceOf(block, BLOCK_MULTIPART_CLASS);
    }

    public static boolean isMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return Mods.ForgeMultipart.isModLoaded() && (isInstanceOf(tileEntity, TILE_MULTIPART_CLASS)
            || isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS));
    }

    /** Returns true only when the multipart tile is the client-side trait variant required by MultipartRenderer. */
    public static boolean isClientMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return Mods.ForgeMultipart.isModLoaded() && isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS);
    }

    public static boolean isSavedMultipartTag(@Nullable NBTTagCompound tag) {
        return tag != null && SAVED_MULTIPART_ID.equals(tag.getString("id"));
    }

    @Nullable
    public static TileEntity loadPreviewTile(World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag) {
        if (world == null || tag == null || (!isForgeMultipartBlock(block) && !isSavedMultipartTag(tag))) {
            return null;
        }

        NBTTagCompound positionedTag = withWorldPosition(tag, x, y, z);
        TileEntity tileEntity = createMultipartTileFromNbt(world, positionedTag);
        if (tileEntity == null || !world.isRemote) {
            return tileEntity;
        }
        return promoteClientMultipartTile(tileEntity, positionedTag);
    }

    @Nullable
    public static TileEntity finalizePreviewTile(@Nullable TileEntity tileEntity) {
        if (!isMultipartTileEntity(tileEntity)) {
            return tileEntity;
        }
        tileEntity = ensureClientMultipartTile(tileEntity);
        Object partList = resolvePartList(tileEntity);
        if (partList == null) {
            return tileEntity;
        }
        // Even when ensureClientMultipartTile already promoted the container, the parts inside may still be
        // server-side variants (this happens for tiles created by createTileFromNBT before promotion). Replace any
        // such parts with MicroblockClient-trait variants so renderStatic dispatches to the rendering trait.
        Object promotedList = promotePartListToClientVariants(partList);
        if (promotedList != partList && promotedList != null) {
            trySetPartList(tileEntity, promotedList);
            partList = promotedList;
        }
        invokeMethodIfPresent(tileEntity, "loadParts", partList);
        invokeMethodIfPresent(tileEntity, "notifyTileChange");
        invokeMethodIfPresent(tileEntity, "markRender");
        if (!isClientMultipartTileEntity(tileEntity)) {
            warnOnce(
                "finalize-not-client:" + tileEntity.getClass()
                    .getName(),
                "Multipart preview tile is not TileMultipartClient after finalize: {} (rendering will be skipped by MultipartRenderer)",
                tileEntity.getClass()
                    .getName());
        }
        return tileEntity;
    }

    public static boolean renderWorldBlock(@Nullable RenderBlocks renderBlocks, @Nullable IBlockAccess blockAccess,
        @Nullable Block block, int x, int y, int z) {
        if (renderBlocks == null || blockAccess == null || !isForgeMultipartBlock(block)) {
            return false;
        }
        Object rendered = invokeStaticOrSingletonMethod(
            MULTIPART_RENDERER_CLASS,
            MULTIPART_RENDERER_OBJECT_CLASS,
            "renderWorldBlock",
            blockAccess,
            x,
            y,
            z,
            block,
            block.getRenderType(),
            renderBlocks);
        return rendered instanceof Boolean && (Boolean) rendered;
    }

    @Nullable
    public static TileEntity createMultipartTileFromNbt(World world, NBTTagCompound tag) {
        Object value = invokeStaticOrSingletonMethod(
            MULTIPART_HELPER_CLASS,
            MULTIPART_HELPER_OBJECT_CLASS,
            "createTileFromNBT",
            world,
            tag);
        return value instanceof TileEntity tileEntity ? tileEntity : null;
    }

    public static final String MICROBLOCK_BASE_CLASS = "codechicken.microblock.Microblock";
    public static final String MICROBLOCK_CLIENT_CLASS = "codechicken.microblock.MicroblockClient";
    public static final String MICROBLOCK_GENERATOR_OBJECT_CLASS = "codechicken.microblock.MicroblockGenerator$";
    public static final String SCALA_JAVA_CONVERSIONS_OBJECT_CLASS = "scala.collection.JavaConversions$";

    /**
     * Walks a Scala partList and replaces every server-side Microblock with a client-trait variant produced by
     * {@code MicroblockGenerator.create(microClass, material, true)}. Returns a brand new Scala Seq containing the
     * promoted (or untouched) parts. Returns the input list unchanged if nothing needed promoting or the conversion
     * helpers are unavailable.
     */
    public static Object promotePartListToClientVariants(Object partList) {
        if (partList == null) {
            return null;
        }
        java.util.List<Object> rebuilt = new java.util.ArrayList<>();
        boolean anyPromoted = false;
        boolean anyMicroblockSeen = false;
        try {
            Method iterator = partList.getClass()
                .getMethod("iterator");
            Object it = iterator.invoke(partList);
            Method hasNext = it.getClass()
                .getMethod("hasNext");
            Method next = it.getClass()
                .getMethod("next");
            while (Boolean.TRUE.equals(hasNext.invoke(it))) {
                Object part = next.invoke(it);
                Object replacement = part;
                if (part != null && isInstanceOf(part, MICROBLOCK_BASE_CLASS)
                    && !isInstanceOf(part, MICROBLOCK_CLIENT_CLASS)) {
                    anyMicroblockSeen = true;
                    Object promoted = promoteMicroblockToClient(part);
                    if (promoted != null && promoted != part) {
                        replacement = promoted;
                        anyPromoted = true;
                    }
                }
                rebuilt.add(replacement);
            }
        } catch (Throwable t) {
            warnOnce(
                "promote-parts-iter-fail:" + partList.getClass()
                    .getName(),
                "Failed to iterate partList while promoting microblock parts: {}",
                t.toString());
            return partList;
        }
        if (!anyPromoted) {
            if (anyMicroblockSeen) {
                warnOnce(
                    "promote-parts-no-promote",
                    "Microblock parts found but none were promoted to client trait (MicroblockGenerator unavailable?)");
            }
            return partList;
        }
        Object scalaSeq = javaListToScalaBuffer(rebuilt);
        if (scalaSeq == null) {
            warnOnce(
                "promote-parts-no-conversion",
                "Promoted Microblock parts but failed to convert Java list to Scala Seq; using original partList");
            return partList;
        }
        return scalaSeq;
    }

    /**
     * Produce a {@code MicroblockClient}-trait equivalent of the given server-side Microblock by invoking
     * {@code MicroblockGenerator.create(microClass, material, true)} and copying the {@code shape} byte over.
     */
    @Nullable
    public static Object promoteMicroblockToClient(Object serverMicroblock) {
        if (serverMicroblock == null) {
            return null;
        }
        try {
            // Read microClass (Scala-generated accessor) and material (var) and shape (var).
            Object microClass = invokeMethodIfPresent(serverMicroblock, "microClass");
            if (microClass == INVOCATION_MISSING || microClass == null) {
                warnOnce(
                    "promote-mb-no-microClass:" + serverMicroblock.getClass()
                        .getName(),
                    "Cannot promote microblock {}: no microClass accessor",
                    serverMicroblock.getClass()
                        .getName());
                return null;
            }
            Object material = invokeMethodIfPresent(serverMicroblock, "material");
            if (material == INVOCATION_MISSING) {
                material = readDeclaredField(serverMicroblock, "material");
            }
            Object shape = invokeMethodIfPresent(serverMicroblock, "shape");
            if (shape == INVOCATION_MISSING) {
                shape = readDeclaredField(serverMicroblock, "shape");
            }
            if (!(material instanceof Integer)) {
                warnOnce(
                    "promote-mb-no-material:" + serverMicroblock.getClass()
                        .getName(),
                    "Cannot promote microblock {}: material is not Integer ({})",
                    serverMicroblock.getClass()
                        .getName(),
                    material);
                return null;
            }
            // MicroblockGenerator$.MODULE$.create(MicroblockClass, Int, Boolean) — cached after first lookup.
            Method create = getMicroblockGeneratorCreate();
            if (create == null) {
                warnOnce(
                    "promote-mb-no-create",
                    "MicroblockGenerator$.create(MicroblockClass, int, boolean) not found via reflection");
                return null;
            }
            Object promoted = create.invoke(MICROBLOCK_GENERATOR_MODULE, microClass, material, Boolean.TRUE);
            if (promoted == null) {
                warnOnce(
                    "promote-mb-null-result:" + serverMicroblock.getClass()
                        .getName(),
                    "MicroblockGenerator.create returned null for {}",
                    serverMicroblock.getClass()
                        .getName());
                return null;
            }
            // Copy shape (size+slot byte) over to preserve geometry.
            if (shape instanceof Byte b) {
                writeDeclaredField(promoted, "shape", b);
            }
            return promoted;
        } catch (Throwable t) {
            warnOnce(
                "promote-mb-fail:" + serverMicroblock.getClass()
                    .getName()
                    + ":"
                    + t.getClass()
                        .getName(),
                "Failed to promote microblock {} to client variant: {}",
                serverMicroblock.getClass()
                    .getName(),
                t.toString());
            return null;
        }
    }

    /**
     * Bridges a {@code java.util.List} to a Scala {@code mutable.Buffer} (which extends {@code Seq}) via
     * JavaConversions.
     */
    @Nullable
    private static Object javaListToScalaBuffer(java.util.List<?> javaList) {
        Method asScalaBuffer = getScalaAsScalaBuffer();
        if (asScalaBuffer == null) {
            return null;
        }
        try {
            return asScalaBuffer.invoke(SCALA_JAVA_CONVERSIONS_MODULE, javaList);
        } catch (Throwable t) {
            warnOnce(
                "scala-conversion-fail:" + t.getClass()
                    .getName(),
                "JavaConversions.asScalaBuffer reflection failed: {}",
                t.toString());
            return null;
        }
    }

    @Nullable
    private static Method getMicroblockGeneratorCreate() {
        Method cached = MICROBLOCK_GENERATOR_CREATE;
        if (cached != null) {
            return cached;
        }
        Class<?> generatorClass = cachedClass(MICROBLOCK_GENERATOR_OBJECT_CLASS);
        if (generatorClass == null) {
            return null;
        }
        try {
            for (Method m : generatorClass.getMethods()) {
                if (!"create".equals(m.getName())) continue;
                if (m.getParameterCount() != 3) continue;
                Class<?>[] pts = m.getParameterTypes();
                if (!"codechicken.microblock.MicroblockClass".equals(pts[0].getName())) continue;
                if (pts[1] != int.class) continue;
                if (pts[2] != boolean.class) continue;
                m.setAccessible(true);
                MICROBLOCK_GENERATOR_MODULE = generatorClass.getField("MODULE$")
                    .get(null);
                MICROBLOCK_GENERATOR_CREATE = m;
                return m;
            }
        } catch (Throwable ignored) {
            // fall through and return null
        }
        return null;
    }

    @Nullable
    private static Method getScalaAsScalaBuffer() {
        Method cached = SCALA_AS_SCALA_BUFFER;
        if (cached != null) {
            return cached;
        }
        Class<?> conversions = cachedClass(SCALA_JAVA_CONVERSIONS_OBJECT_CLASS);
        if (conversions == null) {
            return null;
        }
        try {
            Method m = conversions.getMethod("asScalaBuffer", java.util.List.class);
            m.setAccessible(true);
            SCALA_JAVA_CONVERSIONS_MODULE = conversions.getField("MODULE$")
                .get(null);
            SCALA_AS_SCALA_BUFFER = m;
            return m;
        } catch (Throwable t) {
            warnOnce(
                "scala-conversion-init-fail:" + t.getClass()
                    .getName(),
                "Failed to resolve JavaConversions.asScalaBuffer: {}",
                t.toString());
            return null;
        }
    }

    @Nullable
    private static Object readDeclaredField(Object target, String fieldName) {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                // try parent
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static void writeDeclaredField(Object target, String fieldName, Object value) {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // try parent
            } catch (Throwable t) {
                return;
            }
        }
    }

    public static TileEntity promoteClientMultipartTile(TileEntity tileEntity, @Nullable NBTTagCompound tag) {
        if (tileEntity == null || isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS)) {
            return tileEntity;
        }

        Object partList = resolvePartList(tileEntity);
        // Promote any server-side Microblock parts to their MicroblockClient variants. Without this, even after we
        // promote the container tile, MultipartRenderer.renderStatic on each part falls through to the no-op base.
        partList = promotePartListToClientVariants(partList);
        int sourceSize = partListSize(partList);
        if (partList == null || sourceSize == 0) {
            warnOnce(
                "promote-no-partlist:" + tileEntity.getClass()
                    .getName(),
                "Cannot promote multipart tile to client trait: partList resolution failed for {} (size={})",
                tileEntity.getClass()
                    .getName(),
                sourceSize);
            return tileEntity;
        }

        Object promoted = invokeStaticOrSingletonMethod(
            MULTIPART_GENERATOR_CLASS,
            MULTIPART_GENERATOR_OBJECT_CLASS,
            "generateCompositeTile",
            tileEntity,
            partList,
            Boolean.TRUE);
        if (!(promoted instanceof TileEntity promotedTile)) {
            warnOnce(
                "promote-generate-null:" + tileEntity.getClass()
                    .getName(),
                "MultipartGenerator.generateCompositeTile returned non-TileEntity ({}) for {}; multipart will not render",
                promoted == null ? "null"
                    : promoted.getClass()
                        .getName(),
                tileEntity.getClass()
                    .getName());
            return tileEntity;
        }
        if (promotedTile == tileEntity) {
            warnOnce(
                "promote-same-instance:" + tileEntity.getClass()
                    .getName(),
                "MultipartGenerator.generateCompositeTile returned the same instance for {} (no client trait was added)",
                tileEntity.getClass()
                    .getName());
            return tileEntity;
        }

        // Copy positional and world state forwards. ASM-generated copyFrom for the composite tile is an unknown
        // quantity, so we explicitly populate partList via the Scala setter and ignore from()/copyFrom().
        promotedTile.xCoord = tileEntity.xCoord;
        promotedTile.yCoord = tileEntity.yCoord;
        promotedTile.zCoord = tileEntity.zCoord;
        promotedTile.blockType = tileEntity.blockType;
        promotedTile.blockMetadata = tileEntity.blockMetadata;
        if (tileEntity.getWorldObj() != null) {
            promotedTile.setWorldObj(tileEntity.getWorldObj());
        }

        boolean partListInjected = trySetPartList(promotedTile, partList);
        // loadParts re-binds every TMultiPart back to the new container tile (TMultiPart.bind).
        Object loadResult = invokeMethodIfPresent(promotedTile, "loadParts", partList);
        if (loadResult == INVOCATION_MISSING) {
            warnOnce(
                "promote-no-loadparts:" + promotedTile.getClass()
                    .getName(),
                "Promoted multipart tile {} has no loadParts(Iterable) method visible via reflection",
                promotedTile.getClass()
                    .getName());
        }
        // notifyTileChange touches worldObj.func_147453_f which is harmless on the FakeWorld.
        invokeMethodIfPresent(promotedTile, "notifyTileChange");
        invokeMethodIfPresent(promotedTile, "markRender");

        int promotedSize = partListSize(resolvePartList(promotedTile));
        if (promotedSize == 0) {
            warnOnce(
                "promote-empty-after:" + promotedTile.getClass()
                    .getName(),
                "Promoted multipart tile partList is empty after promotion (sourceSize={} setterUsed={} class={})",
                sourceSize,
                partListInjected,
                promotedTile.getClass()
                    .getName());
        }
        return promotedTile;
    }

    /** Calls Scala-generated {@code partList_$eq(scala.collection.Seq)} to write the part list directly. */
    public static boolean trySetPartList(TileEntity tile, Object partList) {
        if (tile == null || partList == null) {
            return false;
        }
        try {
            for (Method method : tile.getClass()
                .getMethods()) {
                if ("partList_$eq".equals(method.getName()) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(partList.getClass())) {
                    method.setAccessible(true);
                    method.invoke(tile, partList);
                    return true;
                }
            }
        } catch (Throwable t) {
            warnOnce(
                "setPartList-fail:" + tile.getClass()
                    .getName(),
                "Failed to invoke partList_$eq on {}: {}",
                tile.getClass()
                    .getName(),
                t.toString());
        }
        return false;
    }

    public static int partListSize(@Nullable Object partList) {
        if (partList == null) {
            return -1;
        }
        try {
            Method size = partList.getClass()
                .getMethod("size");
            Object value = size.invoke(partList);
            return value instanceof Integer i ? i : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    @Nullable
    public static TileEntity ensureClientMultipartTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null || !isMultipartTileEntity(tileEntity)
            || isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS)) {
            return tileEntity;
        }
        // Allow promotion even when worldObj is null or server-side: previews always render client-side; the FakeWorld
        // sets isRemote=true after binding, but render-time fallback may run before/after that and we still need the
        // client trait in order for MultipartRenderer.renderWorldBlock to dispatch.

        NBTTagCompound snapshot = snapshotTile(tileEntity);
        TileEntity promotedTile = promoteClientMultipartTile(tileEntity, snapshot);
        return promotedTile != null ? promotedTile : tileEntity;
    }

    @Nullable
    public static NBTTagCompound snapshotTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null) {
            return null;
        }
        try {
            NBTTagCompound snapshot = new NBTTagCompound();
            tileEntity.writeToNBT(snapshot);
            return snapshot;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void warnOnce(String key, String message, Object... args) {
        if (ONCE_KEYS.putIfAbsent(key, Boolean.TRUE) == null) {
            LOG.warn(message, args);
        }
    }

    public static void infoOnce(String key, String message, Object... args) {
        if (ONCE_KEYS.putIfAbsent(key, Boolean.TRUE) == null) {
            LOG.info(message, args);
        }
    }

    @Nullable
    public static Object resolvePartList(TileEntity tileEntity) {
        Object value = invokeMethodIfPresent(tileEntity, "partList");
        if (value != INVOCATION_MISSING) {
            return value;
        }
        try {
            Field field = tileEntity.getClass()
                .getField("partList");
            return field.get(tileEntity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final ConcurrentMap<String, Object> SINGLETON_CACHE = new ConcurrentHashMap<>();

    public static Object invokeStaticOrSingletonMethod(String className, String singletonClassName, String methodName,
        Object... args) {
        Object value = invokeStaticMethodIfPresent(className, methodName, args);
        if (value != INVOCATION_MISSING) {
            return value;
        }

        Object singleton = resolveScalaSingleton(singletonClassName);
        if (singleton == null) {
            return null;
        }
        return invokeMethodIfPresent(singleton, methodName, args);
    }

    /** Resolves and caches the {@code MODULE$} singleton for the given Scala object class. */
    @Nullable
    public static Object resolveScalaSingleton(String singletonClassName) {
        Object cached = SINGLETON_CACHE.get(singletonClassName);
        if (cached != null) {
            return cached;
        }
        Class<?> singletonClass = cachedClass(singletonClassName);
        if (singletonClass == null) {
            return null;
        }
        try {
            Object module = singletonClass.getField("MODULE$")
                .get(null);
            if (module != null) {
                SINGLETON_CACHE.putIfAbsent(singletonClassName, module);
            }
            return module;
        } catch (Throwable t) {
            warnOnce(
                "singleton-fail:" + singletonClassName,
                "Failed to read {}.MODULE$: {}",
                singletonClassName,
                t.toString());
            return null;
        }
    }

    public static Object invokeStaticMethodIfPresent(String className, String methodName, Object... args) {
        Class<?> type = cachedClass(className);
        if (type == null) {
            return INVOCATION_MISSING;
        }
        try {
            Method method = findMatchingMethod(type, methodName, true, args);
            if (method == null) {
                return INVOCATION_MISSING;
            }
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (Throwable t) {
            warnOnce(
                "invokeStatic-fail:" + className + "#" + methodName,
                "FMP static reflection failed for {}#{}: {}",
                className,
                methodName,
                t.toString());
            return null;
        }
    }

    public static Object invokeMethodIfPresent(Object target, String methodName, Object... args) {
        if (target == null) {
            return INVOCATION_MISSING;
        }
        try {
            Method method = findMatchingMethod(target.getClass(), methodName, false, args);
            if (method == null) {
                return INVOCATION_MISSING;
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Throwable t) {
            warnOnce(
                "invokeMethod-fail:" + target.getClass()
                    .getName() + "#" + methodName,
                "FMP instance reflection failed for {}#{}: {}",
                target.getClass()
                    .getName(),
                methodName,
                t.toString());
            return null;
        }
    }

    @Nullable
    public static Method findMatchingMethod(Class<?> type, String methodName, boolean requireStatic, Object... args) {
        Method declaredMatch = findMatchingMethod(type.getDeclaredMethods(), methodName, requireStatic, args);
        if (declaredMatch != null) {
            return declaredMatch;
        }
        return findMatchingMethod(type.getMethods(), methodName, requireStatic, args);
    }

    @Nullable
    public static Method findMatchingMethod(Method[] methods, String methodName, boolean requireStatic,
        Object... args) {
        for (Method method : methods) {
            if (!methodName.equals(method.getName()) || method.getParameterCount() != args.length) {
                continue;
            }
            if (requireStatic != java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (matchesParameters(method.getParameterTypes(), args)) {
                return method;
            }
        }
        return null;
    }

    public static boolean matchesParameters(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Object arg = args[i];
            if (arg == null) {
                if (parameterType.isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> argumentType = arg.getClass();
            if (parameterType.isPrimitive()) {
                parameterType = wrapPrimitiveType(parameterType);
            }
            if (!parameterType.isAssignableFrom(argumentType)) {
                return false;
            }
        }
        return true;
    }

    public static Class<?> wrapPrimitiveType(Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return Boolean.class;
        }
        if (primitiveType == int.class) {
            return Integer.class;
        }
        if (primitiveType == long.class) {
            return Long.class;
        }
        if (primitiveType == double.class) {
            return Double.class;
        }
        if (primitiveType == float.class) {
            return Float.class;
        }
        if (primitiveType == short.class) {
            return Short.class;
        }
        if (primitiveType == byte.class) {
            return Byte.class;
        }
        if (primitiveType == char.class) {
            return Character.class;
        }
        return primitiveType;
    }

    /**
     * Cached counterpart of {@link Class#forName(String)}. Returns {@code null} when the class is not present on this
     * side. Result is memoized so subsequent lookups for the same FMP class skip class loading altogether.
     */
    @Nullable
    public static Class<?> cachedClass(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        Class<?> cached = CLASS_CACHE.get(className);
        if (cached != null) {
            return cached == CLASS_NOT_FOUND ? null : cached;
        }
        Class<?> resolved;
        try {
            resolved = Class.forName(className, false, ForgeMultipartHelpers.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            resolved = CLASS_NOT_FOUND;
        } catch (Throwable t) {
            resolved = CLASS_NOT_FOUND;
        }
        CLASS_CACHE.putIfAbsent(className, resolved);
        return resolved == CLASS_NOT_FOUND ? null : resolved;
    }

    public static boolean isInstanceOf(@Nullable Object instance, String className) {
        if (instance == null) {
            return false;
        }
        Class<?> target = cachedClass(className);
        return target != null && target.isInstance(instance);
    }

    public static NBTTagCompound withWorldPosition(NBTTagCompound original, int x, int y, int z) {
        NBTTagCompound copy = (NBTTagCompound) original.copy();
        copy.setInteger("x", x);
        copy.setInteger("y", y);
        copy.setInteger("z", z);
        return copy;
    }
}
