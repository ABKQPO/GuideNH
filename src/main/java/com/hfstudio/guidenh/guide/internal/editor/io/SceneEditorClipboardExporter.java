package com.hfstudio.guidenh.guide.internal.editor.io;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.internal.GuidebookText;

public class SceneEditorClipboardExporter {

    public static final Logger LOG = LogManager.getLogger("guidenh");

    @FunctionalInterface
    public interface ClipboardSink {

        void copy(String text) throws Exception;
    }

    @FunctionalInterface
    public interface LogSink {

        void log(String text);
    }

    @FunctionalInterface
    public interface ChatSink {

        void send(@Nullable EntityPlayer player, GuidebookText key, Object... args);
    }

    private final ClipboardSink clipboardSink;
    private final LogSink logSink;
    private final ChatSink chatSink;

    public SceneEditorClipboardExporter() {
        this(
            text -> Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null),
            text -> LOG.info("Scene editor export:\n{}", text),
            (player, key, args) -> {
                if (player != null) {
                    player.addChatMessage(new ChatComponentTranslation(key.getTranslationKey(), args));
                }
            });
    }

    public SceneEditorClipboardExporter(ClipboardSink clipboardSink, LogSink logSink, ChatSink chatSink) {
        this.clipboardSink = clipboardSink;
        this.logSink = logSink;
        this.chatSink = chatSink;
    }

    public void export(@Nullable EntityPlayer player, String text) throws Exception {
        clipboardSink.copy(text);
        logSink.log(text);
        chatSink.send(player, GuidebookText.SceneEditorSaveSuccess);
    }

    public void notifyFailure(@Nullable EntityPlayer player, Throwable throwable) {
        LOG.error("Failed to save scene snippet", throwable);
        chatSink.send(player, GuidebookText.SceneEditorSaveFailure, getErrorMessage(throwable));
    }

    private String getErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null && !message.isEmpty() ? message
            : throwable.getClass()
                .getSimpleName();
    }
}
