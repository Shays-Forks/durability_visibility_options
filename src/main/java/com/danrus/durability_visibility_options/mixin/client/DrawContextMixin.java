package com.danrus.durability_visibility_options.mixin.client;

import com.danrus.durability_visibility_options.client.DurabilityData;
import com.danrus.durability_visibility_options.client.DurabilityRender;
import com.danrus.durability_visibility_options.client.config.DurabilityConfig;
import com.danrus.durability_visibility_options.client.config.ModConfig;
import com.danrus.durability_visibility_options.client.utils.DrawUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    private static final boolean HAS_DRAW_ITEM_IN_SLOT = hasDrawItemInSlot();

    @Shadow public abstract void drawText(TextRenderer par1, Text par2, int par3, int par4, int par5, boolean par6);

    @Invoker("drawItemBar")
    protected abstract void invokeDrawItemBar(ItemStack stack, int x, int y);

    //? if >1.21.1 {
    /*@Redirect(

            method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawItemBar(Lnet/minecraft/item/ItemStack;II)V"
            )

    )
    private void drawStackOverlayMixin(DrawContext instance, ItemStack stack, int x, int y) {
        String countOverride = stack.getCount() == 1 ? null : String.valueOf(stack.getCount());
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        drawItemInSlotMixin(textRenderer, stack, x, y, countOverride, null);
    }
    *///?}

    //? if <=1.21.1 {
    @Inject(
            method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isItemBarVisible()Z"),
            cancellable = true,
            require = 0)

    //?}
    private void drawItemInSlotMixin(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        DrawContext thisObject = (DrawContext) (Object) this;
        if (stack.isDamaged() && ModConfig.get().showDurability) {
            DurabilityRender.renderBar(thisObject, stack, x, y);
        }

        if (stack.isDamaged() &&  ModConfig.get().showDurabilityPercent ) {
            DurabilityRender.renderPercents(thisObject, stack, x, y);
        }

        if (stack.isDamaged() && ModConfig.get().showDurabilityAmount) {
            DurabilityRender.renderAmount(thisObject, new DurabilityData(stack), x, y, DurabilityConfig.fromModConfig());
        }
        //? if <=1.21.1 {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        float tickDelta = getTickDeltaCompat();
        float f = clientPlayerEntity == null ? 0.0F : getCooldownProgressCompat(clientPlayerEntity, stack, tickDelta);
        if (f > 0.0F) {
            int k = y + MathHelper.floor(16.0F * (1.0F - f));
            int l = k + MathHelper.ceil(16.0F * f);
        DrawUtils.fillOverlay(thisObject, x, k, 16, l - k, Integer.MAX_VALUE);
        }

        DrawUtils.popMatrix(thisObject);
        ci.cancel();

        //?}
    }

    @Inject(
            method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void drawStackOverlayMixin(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        if (HAS_DRAW_ITEM_IN_SLOT) {
            return;
        }

        DrawContext thisObject = (DrawContext) (Object) this;
        if (stack.isDamaged() && ModConfig.get().showDurabilityPercent) {
            DurabilityRender.renderPercents(thisObject, stack, x, y);
        }
        if (stack.isDamaged() && ModConfig.get().showDurabilityAmount) {
            DurabilityRender.renderAmount(thisObject, new DurabilityData(stack), x, y, DurabilityConfig.fromModConfig());
        }
    }

    @Redirect(
            method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawItemBar(Lnet/minecraft/item/ItemStack;II)V"
            ),
            require = 0
    )
    private void drawItemBarRedirect(DrawContext instance, ItemStack stack, int x, int y) {
        if (!HAS_DRAW_ITEM_IN_SLOT && stack.isDamaged() && ModConfig.get().showDurability) {
            DurabilityConfig config = DurabilityConfig.fromModConfig();
            DurabilityData data = new DurabilityData(stack);
            DurabilityRender.renderBar(instance, data, x, y, config);
            return;
        }

        ((DrawContextMixin) (Object) instance).invokeDrawItemBar(stack, x, y);
    }

    @Inject(
            method = "drawItemBar(Lnet/minecraft/item/ItemStack;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void drawItemBarMixin(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (HAS_DRAW_ITEM_IN_SLOT) {
            return;
        }

        DrawContext thisObject = (DrawContext) (Object) this;
        if (stack.isDamaged() && ModConfig.get().showDurability) {
            DurabilityRender.renderBar(thisObject, stack, x, y);
        }
        ci.cancel();
    }

    private static boolean hasDrawItemInSlot() {
        try {
            DrawContext.class.getMethod(
                    "drawItemInSlot",
                    TextRenderer.class,
                    ItemStack.class,
                    int.class,
                    int.class,
                    String.class
            );
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private static float getTickDeltaCompat() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Object counter = client.getRenderTickCounter();
            try {
                return (float) counter.getClass().getMethod("getTickDelta", boolean.class).invoke(counter, true);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return (float) counter.getClass().getMethod("getTickDelta").invoke(counter);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            return (float) client.getClass().getMethod("getTickDelta").invoke(client);
        } catch (ReflectiveOperationException e) {
            return 0.0F;
        }
    }

    private static float getCooldownProgressCompat(ClientPlayerEntity player, ItemStack stack, float tickDelta) {
        Object manager = player.getItemCooldownManager();
        try {
            return (float) manager.getClass()
                    .getMethod("getCooldownProgress", ItemStack.class, float.class)
                    .invoke(manager, stack, tickDelta);
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Item item = stack.getItem();
            return (float) manager.getClass()
                    .getMethod("getCooldownProgress", Item.class, float.class)
                    .invoke(manager, item, tickDelta);
        } catch (ReflectiveOperationException e) {
            return 0.0F;
        }
    }
}
