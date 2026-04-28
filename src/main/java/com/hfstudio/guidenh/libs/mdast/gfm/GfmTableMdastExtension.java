package com.hfstudio.guidenh.libs.mdast.gfm;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hfstudio.guidenh.libs.mdast.MdastContext;
import com.hfstudio.guidenh.libs.mdast.MdastContextProperty;
import com.hfstudio.guidenh.libs.mdast.MdastExtension;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableCell;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.micromark.Token;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.GfmTableSyntax;

public class GfmTableMdastExtension {

    public static final MdastContextProperty<Boolean> IN_TABLE = new MdastContextProperty<>();

    public static final MdastExtension INSTANCE = MdastExtension.builder()
        .enter("table", GfmTableMdastExtension::enterTable)
        .enter("tableData", GfmTableMdastExtension::enterCell)
        .enter("tableHeader", GfmTableMdastExtension::enterCell)
        .enter("tableRow", GfmTableMdastExtension::enterRow)
        .exit("codeText", GfmTableMdastExtension::exitCodeText)
        .exit("table", GfmTableMdastExtension::exitTable)
        .exit("tableData", GfmTableMdastExtension::exit)
        .exit("tableHeader", GfmTableMdastExtension::exit)
        .exit("tableRow", GfmTableMdastExtension::exit)
        .build();

    private GfmTableMdastExtension() {}

    public static void enterTable(MdastContext context, Token token) {
        var align = token.get(GfmTableSyntax.ALIGN);

        var table = new GfmTable();
        table.align = align;

        context.enter(table, token);
        context.set(IN_TABLE, true);
    }

    public static void exitTable(MdastContext context, Token token) {
        context.exit(token);
        context.remove(IN_TABLE);
    }

    public static void enterRow(MdastContext context, Token token) {
        context.enter(new GfmTableRow(), token);
    }

    public static void exit(MdastContext context, Token token) {
        context.exit(token);
    }

    public static void enterCell(MdastContext context, Token token) {
        context.enter(new GfmTableCell(), token);
    }

    public static final Pattern ESCAPED_PIPE_PATERN = Pattern.compile("\\\\([\\\\|])");

    // Overwrite the default code text data handler to unescape escaped pipes when
    // they are in tables.
    public static void exitCodeText(MdastContext context, Token token) {
        var value = context.resume();

        if (Boolean.TRUE.equals(context.get(IN_TABLE))) {
            Matcher m = ESCAPED_PIPE_PATERN.matcher(value);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(replace(m.toMatchResult())));
            }
            m.appendTail(sb);
            value = sb.toString();
        }

        var stack = context.getStack();
        var node = (MdAstInlineCode) stack.get(stack.size() - 1);
        node.value = value;
        context.exit(token);
    }

    public static String replace(MatchResult result) {
        // Pipes work, backslashes don’t (but can’t escape pipes).
        return result.group(1)
            .equals("|") ? "|" : result.group();
    }

}
