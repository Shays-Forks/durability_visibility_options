package com.danrus.durability_visibility_options.client.utils;

import com.danrus.durability_visibility_options.client.DurabilityData;
import com.danrus.durability_visibility_options.client.config.DurabilityConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.RotationAxis;

import java.awt.*;
import java.lang.reflect.Method;
public class DrawUtils {
    private static boolean fillGradientChecked;
    private static Method fillGradient6;
    private static Method fillGradient7;
    private static boolean fillChecked;
    private static Method fill5;
    private static Method fill7;
    private static boolean guiLayerChecked;
    private static Method getGuiLayer;
    private static boolean guiOverlayChecked;
    private static Method getGuiOverlayLayer;

    public static int interpolateColorHSV(int colorStart, int colorEnd, int t) {
        float ratio = Math.max(0f, Math.min(1f, t / 100f));

        Color start = new Color(colorStart);
        Color end = new Color(colorEnd);

        float[] hsvStart = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
        float[] hsvEnd = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

        float h1 = hsvStart[0];
        float h2 = hsvEnd[0];

        float deltaH = h2 - h1;
        if (Math.abs(deltaH) > 0.5f) {
            if (deltaH > 0) {
                h1 += 1.0f;
            } else {
                h2 += 1.0f;
            }
        }

        float h = (h1 + (h2 - h1) * ratio) % 1.0f;
        float s = hsvStart[1] + (hsvEnd[1] - hsvStart[1]) * ratio;
        float v = hsvStart[2] + (hsvEnd[2] - hsvStart[2]) * ratio;

        return Color.HSBtoRGB(h, s, v);
    }

    public static void drawGradientHorizontalBar(DrawContext context, int itemBarStep) {
        int past_color = Color.HSBtoRGB(1, 1f, 1f) | 0xFF000000;
        pushMatrix(context);
        rotateMatrix(context, 90);
        for (int k = 0; k < 13; k++) {
            float hue = (float) k / 13f;
            int rgb = Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
            if (k < itemBarStep) {

                fillGradientCompat(
                        context,
                        0,
                        -(k + 1),
                        1,
                        -k,
                        0,
                        rgb,
                        past_color
                );

            }
            past_color = rgb;
        }
        popMatrix(context);
    }

    public static void drawGradientVerticalBar(DrawContext context, int itemBarStep) {
        int past_color = Color.HSBtoRGB(1, 1f, 1f) | 0xFF000000;
        for (int k = 0; k < 13; k++) {
            float hue = (float) k / 13f;
            int rgb = Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
            if (k < itemBarStep) {
                fillGradientCompat(
                        context,
                        0,
                        13 - (k + 1),
                        1,
                        13 - k,
                        0,
                        rgb,
                        past_color
                );
            }
            past_color = rgb;
        }
    }

    public static void drawGradientBar(DrawContext context, int startX, int startY, int itemBarStep){
        drawGradientBar(context, DurabilityConfig.builder().fromModConfig().build(), startX, startY, itemBarStep) ;
    }

    public static void drawGradientBar(DrawContext context, DurabilityConfig config, int startX, int startY, int itemBarStep) {
        if (config.isVertical) {
            if (config.showDurabilityBarBackground) {fill(context, 0, 0, 2, 13, -16777216);}
            drawGradientVerticalBar(context, itemBarStep);
        } else {
            if (config.showDurabilityBarBackground) {fill(context, 0, 0, 13, 2, -16777216);}
            drawGradientHorizontalBar(context, itemBarStep);
        }
    }

    public static void drawBar(DrawContext context, DurabilityConfig config, DurabilityData data, int startX, int startY){
        if (config.doRgbBar){
            drawGradientBar(context, config, startX, startY, data.barStep);
            return;
        }

        if (config.isVertical){
            if (config.showDurabilityBarBackground) {fill(context, 0, 0, 2, 13, -16777216);}
            fill(context, 0, 13, 1, -data.barStep, DrawUtils.interpolateColorHSV(config.durabilityBarColorMin, config.durabilityBarColor, data.getPercentsInt()));
        } else {
            if (config.showDurabilityBarBackground) {fill(context, 0, 0, 13, 2, -16777216);}
            fill(context, 0 , 0, data.barStep, 1, DrawUtils.interpolateColorHSV(config.durabilityBarColorMin, config.durabilityBarColor, data.getPercentsInt()));
        }
    }

    public static void drawTextInfo(DrawContext drawContext, DurabilityConfig config, int color, String text, int x, int y, float scale) {
        DrawUtils.pushMatrix(drawContext);

        float targetX = x + 9;
        float targetY = y + 11;

        DrawUtils.translateMatrix(drawContext, targetX, targetY);
        DrawUtils.scaleMatrix(drawContext, scale, scale);

        drawContext.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                text,
                0,
                0,
                color
        );

