package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableCell;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.MdAstDelete;
import com.hfstudio.guidenh.libs.mdast.guidemark.MdAstMark;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstDottedUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstWavyUnderline;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImageReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLinkReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdast.model.MdAstThematicBreak;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.Align;

public final class MdAstToMdxConverter {

    private MdAstToMdxConverter() {}

    /**
     * @param definitions pre-collected link/image reference definitions (from GuideMarkdownDefinitions.collect())
     */
    public static void convert(MdAstRoot root, Map<String, MdAstDefinition> definitions) {
        convertParent(root, definitions);
    }

    private static void convertParent(MdAstParent<?> parent, Map<String, MdAstDefinition> definitions) {
        // First: depth-first recursion into all MdAstParent children, using a snapshot
        // to safely handle concurrent modification.
        List<?> children = parent.children();
        for (Object child : new ArrayList<>(children)) {
            if (child instanceof MdAstParent<?>childParent) {
                convertParent(childParent, definitions);
            }
        }

        // Then convert the current level's children in-place.
        if (isPhrasingParent(parent)) {
            convertPhrasingChildren(parent.children(), definitions);
        } else {
            convertFlowChildren(castAnyChildren(parent.children()), definitions);
        }
    }

    private static boolean isPhrasingParent(MdAstParent<?> parent) {
        if (parent instanceof MdAstParagraph || parent instanceof MdxJsxTextElement) {
            return true;
        }
        // New MdxJsxFlowElement containers that hold phrasing/inline children
        if (parent instanceof MdxJsxFlowElement el) {
            String name = el.name();
            return name != null && PHRASING_CONTAINER_NAMES.contains(name);
        }
        String type = parent.type();
        return "link".equals(type) || "strong".equals(type)
            || "emphasis".equals(type)
            || "delete".equals(type)
            || "heading".equals(type);
    }

    // Containers whose children are inline/phrasing content only
    private static final java.util.Set<String> PHRASING_CONTAINER_NAMES = new java.util.HashSet<>(
        java.util.Arrays.asList(
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "td",
            "th",
            "summary",
            "a",
            "strong",
            "em",
            "del",
            "u",
            "wavy",
            "dotted",
            "mark",
            "code",
            "span"));

    @SuppressWarnings("unchecked")
    private static List<MdAstAnyContent> castAnyChildren(List<?> children) {
        return (List<MdAstAnyContent>) (List<?>) children;
    }

    // -----------------------------------------------------------------------
    // Phrasing (inline) children conversion — also handles block nodes that
    // may appear inside phrasing containers (e.g. MdAstParagraph inside <td>).
    // -----------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void convertPhrasingChildren(List<?> children, Map<String, MdAstDefinition> definitions) {
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            Object replacement = null;

