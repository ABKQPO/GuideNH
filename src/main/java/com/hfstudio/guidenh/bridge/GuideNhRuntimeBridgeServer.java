package com.hfstudio.guidenh.bridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.bridge.protocol.BridgeMessageCodec;
import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;
import com.hfstudio.guidenh.bridge.security.BridgeTokenAuthenticator;
import com.hfstudio.guidenh.bridge.semantic.SemanticProviderRegistry;
import com.hfstudio.guidenh.bridge.semantic.providers.RuntimeSemanticProviders;
import com.hfstudio.guidenh.bridge.transport.RuntimeBridgeConnection;

public class GuideNhRuntimeBridgeServer {

    private final GuideNhRuntimeBridgeSettings settings;
    private final BridgeProtocolLimits limits;
    private final BridgeMessageCodec messageCodec;
    private final BridgeTokenAuthenticator authenticator;
    private final SemanticProviderRegistry registry = new SemanticProviderRegistry();
    private final Set<RuntimeBridgeConnection> connections = Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService executor = Executors.newCachedThreadPool(new RuntimeBridgeThreadFactory());
    private final AtomicBoolean running = new AtomicBoolean();
    private ServerSocket serverSocket;

    public GuideNhRuntimeBridgeServer(GuideNhRuntimeBridgeSettings settings) {
        this.settings = settings;
        this.limits = new BridgeProtocolLimits(
            settings.getMaxMessageBytes(),
            settings.getMaxPageSize(),
            settings.getMaxSubscriptions(),
            settings.getMaxConnections(),
            settings.getMaxDeltaEntries());
        this.messageCodec = new BridgeMessageCodec(limits);
        this.authenticator = new BridgeTokenAuthenticator(settings.getToken());
        RuntimeSemanticProviders.registerBaseline(registry);
    }

    public void start() {
        if (!settings.canStart() || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(settings.getHost(), settings.getPort()));
            executor.execute(this::acceptConnections);
            GuideNH.LOG.info("GuideNH runtime bridge started at ws://{}:{}", settings.getHost(), settings.getPort());
        } catch (IOException e) {
            running.set(false);
            closeServerSocket();
            GuideNH.LOG.warn("Failed to start GuideNH runtime bridge", e);
        }
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        closeServerSocket();
        List<RuntimeBridgeConnection> snapshot;
        synchronized (connections) {
            snapshot = new ArrayList<>(connections);
            connections.clear();
        }
        for (RuntimeBridgeConnection connection : snapshot) {
            connection.close();
        }
        executor.shutdownNow();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                if (connections.size() >= limits.getMaxConnections()) {
                    socket.close();
                    continue;
                }
                RuntimeBridgeConnection connection = new RuntimeBridgeConnection(
                    socket,
                    messageCodec,
                    authenticator,
                    registry,
                    limits,
                    connections::remove);
                connections.add(connection);
                executor.execute(connection);
            } catch (IOException e) {
                if (running.get()) {
                    GuideNH.LOG.warn("GuideNH runtime bridge accept loop failed", e);
                }
            }
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
    }

    public static class RuntimeBridgeThreadFactory implements ThreadFactory {

        private int nextThreadId;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "GuideNH-RuntimeBridge-" + nextThreadId++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
