package com.hfstudio.guidenh.guide.document.flow;

import java.net.URI;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytFlowLink extends LytTooltipSpan {

    @Nullable
    private Consumer<GuideUiHost> clickCallback;
    @Nullable
    private URI externalUrl;
    @Nullable
    private PageAnchor pageAnchor;

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
<<<<<<< HEAD
        this.externalUrl = uri;
        this.pageAnchor = null;
        setClickCallback(screen -> {
            try {
                Desktop.getDesktop()
                    .browse(uri);
            } catch (Exception e) {
                // ignore
            }
        });
=======
        setClickCallback(screen -> screen.openExternalUrl(uri));
>>>>>>> origin/master
    }

    public void setPageLink(PageAnchor anchor) {
        this.pageAnchor = anchor;
        this.externalUrl = null;
        setClickCallback(screen -> screen.navigateTo(anchor));
    }

    @Nullable
    public URI getExternalUrl() {
        return externalUrl;
    }

    @Nullable
    public PageAnchor getPageAnchor() {
        return pageAnchor;
    }
}
