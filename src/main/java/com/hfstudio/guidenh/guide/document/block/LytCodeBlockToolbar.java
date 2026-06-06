package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytPoint;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.LytSize;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.markdown.highlight.CodeHighlightTheme;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuiSprite;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytCodeBlockToolbar extends LytBox implements InteractiveElement {

    private static final GuiSprite COPY_SPRITE = new GuiSprite(
        GuideIconButton.TEX,
        0,
        48,
        16,
        16,
        GuideIconButton.TEXTURE_SIZE,
        GuideIconButton.TEXTURE_SIZE);
    private static final long COPY_TOOLTIP_RESET_DELAY_MILLIS = 1500L;
    private static final int TEXT_CENTERING_OFFSET_Y = 1;
    private static final CodeHighlightTheme CODE_THEME = CodeHighlightTheme.GITHUB_DARK_DEFAULT;
    private static final ConstantColor DEFAULT_TOOLBAR_BACKGROUND = new ConstantColor(
        CODE_THEME.toolbarBackgroundArgb());
    private static final ConstantColor DEFAULT_TOOLBAR_BORDER = new ConstantColor(CODE_THEME.borderArgb());
    private static final ConstantColor DEFAULT_TOOLBAR_TEXT = new ConstantColor(CODE_THEME.toolbarTextArgb());

    private final LytParagraph languageLabel = new LytParagraph();
    private final LytGuiSprite copyButton = new LytGuiSprite(COPY_SPRITE, new LytSize(16, 16));

    private ColorValue toolbarBackground = DEFAULT_TOOLBAR_BACKGROUND;
    private ColorValue toolbarBorder = DEFAULT_TOOLBAR_BORDER;
    private ColorValue toolbarText = DEFAULT_TOOLBAR_TEXT;

    private String copyText = "";
    private boolean copied;
    private long copiedUntilMillis;
    private int preferredWidth;
    private boolean copyButtonVisible = true;

    public LytCodeBlockToolbar() {
        languageLabel.setMarginTop(0);
        languageLabel.setMarginBottom(0);
        languageLabel.modifyStyle(
            style -> style.bold(true)
                .color(toolbarText));
        copyButton.setColor(toolbarText);
        copyButton.setHoverColor(SymbolicColor.ICON_BUTTON_HOVER);
        append(languageLabel);
        append(copyButton);
        setPaddingLeft(8);
        setPaddingTop(4);
        setPaddingRight(8);
        setPaddingBottom(4);
        setBorderBottom(new BorderStyle(toolbarBorder, 1));
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

    public void setCopyButtonVisible(boolean copyButtonVisible) {
        this.copyButtonVisible = copyButtonVisible;
    }

    public void setToolbarBackground(ColorValue toolbarBackground) {
        this.toolbarBackground = toolbarBackground != null ? toolbarBackground : DEFAULT_TOOLBAR_BACKGROUND;
    }

    public void setToolbarBorder(ColorValue toolbarBorder) {
        this.toolbarBorder = toolbarBorder != null ? toolbarBorder : DEFAULT_TOOLBAR_BORDER;
        setBorderBottom(new BorderStyle(this.toolbarBorder, getBorderBottom().width()));
    }

    public void setToolbarText(ColorValue toolbarText) {
        this.toolbarText = toolbarText != null ? toolbarText : DEFAULT_TOOLBAR_TEXT;
        languageLabel.modifyStyle(style -> style.color(this.toolbarText));
        copyButton.setColor(this.toolbarText);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int toolbarWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        int labelWidth = Math.max(1, toolbarWidth - (copyButtonVisible ? 16 + 8 : 0));
        LytRect labelBounds = languageLabel.layout(context, x, y, labelWidth);
        int buttonX = x + Math.max(0, toolbarWidth - 16);
        LytRect buttonBounds = copyButtonVisible ? copyButton.layout(context, buttonX, y, 16) : LytRect.empty();
        int height = Math.max(16, Math.max(labelBounds.height(), copyButtonVisible ? buttonBounds.height() : 0));
        float labelY = y + (height - labelBounds.height()) / 2f + TEXT_CENTERING_OFFSET_Y;
        languageLabel.setLayoutPos(new LytPoint(labelBounds.x(), labelY));
        if (copyButtonVisible) {
            copyButton.setLayoutPos(new LytPoint(buttonX, y + (height - buttonBounds.height()) / 2f));
        } else {
            copyButton.setLayoutPos(new LytPoint(x + toolbarWidth, y));
        }
        return new LytRect(x, y, toolbarWidth, height);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (!copyButtonVisible) {
            return false;
        }
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
        if (!copyButtonVisible) {
            return Optional.empty();
        }
        LytRect bounds = copyButton.getBounds();
        if (bounds != null && bounds.contains((int) x, (int) y)) {
            return Optional.of(new TextTooltip(getCopyTooltipText()));
        }
        return Optional.empty();
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    @Override
    public void render(RenderContext context) {
        context.fillRect(bounds, toolbarBackground);
        languageLabel.render(context);
        if (copyButtonVisible) {
            copyButton.render(context);
        }
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            new BorderRenderer()
                .render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
        }
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
