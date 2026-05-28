package com.hfstudio.guidenh.bridge.transport;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.bridge.preview.PreviewQueryFactory;
import com.hfstudio.guidenh.bridge.preview.PreviewResolveQuery;
import com.hfstudio.guidenh.bridge.preview.PreviewResolveResult;
import com.hfstudio.guidenh.bridge.preview.PreviewSearchQuery;
import com.hfstudio.guidenh.bridge.preview.PreviewSearchResult;
import com.hfstudio.guidenh.bridge.preview.RuntimePreviewFacade;
import com.hfstudio.guidenh.bridge.protocol.BridgeEnvelope;
import com.hfstudio.guidenh.bridge.protocol.BridgeMessageCodec;
import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.bridge.protocol.BridgeResponseFactory;
import com.hfstudio.guidenh.bridge.security.BridgeTokenAuthenticator;
import com.hfstudio.guidenh.bridge.semantic.SemanticProviderRegistry;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryFactory;

public class RuntimeBridgeConnection implements Runnable {

    private static final int SOCKET_TIMEOUT_MILLIS = 30000;

    private final Socket socket;
    private final BridgeMessageCodec messageCodec;
    private final WebSocketFrameCodec frameCodec;
    private final BridgeTokenAuthenticator authenticator;
    private final SemanticProviderRegistry registry;
    private final RuntimePreviewFacade previewFacade;
    private final BridgeProtocolLimits limits;
    private final Consumer<RuntimeBridgeConnection> closeCallback;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final BridgeResponseFactory responseFactory = new BridgeResponseFactory();
    private final SemanticQueryFactory queryFactory;
    private final PreviewQueryFactory previewQueryFactory;
    private boolean authenticated;