            // Inline phrasing types → MdxJsxTextElement
            if (child instanceof MdAstStrong) {
                replacement = createText("strong", ((MdAstStrong) child).children());
            } else if (child instanceof MdAstEmphasis) {
                replacement = createText("em", ((MdAstEmphasis) child).children());
            } else if (child instanceof MdAstDelete) {
                replacement = createText("del", ((MdAstDelete) child).children());
            } else if (child instanceof MdAstUnderline) {
                replacement = createText("u", ((MdAstUnderline) child).children());
            } else if (child instanceof MdAstWavyUnderline) {
                replacement = createText("wavy", ((MdAstWavyUnderline) child).children());
            } else if (child instanceof MdAstDottedUnderline) {
                replacement = createText("dotted", ((MdAstDottedUnderline) child).children());
            } else if (child instanceof MdAstMark) {
                replacement = createText("mark", ((MdAstMark) child).children());
            } else if (child instanceof MdAstLink link) {
                MdxJsxTextElement el = createText("a", link.children());
                el.addAttribute("href", link.url());
                replacement = el;
            } else if (child instanceof MdAstLinkReference ref) {
                MdxJsxTextElement el = createText("a", ref.children());
                MdAstDefinition def = definitions.get(ref.identifier());
                el.addAttribute("href", def != null ? def.url() : "");
                replacement = el;
            } else if (child instanceof MdAstImage image) {
                MdxJsxTextElement el = createText("img", new ArrayList<>());
                el.addAttribute("src", image.url());
                if (image.alt != null) {
                    el.addAttribute("alt", image.alt);
                }
                if (image.title != null) {
                    el.addAttribute("title", image.title);
                }
                replacement = el;
            } else if (child instanceof MdAstImageReference ref) {
                MdxJsxTextElement el = createText("img", new ArrayList<>());
                MdAstDefinition def = definitions.get(ref.identifier());
                if (def != null) {
                    el.addAttribute("src", def.url());
                    if (def.title != null) {
                        el.addAttribute("title", def.title);
                    }
                } else {
                    el.addAttribute("src", "");
                }
                if (ref.alt != null) {
                    el.addAttribute("alt", ref.alt);
                }
                replacement = el;
            } else if (child instanceof MdAstInlineCode code) {
                MdxJsxTextElement el = createText("code", new ArrayList<>());
                MdAstText text = new MdAstText();
                text.setValue(code.value);
                addChildRaw(el, text);
                replacement = el;
            } else if (child instanceof MdAstHTML html) {
                MdxJsxTextElement el = createText("span", new ArrayList<>());
                MdAstText text = new MdAstText();
                text.setValue(html.value);
                addChildRaw(el, text);
                replacement = el;
            } else if (child instanceof MdAstBreak) {
                replacement = createText("br", new ArrayList<>());
            }
            // Flow block types that can appear inside phrasing containers
            else if (child instanceof MdAstParagraph p) {
                replacement = createFlow("p", p.children());
            }