        DrawUtils.popMatrix(drawContext);
    }

    public static void pushMatrix(DrawContext context) {
        invokeMatrixNoArgs(context, "pushMatrix", "push");
    }

    public static void popMatrix(DrawContext context) {
        invokeMatrixNoArgs(context, "popMatrix", "pop");
    }

    public static void rotateMatrix(DrawContext context, float deg) {
        Object matrices = context.getMatrices();
        float radians = (float) Math.toRadians(deg);
        if (invokeMatrixFloat(matrices, "rotate", radians)) {
            return;
        }
        invokeMatrixObject(matrices, "multiply", RotationAxis.POSITIVE_Z.rotationDegrees(deg));
    }

    public static void translateMatrix(DrawContext context, int x, int y) {
        translateMatrix(context, (float) x, (float) y);
    }

    public static void translateMatrix(DrawContext context, float x, float y) {
        Object matrices = context.getMatrices();
        if (invokeMatrixFloatFloat(matrices, "translate", x, y)) {
            return;
        }
        if (invokeMatrixDoubleDouble(matrices, "translate", x, y)) {
            return;
        }
        if (invokeMatrixFloatFloatFloat(matrices, "translate", x, y, 200.0F)) {
            return;
        }
        if (invokeMatrixDoubleDoubleDouble(matrices, "translate", x, y, 200.0F)) {
            return;
        }
        throw new RuntimeException("No compatible translate method found on matrix stack");
    }

    public static void scaleMatrix(DrawContext context, int x, int y) {
        scaleMatrix(context, (float) x, (float) y);
    }

    public static void scaleMatrix(DrawContext context, float x, float y) {
        Object matrices = context.getMatrices();
        if (invokeMatrixFloatFloat(matrices, "scale", x, y)) {
            return;
        }
        if (invokeMatrixDoubleDouble(matrices, "scale", x, y)) {
            return;
        }
        if (invokeMatrixFloatFloatFloat(matrices, "scale", x, y, 1.0F)) {
            return;
        }
        if (invokeMatrixDoubleDoubleDouble(matrices, "scale", x, y, 1.0F)) {
            return;
        }
        throw new RuntimeException("No compatible scale method found on matrix stack");
    }

    public static void fill(DrawContext context, int x, int y, int width, int height, int color) {
        fillCompat(context, x, y, x + width, y + height, 200, color, getGuiLayer());
    }

    public static void fillOverlay(DrawContext context, int x, int y, int width, int height, int color) {
        fillCompat(context, x, y, x + width, y + height, Integer.MAX_VALUE, color, getGuiOverlayLayer());
    }

    private static void fillGradientCompat(DrawContext context, int x1, int y1, int x2, int y2, int z, int colorStart, int colorEnd) {
        if (!fillGradientChecked) {
            fillGradientChecked = true;
            try {
                fillGradient6 = DrawContext.class.getMethod("fillGradient", int.class, int.class, int.class, int.class, int.class, int.class);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                fillGradient7 = DrawContext.class.getMethod("fillGradient", int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        try {
            if (fillGradient6 != null) {
                fillGradient6.invoke(context, x1, y1, x2, y2, colorStart, colorEnd);
                return;
            }
            if (fillGradient7 != null) {
                fillGradient7.invoke(context, x1, y1, x2, y2, z, colorStart, colorEnd);
                return;
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("No compatible fillGradient method found on DrawContext");
    }

    private static void fillCompat(DrawContext context, int x1, int y1, int x2, int y2, int z, int color, RenderLayer layer) {
        if (!fillChecked) {
            fillChecked = true;
            try {
                fill5 = DrawContext.class.getMethod("fill", int.class, int.class, int.class, int.class, int.class);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                fill7 = DrawContext.class.getMethod("fill", RenderLayer.class, int.class, int.class, int.class, int.class, int.class, int.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        try {
            if (fill5 != null) {
                fill5.invoke(context, x1, y1, x2, y2, color);
                return;
            }
            if (fill7 != null) {
                fill7.invoke(context, layer, x1, y1, x2, y2, z, color);
                return;
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("No compatible fill method found on DrawContext");
    }

    private static RenderLayer getGuiLayer() {
        if (!guiLayerChecked) {
            guiLayerChecked = true;
            try {
                getGuiLayer = RenderLayer.class.getMethod("getGui");
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (getGuiLayer == null) {
            throw new RuntimeException("RenderLayer.getGui is not available");
        }
        try {
            return (RenderLayer) getGuiLayer.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static RenderLayer getGuiOverlayLayer() {
        if (!guiOverlayChecked) {
            guiOverlayChecked = true;
            try {
                getGuiOverlayLayer = RenderLayer.class.getMethod("getGuiOverlay");
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (getGuiOverlayLayer == null) {
            throw new RuntimeException("RenderLayer.getGuiOverlay is not available");
        }
        try {
            return (RenderLayer) getGuiOverlayLayer.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeMatrixNoArgs(DrawContext context, String primary, String fallback) {
        Object matrices = context.getMatrices();
        if (invokeMatrixNoArgs(matrices, primary)) {
            return;
        }
        if (invokeMatrixNoArgs(matrices, fallback)) {
            return;
        }
        throw new RuntimeException("No compatible matrix method found: " + primary + "/" + fallback);
    }

    private static boolean invokeMatrixNoArgs(Object matrices, String name) {
        try {
            Method method = matrices.getClass().getMethod(name);
            method.invoke(matrices);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeMatrixFloat(Object matrices, String name, float value) {
        try {
            Method method = matrices.getClass().getMethod(name, float.class);
            method.invoke(matrices, value);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeMatrixObject(Object matrices, String name, Object value) {
        try {
            Method method = matrices.getClass().getMethod(name, value.getClass());
            method.invoke(matrices, value);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeMatrixFloatFloat(Object matrices, String name, float x, float y) {
        try {
            Method method = matrices.getClass().getMethod(name, float.class, float.class);
            method.invoke(matrices, x, y);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeMatrixDoubleDouble(Object matrices, String name, float x, float y) {
        try {
            Method method = matrices.getClass().getMethod(name, double.class, double.class);
            method.invoke(matrices, (double) x, (double) y);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeMatrixFloatFloatFloat(Object matrices, String name, float x, float y, float z) {
        try {
            Method method = matrices.getClass().getMethod(name, float.class, float.class, float.class);
            method.invoke(matrices, x, y, z);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean invokeMatrixDoubleDoubleDouble(Object matrices, String name, float x, float y, float z) {
        try {
            Method method = matrices.getClass().getMethod(name, double.class, double.class, double.class);
            method.invoke(matrices, (double) x, (double) y, (double) z);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
