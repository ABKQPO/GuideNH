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

/**
 * Captures which BetterQuesting quest panel is currently under the mouse cursor.
 * <p/>
 * The hovered UUID is published to {@link BqCompat#setCurrentHoveredQuestUuid(UUID)} so that
 * the {@code OpenGuideHotkey} handler can navigate to the corresponding guide page when the
 * player holds the open-guide key while pointing at a quest in the BQ quest-line GUI.
 * <p/>
 * Each panel checks itself every render frame: if the mouse is inside its bounds it sets the
 * hover UUID; otherwise, if the global hover UUID still equals this panel's quest, it clears
 * it. This converges to the correct value within one frame even when many panels exist.
 */
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