    public RuntimeBridgeConnection(Socket socket, BridgeMessageCodec messageCodec,
        BridgeTokenAuthenticator authenticator, SemanticProviderRegistry registry, RuntimePreviewFacade previewFacade,
        BridgeProtocolLimits limits, Consumer<RuntimeBridgeConnection> closeCallback) {
        this.socket = socket;
        this.messageCodec = messageCodec;
        this.frameCodec = new WebSocketFrameCodec(limits.getMaxMessageBytes());
        this.authenticator = authenticator;
        this.registry = registry;
        this.previewFacade = previewFacade;
        this.limits = limits;
        this.closeCallback = closeCallback;
        this.queryFactory = new SemanticQueryFactory(limits);
        this.previewQueryFactory = new PreviewQueryFactory(limits);
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
            GuideNH.LOG.info("GuideNH runtime bridge waiting for WebSocket handshake from {}", describeRemote());
            if (!new WebSocketHandshake().accept(socket.getInputStream(), socket.getOutputStream())) {
                GuideNH.LOG
                    .warn("GuideNH runtime bridge rejected invalid WebSocket handshake from {}", describeRemote());
                return;
            }
            GuideNH.LOG.info("GuideNH runtime bridge WebSocket handshake completed for {}", describeRemote());
            readFrames();
        } catch (SocketTimeoutException ignored) {
            GuideNH.LOG.warn("GuideNH runtime bridge connection timed out for {}", describeRemote());
            closeQuietly();
        } catch (IOException e) {
            GuideNH.LOG.warn("GuideNH runtime bridge connection I/O failed for {}", describeRemote(), e);
            closeQuietly();
        } finally {
            close();
        }
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        GuideNH.LOG.info("GuideNH runtime bridge closing connection for {}", describeRemote());
        closeQuietly();
        closeCallback.accept(this);
    }

    public String getRemoteAddress() {
        return describeRemote();
    }

    private void readFrames() throws IOException {
        while (!closed.get()) {
            WebSocketFrame frame = frameCodec.read(socket.getInputStream());
            if (frame.isClose()) {
                frameCodec.writeClose(socket.getOutputStream());
                return;
            }
            if (frame.isPing()) {
                frameCodec.writePong(socket.getOutputStream(), frame.getPayload());
                continue;
            }
            if (frame.isText()) {
                handleText(new String(frame.getPayload(), StandardCharsets.UTF_8));
            }
        }
    }

    private void handleText(String text) throws IOException {
        BridgeEnvelope envelope;
        try {
            envelope = messageCodec.decode(text);
            BridgeEnvelope response = dispatch(envelope);
            if (response != null) {
                frameCodec.writeText(socket.getOutputStream(), messageCodec.encode(response));
            }
        } catch (IllegalArgumentException e) {
            BridgeEnvelope error = responseFactory.error(null, "unknown", "invalid_request", e.getMessage(), false);
            frameCodec.writeText(socket.getOutputStream(), messageCodec.encode(error));
        }
    }

    private BridgeEnvelope dispatch(BridgeEnvelope envelope) {
        if ("hello".equals(envelope.getMethod())) {
            return handleHello(envelope);
        }
        if (!authenticated) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "unauthorized", "Bridge token is required", false);
        }
        if ("semantic.query".equals(envelope.getMethod())) {
            return handleSemanticQuery(envelope);
        }
        if ("document.validate".equals(envelope.getMethod())) {
            return handleDocumentValidate(envelope);
        }
        if ("preview.search".equals(envelope.getMethod())) {
            return handlePreviewSearch(envelope);
        }
        if ("preview.resolve".equals(envelope.getMethod())) {
            return handlePreviewResolve(envelope);
        }
        if ("capabilities".equals(envelope.getMethod())) {
            return responseFactory.capabilities(envelope.getId(), registry.getCapabilities());
        }
        return responseFactory
            .error(envelope.getId(), envelope.getMethod(), "unknown_method", "Unknown bridge method", false);
    }

    private BridgeEnvelope handleHello(BridgeEnvelope envelope) {
        String token = envelope.getPayload() != null && envelope.getPayload()
            .has("token") ? envelope.getPayload()
                .get("token")
                .getAsString() : "";
        if (!authenticator.matches(token)) {
            GuideNH.LOG.warn("GuideNH runtime bridge authentication failed for {}", describeRemote());
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "unauthorized", "Invalid bridge token", false);
        }
        authenticated = true;
        GuideNH.LOG.info("GuideNH runtime bridge authenticated {}", describeRemote());
        return responseFactory.hello(envelope.getId(), limits);
    }

    private BridgeEnvelope handleSemanticQuery(BridgeEnvelope envelope) {
        String capability = queryFactory.readCapability(envelope.getPayload());
        if (capability.isEmpty()) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "invalid_capability", "Capability is required", false);
        }
        try {
            SemanticQuery query = queryFactory.fromPayload(envelope.getPayload());
            return responseFactory
                .semanticResult(envelope.getId(), envelope.getMethod(), registry.query(capability, query));
        } catch (IllegalArgumentException e) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "invalid_capability", e.getMessage(), false);
        }
    }

    private BridgeEnvelope handleDocumentValidate(BridgeEnvelope envelope) {
        return responseFactory.documentValidate(envelope.getId(), envelope.getMethod());
    }

    private BridgeEnvelope handlePreviewSearch(BridgeEnvelope envelope) {
        String capability = previewQueryFactory.readCapability(envelope.getPayload());
        if (capability.isEmpty()) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "invalid_capability", "Capability is required", false);
        }
        try {
            PreviewSearchQuery query = previewQueryFactory.createSearchQuery(envelope.getPayload());
            PreviewSearchResult result = previewFacade.search(query);
            return responseFactory.previewSearch(envelope.getId(), result);
        } catch (IllegalArgumentException error) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "invalid_preview_query", error.getMessage(), false);
        }
    }

    private BridgeEnvelope handlePreviewResolve(BridgeEnvelope envelope) {
        String capability = previewQueryFactory.readCapability(envelope.getPayload());
        if (capability.isEmpty()) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "invalid_capability", "Capability is required", false);
        }
        try {
            PreviewResolveQuery query = previewQueryFactory.createResolveQuery(envelope.getPayload());
            PreviewResolveResult result = previewFacade.resolve(query);
            responseFactory.validatePreviewResultSize(result, limits);
            return responseFactory.previewResolve(envelope.getId(), result);
        } catch (IllegalArgumentException error) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "invalid_preview_query", error.getMessage(), false);
        } catch (IllegalStateException error) {
            return responseFactory
                .error(envelope.getId(), envelope.getMethod(), "preview_render_failed", error.getMessage(), true);
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    private String describeRemote() {
        if (socket.getRemoteSocketAddress() == null) {
            return "unknown";
        }
        return String.valueOf(socket.getRemoteSocketAddress());
    }
}
