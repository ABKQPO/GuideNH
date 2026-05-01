package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytPoint;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytCodeBlockToolbar extends LytBox implements InteractiveElement {

    private static final GuiSprite COPY_SPRITE = new GuiSprite(GuideIconButton.TEX, 0, 48, 16, 16, 64, 64);
    private static final long COPY_TOOLTIP_RESET_DELAY_MILLIS = 1500L;

    private final LytParagraph languageLabel = new LytParagraph();
    private final LytGuiSprite copyButton = new LytGuiSprite(COPY_SPRITE, new LytSize(16, 16));

    private String copyText = "";
    private boolean copied;
    private long copiedUntilMillis;
    private int preferredWidth;

    public LytCodeBlockToolbar() {
        languageLabel.setMarginTop(0);
        languageLabel.setMarginBottom(0);
        languageLabel.modifyStyle(
            style -> style.bold(true)
                .color(new ConstantColor(0xFFB8BEC9)));
        copyButton.setColor(SymbolicColor.ICON_BUTTON_NORMAL);
        append(languageLabel);
        append(copyButton);
        setPaddingBottom(4);
        setBorderBottom(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));
    }

    public void setLanguageDisplayName(String languageDisplayName) {
        languageLabel.clearContent();
        languageLabel
            .appendText(languageDisplayName != null && !languageDisplayName.isEmpty() ? languageDisplayName : "Text");
    }

    public void setCopyText(String copyText) {
        this.copyText = copyText != null ? copyText : "";
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int toolbarWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        int labelWidth = Math.max(1, toolbarWidth - 16 - 8);
        LytRect labelBounds = languageLabel.layout(context, x, y, labelWidth);
        int buttonX = x + Math.max(0, toolbarWidth - 16);
        LytRect buttonBounds = copyButton.layout(context, buttonX, y, 16);
        int height = Math.max(labelBounds.height(), buttonBounds.height());
        languageLabel.setLayoutPos(new LytPoint(labelBounds.x(), y + (height - labelBounds.height()) / 2f));
        copyButton.setLayoutPos(new LytPoint(buttonX, y + (height - buttonBounds.height()) / 2f));
        return new LytRect(x, y, toolbarWidth, height);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        LytRect bounds = copyButton.getBounds();
        if (button != 0 || bounds == null || !bounds.contains(x, y)) {
            return false;
        }
        boolean success = screen.copyCodeBlock(copyText);
        if (success) {
            markCopied();
        }
        return success;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        LytRect bounds = copyButton.getBounds();
        if (bounds != null && bounds.contains((int) x, (int) y)) {
            return Optional.of(new TextTooltip(getCopyTooltipText()));
        }
        return Optional.empty();
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    private void markCopied() {
        copied = true;
        copiedUntilMillis = System.currentTimeMillis() + COPY_TOOLTIP_RESET_DELAY_MILLIS;
    }

    private String getCopyTooltipText() {
        if (copied && System.currentTimeMillis() < copiedUntilMillis) {
            return GuidebookText.CodeBlockCopySuccess.text();
        }
        copied = false;
        return GuidebookText.CodeBlockCopy.text();
    }
}
