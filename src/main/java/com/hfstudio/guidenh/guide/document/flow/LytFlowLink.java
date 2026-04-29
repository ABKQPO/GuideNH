package com.hfstudio.guidenh.guide.document.flow;

import java.awt.Desktop;
import java.net.URI;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytFlowLink extends LytTooltipSpan {

    @Nullable
    private Consumer<GuideUiHost> clickCallback;

    @Nullable
    private String clickSound = "gui.button.press";

    public LytFlowLink() {
        modifyStyle(style -> style.color(SymbolicColor.LINK));
        modifyHoverStyle(style -> style.underlined(true));
    }

    public void setClickCallback(@Nullable Consumer<GuideUiHost> clickCallback) {
        this.clickCallback = clickCallback;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button == 0 && clickCallback != null) {
            clickCallback.accept(screen);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(GuideUiHost screen, int x, int y, int button) {
        return false;
    }

    public @Nullable String getClickSound() {
        return clickSound;
    }

    public void setClickSound(@Nullable String clickSound) {
        this.clickSound = clickSound;
    }

    public void setExternalUrl(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("External URLs must be absolute: " + uri);
        }
        setClickCallback(screen -> {
            try {
                Desktop.getDesktop()
                    .browse(uri);
            } catch (Exception e) {
                // ignore
            }
        });
    }

    public void setPageLink(PageAnchor anchor) {
        setClickCallback(screen -> screen.navigateTo(anchor));
    }
}
