package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class GuideSiteLocalServer {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final int DEFAULT_PORT = 8734;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final Map<String, String> MIME_TYPES = createMimeTypes();

    private final Path rootDir;
    private final int port;
    private final String host;
    private final Path pidFile;

    private GuideSiteLocalServer(Path rootDir, int port, String host, Path pidFile) {
        this.rootDir = rootDir;
        this.port = port;
        this.host = host;
        this.pidFile = pidFile;
    }

    public static void main(String[] args) throws Exception {
        Path rootDir = args.length >= 1 ? Paths.get(args[0]) : Paths.get(".");
        rootDir = rootDir.toAbsolutePath()
            .normalize();
        int port = args.length >= 2 ? parsePort(args[1]) : DEFAULT_PORT;
        String host = args.length >= 3 ? trimToNull(args[2]) : null;
        if (host == null) {
            host = DEFAULT_HOST;
        }
        Path pidFile = args.length >= 4 ? Paths.get(args[3]) : rootDir.resolve(".guidenh-site-server.pid");
        pidFile = pidFile.toAbsolutePath()
            .normalize();
        new GuideSiteLocalServer(rootDir, port, host, pidFile).run();
    }

    private void run() throws Exception {
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("GuideNH site root does not exist: " + rootDir);
        }

        writePidFile();

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", exchange -> handleExchange(exchange));
        server.setExecutor(null);
        Runtime.getRuntime()
            .addShutdownHook(new Thread(this::deletePidFileQuietly, "GuideNH-SiteServer-Shutdown"));
        server.start();

        System.out.println("GuideNH site server started at http://" + host + ":" + port + "/index.html");
        Object waitLock = new Object();
        synchronized (waitLock) {
            while (true) {
                waitLock.wait();
            }
        }
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders()
                    .set("Allow", "GET, HEAD");
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            Path target = resolveTarget(exchange);
            if (target == null) {
                sendText(exchange, 403, "Forbidden");
                return;
            }
            if (!Files.exists(target) || !Files.isRegularFile(target)) {
                sendText(exchange, 404, "Not Found");
                return;
            }

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", guessContentType(target));
            headers.set("Cache-Control", "no-cache");
            headers.set("X-Content-Type-Options", "nosniff");

            long size = Files.size(target);
            if ("HEAD".equalsIgnoreCase(method)) {
                headers.set("Content-Length", Long.toString(size));
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            exchange.sendResponseHeaders(200, size);
            try (OutputStream body = exchange.getResponseBody()) {
                Files.copy(target, body);
            }
        } finally {
            exchange.close();
        }
    }

    private Path resolveTarget(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI()
            .getPath();
        if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
            return rootDir.resolve("index.html");
        }

        String relativePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        relativePath = URLDecoder.decode(relativePath, UTF_8.name());
        Path candidate = rootDir.resolve(relativePath)
            .normalize();
        if (!candidate.startsWith(rootDir)) {
            return null;
        }
        if (Files.isDirectory(candidate)) {
            candidate = candidate.resolve("index.html")
                .normalize();
            if (!candidate.startsWith(rootDir)) {
                return null;
            }
        }
        return candidate;
    }

    private void sendText(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(UTF_8);
        exchange.getResponseHeaders()
            .set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private void writePidFile() throws IOException {
        Path parent = pidFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(
            pidFile,
            Collections.singleton(resolveProcessId()),
            UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
    }

    private void deletePidFileQuietly() {
        try {
            Files.deleteIfExists(pidFile);
        } catch (IOException ignored) {}
    }

    private static String resolveProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean()
            .getName();
        int separator = runtimeName.indexOf('@');
        return separator > 0 ? runtimeName.substring(0, separator) : runtimeName;
    }

    private static int parsePort(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 65535) {
                throw new IllegalArgumentException("Port must be within 1-65535: " + value);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + value, e);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String guessContentType(Path target) {
        String fileName = target.getFileName()
            .toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(dot + 1)
            .toLowerCase(Locale.ROOT);
        String contentType = MIME_TYPES.get(extension);
        return contentType != null ? contentType : "application/octet-stream";
    }

    private static Map<String, String> createMimeTypes() {
        Map<String, String> mimeTypes = new LinkedHashMap<>();
        mimeTypes.put("html", "text/html; charset=UTF-8");
        mimeTypes.put("css", "text/css; charset=UTF-8");
        mimeTypes.put("js", "text/javascript; charset=UTF-8");
        mimeTypes.put("json", "application/json; charset=UTF-8");
        mimeTypes.put("map", "application/json; charset=UTF-8");
        mimeTypes.put("txt", "text/plain; charset=UTF-8");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("ico", "image/x-icon");
        mimeTypes.put("webp", "image/webp");
        mimeTypes.put("wasm", "application/wasm");
        mimeTypes.put("gz", "application/gzip");
        mimeTypes.put("bin", "application/octet-stream");
        return mimeTypes;
    }
}
