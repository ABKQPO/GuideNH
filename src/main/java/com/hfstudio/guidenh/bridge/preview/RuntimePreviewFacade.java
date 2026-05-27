package com.hfstudio.guidenh.bridge.preview;

public class RuntimePreviewFacade {

    private final ItemPreviewSearchService itemPreviewSearchService;
    private final ItemPreviewService itemPreviewService;

    public RuntimePreviewFacade(ItemPreviewSearchService itemPreviewSearchService,
        ItemPreviewService itemPreviewService) {
        this.itemPreviewSearchService = itemPreviewSearchService;
        this.itemPreviewService = itemPreviewService;
    }

    public PreviewSearchResult search(PreviewSearchQuery query) {
        return itemPreviewSearchService.search(query);
    }

    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        return itemPreviewService.resolve(query);
    }
}
