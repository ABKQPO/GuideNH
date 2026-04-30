package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class GuideSiteLocalServer {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final int DEFAULT_PORT = 8734;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_STATE_FILE = ".guidenh-site-server.state";
    private static final String CONTROL_STATUS_PATH = "/__guidenh_control__/status";
    private static final String CONTROL_STOP_PATH = "/__guidenh_control__/stop";
    private static final int CONTROL_TIMEOUT_MILLIS = 1500;
    private static final Map<String, String> MIME_TYPES = createMimeTypes();

    private final Path rootDir;
    private final int port;
    private final String host;
    private final Path stateFile;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private volatile HttpServer server;
    private volatile boolean stopping;
    private String controlToken;

    private GuideSiteLocalServer(Path rootDir, int port, String host, Path stateFile) {
        this.rootDir = rootDir;
        this.port = port;
        this.host = host;
        this.stateFile = stateFile;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static int execute(String[] args) throws Exception {
        String command = args.length > 0 ? trimToNull(args[0]) : null;
        if ("serve".equalsIgnoreCase(command)) {
            return serve(args, 1);
        }
        if ("status".equalsIgnoreCase(command)) {
            return status(args, 1);
        }
        if ("stop".equalsIgnoreCase(command)) {
            return stop(args, 1);
        }
        return serve(args, 0);
    }

    private static int serve(String[] args, int offset) throws Exception {
        Path rootDir = args.length > offset ? Paths.get(args[offset]) : Paths.get(".");
        rootDir = rootDir.toAbsolutePath()
            .normalize();
        int port = args.length > offset + 1 ? parsePort(args[offset + 1]) : DEFAULT_PORT;
        String host = args.length > offset + 2 ? trimToNull(args[offset + 2]) : null;
        if (host == null) {
            host = DEFAULT_HOST;
        }
        Path stateFile = args.length > offset + 3 ? Paths.get(args[offset + 3]) : rootDir.resolve(DEFAULT_STATE_FILE);
        stateFile = stateFile.toAbsolutePath()
            .normalize();
        new GuideSiteLocalServer(rootDir, port, host, stateFile).run();
        return 0;
    }

    private static int status(String[] args, int offset) throws Exception {
        Path stateFile = resolveStateFileArgument(args, offset);
        Map<String, String> state = readStateFile(stateFile);
        if (state.isEmpty()) {
            return 1;
        }

        if (isControlEndpointReachable(state)) {
            System.out.println("GuideNH site server is running at " + buildIndexUrl(state));
            return 0;
        }

        Long pid = resolveStatePid(state);
        if (pid != null && isProcessAlive(pid)) {
            System.out.println("GuideNH site server process is still alive (pid=" + pid + ").");
            return 0;
        }

        return 1;
    }

    private static int stop(String[] args, int offset) throws Exception {
        Path stateFile = resolveStateFileArgument(args, offset);
        if (!Files.exists(stateFile)) {
            System.out.println("GuideNH static site server is not running.");
            return 0;
        }

        Map<String, String> state = readStateFile(stateFile);
        if (state.isEmpty()) {
            Files.deleteIfExists(stateFile);
            System.out.println("Removed empty state file.");
            return 0;
        }

        if (sendControlRequest(state, CONTROL_STOP_PATH)) {
            waitForStateFileDeletion(stateFile);
            System.out.println("GuideNH static site server stopped.");
            return 0;
        }

        Long pid = resolveStatePid(state);
        if (pid != null && stopProcess(pid)) {
            Files.deleteIfExists(stateFile);
            System.out.println("GuideNH static site server stopped.");
            return 0;
        }

        Files.deleteIfExists(stateFile);
        System.out.println("Removed stale state file.");
        return 0;
    }

    private void run() throws Exception {
        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("GuideNH site root does not exist: " + rootDir);
        }

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        httpServer.createContext(CONTROL_STATUS_PATH, this::handleStatusExchange);
        httpServer.createContext(CONTROL_STOP_PATH, this::handleStopExchange);
        httpServer.createContext("/", this::handleSiteExchange);
        httpServer.setExecutor(null);

        server = httpServer;
        controlToken = UUID.randomUUID()
            .toString();

        Runtime.getRuntime()
            .addShutdownHook(new Thread(this::shutdownFromHook, "GuideNH-SiteServer-Shutdown"));

        httpServer.start();
        writeStateFile();

        System.out.println("GuideNH site server started at http://" + host + ":" + port + "/index.html");
        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }
    }

    private void handleSiteExchange(HttpExchange exchange) throws IOException {
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

    private void handleStatusExchange(HttpExchange exchange) throws IOException {
        try {
            if (!isAuthorized(exchange)) {
                sendText(exchange, 403, "Forbidden");
                return;
            }
            sendText(exchange, 200, "OK");
        } finally {
            exchange.close();
        }
    }

    private void handleStopExchange(HttpExchange exchange) throws IOException {
        try {
            if (!isAuthorized(exchange)) {
                sendText(exchange, 403, "Forbidden");
                return;
            }
            sendText(exchange, 200, "Stopping");
            new Thread(this::stopServer, "GuideNH-SiteServer-Stop").start();
        } finally {
            exchange.close();
        }
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String token = parseQuery(
            exchange.getRequestURI()
                .getRawQuery()).get("token");
        return token != null && token.equals(controlToken);
    }

    private Path resolveTarget(HttpExchange exchange) {
        String rawPath = exchange.getRequestURI()
            .getPath();
        if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
            return rootDir.resolve("index.html");
        }

        String relativePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        relativePath = urlDecode(relativePath);
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

    private void writeStateFile() throws IOException {
        Path parent = stateFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>();
        lines.add("pid=" + resolveProcessId());
        lines.add("host=" + host);
        lines.add("port=" + port);
        lines.add("token=" + controlToken);
        lines.add("url=http://" + host + ":" + port + "/index.html");
        lines.add("rootDir=" + rootDir);
        Files.write(
            stateFile,
            lines,
            UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
    }

    private void shutdownFromHook() {
        deleteStateFileQuietly();
        stopLatch.countDown();
    }

    private void stopServer() {
        if (stopping) {
            return;
        }
        stopping = true;
        try {
            HttpServer httpServer = server;
            if (httpServer != null) {
                httpServer.stop(0);
            }
        } finally {
            deleteStateFileQuietly();
            stopLatch.countDown();
        }
    }

    private void deleteStateFileQuietly() {
        try {
            Files.deleteIfExists(stateFile);
        } catch (IOException ignored) {}
    }

    private static Path resolveStateFileArgument(String[] args, int offset) {
        Path stateFile = args.length > offset ? Paths.get(args[offset]) : Paths.get(DEFAULT_STATE_FILE);
        return stateFile.toAbsolutePath()
            .normalize();
    }

    private static Map<String, String> readStateFile(Path stateFile) throws IOException {
        if (!Files.exists(stateFile) || !Files.isRegularFile(stateFile)) {
            return Collections.emptyMap();
        }

        List<String> lines = Files.readAllLines(stateFile, UTF_8);
        Map<String, String> state = new LinkedHashMap<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals > 0) {
                state.put(trimmed.substring(0, equals), trimmed.substring(equals + 1));
            } else if (trimmed.chars()
                .allMatch(Character::isDigit)) {
                    state.put("legacyPid", trimmed);
                }
        }
        return state;
    }

    private static Long resolveStatePid(Map<String, String> state) {
        Long pid = parseLong(state.get("pid"));
        return pid != null ? pid : parseLong(state.get("legacyPid"));
    }

    private static Long parseLong(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isControlEndpointReachable(Map<String, String> state) {
        return sendControlRequest(state, CONTROL_STATUS_PATH);
    }

    private static boolean sendControlRequest(Map<String, String> state, String path) {
        String host = trimToNull(state.get("host"));
        String portValue = trimToNull(state.get("port"));
        String token = trimToNull(state.get("token"));
        if (host == null || portValue == null || token == null) {
            return false;
        }

        int port;
        try {
            port = parsePort(portValue);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        try {
            String encodedToken = urlEncode(token);
            URL url = new URL("http", host, port, path + "?token=" + encodedToken);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONTROL_TIMEOUT_MILLIS);
            connection.setReadTimeout(CONTROL_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode >= 200 && responseCode < 300;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String buildIndexUrl(Map<String, String> state) {
        String url = trimToNull(state.get("url"));
        if (url != null) {
            return url;
        }
        String host = trimToNull(state.get("host"));
        String port = trimToNull(state.get("port"));
        if (host == null || port == null) {
            return "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/index.html";
        }
        return "http://" + host + ":" + port + "/index.html";
    }

    private static String resolveProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean()
            .getName();
        int separator = runtimeName.indexOf('@');
        return separator > 0 ? runtimeName.substring(0, separator) : runtimeName;
    }

    private static boolean isProcessAlive(long pid) {
        if (pid <= 0L) {
            return false;
        }

        if (isWindows()) {
            String output = runCommandAndCapture("cmd", "/c", "tasklist /FI \"PID eq " + pid + "\" /FO LIST /NH");
            return output.contains("PID: " + pid);
        }
        return runCommand("sh", "-c", "kill -0 " + pid + " >/dev/null 2>&1");
    }

    private static boolean stopProcess(long pid) {
        if (!isProcessAlive(pid)) {
            return false;
        }

        if (isWindows()) {
            runCommand("cmd", "/c", "taskkill /PID " + pid + " /T /F >nul 2>nul");
            return !isProcessAlive(pid);
        }

        runCommand("sh", "-c", "kill " + pid + " >/dev/null 2>&1 || true");
        if (!waitForProcessExit(pid, 2L)) {
            runCommand("sh", "-c", "kill -9 " + pid + " >/dev/null 2>&1 || true");
            waitForProcessExit(pid, 2L);
        }
        return !isProcessAlive(pid);
    }

    private static boolean waitForProcessExit(long pid, long timeoutSeconds) {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadlineNanos) {
            if (!isProcessAlive(pid)) {
                return true;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                return !isProcessAlive(pid);
            }
        }
        return !isProcessAlive(pid);
    }

    private static void waitForStateFileDeletion(Path stateFile) {
        for (int i = 0; i < 50; i++) {
            if (!Files.exists(stateFile)) {
                return;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                return;
            }
        }
    }

    private static boolean runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            consume(process.getInputStream());
            consume(process.getErrorStream());
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String runCommandAndCapture(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            String stdout = readFully(process.getInputStream());
            consume(process.getErrorStream());
            process.waitFor();
            return stdout;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void consume(InputStream inputStream) {
        try {
            readFully(inputStream);
        } catch (IOException ignored) {}
    }

    private static String readFully(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), UTF_8);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("win");
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> query = new LinkedHashMap<>();
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String rawKey = equals >= 0 ? pair.substring(0, equals) : pair;
            String rawValue = equals >= 0 ? pair.substring(equals + 1) : "";
            query.put(urlDecode(rawKey), urlDecode(rawValue));
        }
        return query;
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, UTF_8.name());
        } catch (IOException e) {
            throw new IllegalStateException("UTF-8 is not available", e);
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8.name());
        } catch (IOException e) {
            throw new IllegalStateException("UTF-8 is not available", e);
        }
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
