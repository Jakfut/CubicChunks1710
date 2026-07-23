package com.cardinalstar.cubicchunks.util;

import java.awt.image.BufferedImage;

import com.cardinalstar.cubicchunks.CubicChunks;

public final class JourneyMapRenderGuard {

    private static final int VOID_ARGB = 0xFF110C19;
    private static final ThreadLocal<RenderContext> CONTEXT = new ThreadLocal<>();

    private JourneyMapRenderGuard() {}

    public static void begin(Object chunkMetadata, Object mapType) {
        boolean underground = false;
        try {
            underground = (Boolean) mapType.getClass()
                .getMethod("isUnderground")
                .invoke(mapType);
        } catch (ReflectiveOperationException exception) {
            CubicChunks.LOGGER.warn("[CC_JM_RENDER_GUARD] Failed to inspect JourneyMap map type", exception);
        }
        CONTEXT.set(new RenderContext(String.valueOf(chunkMetadata), String.valueOf(mapType), underground));
    }

    public static void end() {
        CONTEXT.remove();
    }

    public static boolean shouldDiscard(BufferedImage image) {
        RenderContext context = CONTEXT.get();
        if (context == null || context.underground) {
            return false;
        }

        int voidPixels = 0;
        int transparentPixels = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int z = 0; z < image.getHeight(); z++) {
                int argb = image.getRGB(x, z);
                if (argb == VOID_ARGB) {
                    voidPixels++;
                } else if (argb >>> 24 == 0) {
                    transparentPixels++;
                }
            }
        }

        if (voidPixels == 0 && transparentPixels == 0) {
            return false;
        }

        CubicChunks.LOGGER.warn(
            "[CC_JM_RENDER_GUARD] Discarded incomplete surface render: chunk={}, mapType={}, voidPixels={}, transparentPixels={}",
            context.chunkMetadata,
            context.mapType,
            voidPixels,
            transparentPixels);
        return true;
    }

    private static final class RenderContext {

        private final String chunkMetadata;
        private final String mapType;
        private final boolean underground;

        private RenderContext(String chunkMetadata, String mapType, boolean underground) {
            this.chunkMetadata = chunkMetadata;
            this.mapType = mapType;
            this.underground = underground;
        }
    }
}
