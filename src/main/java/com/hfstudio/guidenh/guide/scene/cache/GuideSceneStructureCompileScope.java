package com.hfstudio.guidenh.guide.scene.cache;

import java.util.function.Supplier;

public class GuideSceneStructureCompileScope {

    private static final ThreadLocal<Boolean> STRUCTURE_MUTATION_ENABLED = ThreadLocal.withInitial(() -> Boolean.TRUE);

    public static boolean isStructureMutationEnabled() {
        return STRUCTURE_MUTATION_ENABLED.get();
    }

    public static void run(boolean structureMutationEnabled, Runnable action) {
        supply(structureMutationEnabled, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T supply(boolean structureMutationEnabled, Supplier<T> action) {
        Boolean previous = STRUCTURE_MUTATION_ENABLED.get();
        STRUCTURE_MUTATION_ENABLED.set(structureMutationEnabled);
        try {
            return action.get();
        } finally {
            STRUCTURE_MUTATION_ENABLED.set(previous);
        }
    }
}
