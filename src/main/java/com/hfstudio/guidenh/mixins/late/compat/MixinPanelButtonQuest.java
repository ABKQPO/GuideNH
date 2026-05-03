package com.hfstudio.guidenh.mixins.late.compat;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hfstudio.guidenh.compat.betterquesting.BqCompat;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.controls.PanelButtonQuest;

@Mixin(value = PanelButtonQuest.class, remap = false)
public abstract class MixinPanelButtonQuest {

    @Inject(method = "drawPanel(IIF)V", at = @At("HEAD"), remap = false, require = 0)
    private void guidenh$captureQuestHover(int mx, int my, float partialTick, CallbackInfo ci) {
        PanelButtonQuest self = (PanelButtonQuest) (Object) this;
        Map.Entry<UUID, IQuest> stored = self.getStoredValue();
        if (stored == null) {
            return;
        }
        UUID id = stored.getKey();
        if (self.getTransform()
            .contains(mx, my)) {
            BqCompat.setCurrentHoveredQuestUuid(id);
        } else if (Objects.equals(BqCompat.getCurrentHoveredQuestUuid(), id)) {
            BqCompat.setCurrentHoveredQuestUuid(null);
        }
    }
}
