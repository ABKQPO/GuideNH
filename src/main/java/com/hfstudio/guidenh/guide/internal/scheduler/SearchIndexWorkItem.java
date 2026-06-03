package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;

public class SearchIndexWorkItem implements WorkItem {

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    @Override
    public boolean shouldRun() {
        return true;
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        GuideME.getSearch()
            .processWork(GuideSearch.BACKGROUND_TIME_PER_TICK);
        return WorkResult.DONE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SearchIndexWorkItem;
    }

    @Override
    public int hashCode() {
        return SearchIndexWorkItem.class.hashCode();
    }
}
