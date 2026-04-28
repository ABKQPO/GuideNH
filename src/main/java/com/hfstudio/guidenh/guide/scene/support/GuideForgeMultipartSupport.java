package com.hfstudio.guidenh.guide.scene.support;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class GuideForgeMultipartSupport {

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

    private GuideForgeMultipartSupport() {}

    public static boolean isForgeMultipartBlock(@Nullable Block block) {
        return isInstanceOf(block, BLOCK_MULTIPART_CLASS);
    }

    public static boolean isMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return isInstanceOf(tileEntity, TILE_MULTIPART_CLASS) || isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS);
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
        invokeMethodIfPresent(tileEntity, "loadParts", partList);
        invokeMethodIfPresent(tileEntity, "notifyTileChange");
        invokeMethodIfPresent(tileEntity, "markRender");
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

    public static TileEntity promoteClientMultipartTile(TileEntity tileEntity, @Nullable NBTTagCompound tag) {
        if (tileEntity == null || isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS)) {
            return tileEntity;
        }

        Object partList = resolvePartList(tileEntity);
        if (partList == null) {
            return tileEntity;
        }

        Object promoted = invokeStaticOrSingletonMethod(
            MULTIPART_GENERATOR_CLASS,
            MULTIPART_GENERATOR_OBJECT_CLASS,
            "generateCompositeTile",
            tileEntity,
            partList,
            Boolean.TRUE);
        if (!(promoted instanceof TileEntity promotedTile) || promotedTile == tileEntity) {
            return tileEntity;
        }

        if (invokeMethodIfPresent(promotedTile, "from", tileEntity) != INVOCATION_MISSING) {
            return promotedTile;
        }

        if (tag != null) {
            invokeMethodIfPresent(promotedTile, "readFromNBT", tag);
        }
        invokeMethodIfPresent(promotedTile, "loadParts", partList);
        return promotedTile;
    }

    @Nullable
    public static TileEntity ensureClientMultipartTile(@Nullable TileEntity tileEntity) {
        if (tileEntity == null || !isMultipartTileEntity(tileEntity)
            || isInstanceOf(tileEntity, TILE_MULTIPART_CLIENT_CLASS)
            || tileEntity.getWorldObj() == null
            || !tileEntity.getWorldObj().isRemote) {
            return tileEntity;
        }

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

    public static Object invokeStaticOrSingletonMethod(String className, String singletonClassName, String methodName,
        Object... args) {
        Object value = invokeStaticMethodIfPresent(className, methodName, args);
        if (value != INVOCATION_MISSING) {
            return value;
        }

        try {
            Class<?> singletonClass = Class.forName(singletonClassName);
            Object singleton = singletonClass.getField("MODULE$")
                .get(null);
            return invokeMethodIfPresent(singleton, methodName, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object invokeStaticMethodIfPresent(String className, String methodName, Object... args) {
        try {
            Class<?> type = Class.forName(className);
            Method method = findMatchingMethod(type, methodName, true, args);
            if (method == null) {
                return INVOCATION_MISSING;
            }
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (ClassNotFoundException ignored) {
            return INVOCATION_MISSING;
        } catch (Throwable ignored) {
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
        } catch (Throwable ignored) {
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

    public static boolean isInstanceOf(@Nullable Object instance, String className) {
        if (instance == null || className == null || className.isEmpty()) {
            return false;
        }
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            if (className.equals(type.getName())) {
                return true;
            }
            for (Class<?> implementedInterface : type.getInterfaces()) {
                if (className.equals(implementedInterface.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static NBTTagCompound withWorldPosition(NBTTagCompound original, int x, int y, int z) {
        NBTTagCompound copy = (NBTTagCompound) original.copy();
        copy.setInteger("x", x);
        copy.setInteger("y", y);
        copy.setInteger("z", z);
        return copy;
    }
}
