package com.hfstudio.guidenh.guide.layout.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.LytRect;

@Desugar
record Line(LytRect bounds, LineElement firstElement) {

    Stream<LineElement> elements() {
        // Java 8 compatible: manual iteration
        List<LineElement> elems = new ArrayList<>();
        LineElement cur = firstElement;
        while (cur != null) {
            elems.add(cur);
            cur = cur.next;
        }
        return elems.stream();
    }
}
