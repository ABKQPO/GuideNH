package com.hfstudio.guidenh.guide.internal;

import com.hfstudio.guidenh.guide.internal.data.LocalizationEnum;

public enum GuidebookText implements LocalizationEnum {

    HistoryGoBack,
    HistoryGoForward,
    Close,
    HoldToShow,
    HideAnnotations,
    ShowAnnotations,
    ZoomIn,
    ZoomOut,
    ResetView,
    Search,
    SearchNoQuery,
    SearchNoResults,
    SearchPlaceholder,
    SearchNoMatch,
    ContentFrom,
    ItemNoGuideId,
    ItemInvalidGuideId,
    CommandOnlyWorksInSinglePlayer,
    CommandUsage,
    CommandOpenUsage,
    CommandSearchUsage,
    CommandExportUsage,
    CommandListHeader,
    CommandListEntry,
    CommandGuideNotFound,
    CommandReloaded,
    CommandReloadUnsupported,
    CommandSearchNoResults,
    CommandSearchResults,
    CommandSearchResult,
    CommandSearchFailure,
    CommandExportStart,
    CommandExportSuccess,
    CommandExportFailure,
    PageNotFound,
    RegionWandChatPos,
    RegionWandNeedTwoCorners,
    RegionWandAreaTooLarge,
    RegionWandCopied,
    RegionWandCopyFailed,
    RegionWandTooltipSelect,
    RegionWandTooltipExport,
    RegionWandTooltipPos,
    Smelting,
    Blasting,
    ShapelessCrafting,
    Crafting,
    FullWidthView,
    CloseFullWidthView,
    RunsCommand;

    @Override
    public String getTranslationKey() {
        return "guideme.guidebook." + name();
    }
}