            if (replacement != null) {
                ((List) children).set(i, replacement);
                if (replacement instanceof MdxJsxTextElement) {
                    convertParent((MdxJsxTextElement) replacement, definitions);
                } else if (replacement instanceof MdxJsxFlowElement) {
                    convertParent((MdxJsxFlowElement) replacement, definitions);
                }
            }
            // MdAstText, MdxJsxTextElement, MdxJsxFlowElement: silently pass through
        }
    }

    // -----------------------------------------------------------------------
    // Flow (block) children conversion
    // -----------------------------------------------------------------------

    private static void convertFlowChildren(List<MdAstAnyContent> children, Map<String, MdAstDefinition> definitions) {
        for (int i = 0; i < children.size(); i++) {
            MdAstAnyContent child = children.get(i);
            MdxJsxFlowElement replacement = null;

            if (child instanceof MdAstParagraph p) {
                String kramdownMeta = extractKramdownMeta(p);
                if (kramdownMeta != null) {
                    replacement = new MdxJsxFlowElement();
                    replacement.setName("table-meta");
                    replacement.addAttribute("content", kramdownMeta);
                } else {
                    replacement = createFlow("p", p.children());
                }
            } else if (child instanceof MdAstHeading h) {
                MdxJsxFlowElement el = createFlow("h" + h.depth, h.children());
                el.addAttribute("depth", h.depth);
                replacement = el;
            } else if (child instanceof MdAstList list) {
                String name = list.ordered ? "ol" : "ul";
                MdxJsxFlowElement el = createFlow(name, list.children());
                if (list.ordered && list.start != 1) {
                    el.addAttribute("start", list.start);
                }
                replacement = el;
            } else if (child instanceof MdAstListItem item) {
                replacement = createFlow("li", item.children());
            } else if (child instanceof MdAstCode code) {
                MdxJsxFlowElement el = createFlow("pre", new ArrayList<>());
                if (code.lang != null) {
                    el.addAttribute("lang", code.lang);
                }
                if (code.meta != null) {
                    el.addAttribute("meta", code.meta);
                }
                MdAstText text = new MdAstText();
                text.setValue(code.value);
                addChildRaw(el, text);
                replacement = el;
            } else if (child instanceof MdAstBlockquote bq) {
                replacement = createFlow("blockquote", bq.children());
            } else if (child instanceof GfmTable table) {
                MdxJsxFlowElement el = createFlow("table", table.children());
                String alignStr = serializeAlign(table.align);
                if (alignStr != null) {
                    el.addAttribute("align", alignStr);
                }
                replacement = el;
            } else if (child instanceof GfmTableRow row) {
                replacement = createFlow("tr", row.children());
            } else if (child instanceof GfmTableCell cell) {
                replacement = createFlow("td", cell.children());
            } else if (child instanceof MdAstThematicBreak) {
                replacement = createFlow("hr", new ArrayList<>());
            } else if (child instanceof MdAstDefinition def) {
                MdxJsxFlowElement el = createFlow("definition", new ArrayList<>());
                if (def.identifier != null) {
                    el.addAttribute("identifier", def.identifier);
                }
                if (def.url != null) {
                    el.addAttribute("url", def.url);
                }
                if (def.title != null) {
                    el.addAttribute("title", def.title);
                }
                replacement = el;
            } else if (child instanceof MdAstYamlFrontmatter) {
                // Remove from children
                children.remove(i);
                i--;
                continue;
            } else if (child instanceof MdAstHTML html) {
                MdxJsxFlowElement el = createFlow("div", new ArrayList<>());
                MdAstText text = new MdAstText();
                text.setValue(html.value);
                addChildRaw(el, text);
                replacement = el;
            }

            if (replacement != null) {
                children.set(i, replacement);
                convertParent(replacement, definitions);
            }
            // Already-converted types (MdxJsxFlowElement, MdxJsxTextElement) and
            // leaf nodes (MdAstText) are silently passed through.
        }
    }

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a flow element with the given tag name and children.
     * <p>
     * Uses raw-type list access to bypass the generic type check so that phrasing
     * content (e.g. {@link MdxJsxTextElement}, {@link MdAstText}) can be placed
     * inside flow elements where they are semantically valid (e.g. text inside
     * {@code 
     * 
    <p>
     * }).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static MdxJsxFlowElement createFlow(String name, List<? extends MdAstAnyContent> children) {
        MdxJsxFlowElement element = new MdxJsxFlowElement();
        element.setName(name);
        List rawChildren = element.children();
        for (MdAstAnyContent child : children) {
            rawChildren.add(child);
        }
        return element;
    }

    /**
     * Creates a text (inline) element with the given tag name and children.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static MdxJsxTextElement createText(String name, List<? extends MdAstPhrasingContent> children) {
        MdxJsxTextElement element = new MdxJsxTextElement();
        element.setName(name);
        List rawChildren = element.children();
        for (MdAstPhrasingContent child : children) {
            rawChildren.add(child);
        }
        return element;
    }

    /**
     * Adds an {@link MdAstNode} to a flow element's children list via raw-type
     * access, bypassing the generic type check. This is needed when the child
     * is phrasing content (text, inline elements) that are semantically valid
     * inside the element (e.g. a {@link MdAstText} inside a {@code 
     * 
     * 
    
    <pre>
     * } tag).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addChildRaw(MdxJsxFlowElement element, MdAstNode node) {
        ((List) element.children()).add(node);
    }

    /**
     * Adds an {@link MdAstNode} to a text element's children list via raw-type
     * access, bypassing the generic type check. This is needed when the child
     * is a non-phrasing node that is semantically valid inline (e.g. a
     * {@link MdAstText} inside {@code <code>}).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addChildRaw(MdxJsxTextElement element, MdAstNode node) {
        ((List) element.children()).add(node);
    }

    /**
     * Serializes the GfmTable align list to a comma-separated lowercase string,
     * e.g. {@code "left,center,right"}. Returns {@code null} when the list is
     * null or empty.
     */
    @Nullable
    private static String serializeAlign(@Nullable List<Align> aligns) {
        if (aligns == null || aligns.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aligns.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            switch (aligns.get(i)) {
                case LEFT -> sb.append("left");
                case CENTER -> sb.append("center");
                case RIGHT -> sb.append("right");
                case NONE -> sb.append("none");
            }
        }
        return sb.toString();
    }

    /**
     * Extracts the expression from a kramdown-style attribute paragraph
     * ({@code {: ...}}), or returns null if not a kramdown meta line.
     */
    @Nullable
    private static String extractKramdownMeta(MdAstParagraph p) {
        if (p.children()
            .size() != 1) return null;
        if (!(p.children()
            .get(0) instanceof MdAstText t)) return null;
        String v = t.value.trim();
        if (v.startsWith("{:") && v.endsWith("}")) return v;
        return null;
    }
}
