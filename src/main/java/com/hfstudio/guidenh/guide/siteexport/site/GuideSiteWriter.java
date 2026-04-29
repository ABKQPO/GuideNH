package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class GuideSiteWriter {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();

    public void writeBootstrapFiles(Path outDir) throws Exception {
        writeResource(outDir.resolve("_site/app.css"), "/assets/guidenh/siteexport/app.css");
        writeResource(outDir.resolve("_site/app.js"), "/assets/guidenh/siteexport/app.js");
        writeResource(outDir.resolve("_site/search.js"), "/assets/guidenh/siteexport/search.js");
        writeResource(outDir.resolve("_site/decompress.js"), "/assets/guidenh/siteexport/decompress.js");
        writeResource(
            outDir.resolve("_site/decompressFallback.js"),
            "/assets/guidenh/siteexport/decompressFallback.js");
        writeResource(outDir.resolve("_site/model-viewer.css"), "/assets/guidenh/siteexport/model-viewer.css");
        writeResource(outDir.resolve("_site/viewer.js"), "/assets/guidenh/siteexport/viewer.js");
        writeResource(
            outDir.resolve("_site/model-viewer/modelViewer.js"),
            "/assets/guidenh/siteexport/model-viewer/modelViewer.js");
        writeResource(
            outDir.resolve("_site/model-viewer/vendor/modelViewer-A42QTX7N.js"),
            "/assets/guidenh/siteexport/model-viewer/vendor/modelViewer-A42QTX7N.js");
        writeResource(
            outDir.resolve("_site/model-viewer/vendor/chunk-ZM3PKBJN.js"),
            "/assets/guidenh/siteexport/model-viewer/vendor/chunk-ZM3PKBJN.js");
        writeResource(
            outDir.resolve("_site/model-viewer/vendor/decompressFallback-VGYIC7XH.js"),
            "/assets/guidenh/siteexport/model-viewer/vendor/decompressFallback-VGYIC7XH.js");
        writeResource(
            outDir.resolve("_site/model-viewer/vendor/diamond-7X2HHREI.png"),
            "/assets/guidenh/siteexport/model-viewer/vendor/diamond-7X2HHREI.png");
        writeResource(
            outDir.resolve("_site/model-viewer/vendor/diamond_colored-LGQLQFTB.png"),
            "/assets/guidenh/siteexport/model-viewer/vendor/diamond_colored-LGQLQFTB.png");
        GuideSiteLocalServerJarWriter.writeTo(outDir.resolve("_site/guidenh-site-server.jar"));
        writeStartScripts(outDir);
    }

    public void cleanupGeneratedOutputs(Path outDir) throws Exception {
        Path normalizedOutDir = outDir.toAbsolutePath()
            .normalize();
        deleteRecursively(normalizedOutDir.resolve("_site"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("_res"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("_data"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("guides"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("index.html"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("start.bat"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("start.sh"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("stop.bat"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("stop.sh"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve(".guidenh-site-server.pid"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve(".guidenh-site-server"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("export-report.json"), normalizedOutDir);
    }

    public void writePage(Path outDir, String namespace, String guidePath, String language, String pageRelativeFile,
        String sidebarHtml, String contentHtml, List<String> templateHtml, String title) throws Exception {
        Path pagePath = outDir.resolve(Paths.get("guides", namespace, guidePath, language))
            .resolve(pageRelativeFile);
        Files.createDirectories(pagePath.getParent());

        String layout = loadText("/assets/guidenh/siteexport/layout.html").replace("{{lang}}", escapeHtml(language))
            .replace("{{title}}", escapeHtml(title))
            .replace("{{sidebar}}", sidebarHtml)
            .replace("{{content}}", contentHtml + String.join("", templateHtml))
            .replace("{{root}}", relativeRoot(outDir, pagePath));
        Files.write(pagePath, layout.getBytes(StandardCharsets.UTF_8));
    }

    public void writeNavigationIndex(Path outDir, String namespace, String guidePath, String language, String json)
        throws Exception {
        Path path = outDir.resolve(Paths.get("_data", "nav", namespace, guidePath, language + ".json"));
        Files.createDirectories(path.getParent());
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }

    public void writeSearchIndex(Path outDir, String language, String json) throws Exception {
        Path path = outDir.resolve(Paths.get("_data", "search", language + ".json"));
        Files.createDirectories(path.getParent());
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }

    public void writeReport(Path outDir, String json) throws Exception {
        Files.write(outDir.resolve("export-report.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    public void writeLandingPage(Path outDir, @Nullable String firstPageUrl, String title) throws Exception {
        String html;
        if (firstPageUrl == null || firstPageUrl.isEmpty()) {
            html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + escapeHtml(title)
                + "</title></head><body><main><h1>"
                + escapeHtml(title)
                + "</h1><p>No guide pages were exported.</p></main></body></html>";
        } else {
            String escapedUrl = escapeHtml(firstPageUrl);
            html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + escapeHtml(title)
                + "</title><meta http-equiv=\"refresh\" content=\"0; url="
                + escapedUrl
                + "\"></head><body><p><a href=\""
                + escapedUrl
                + "\">Open exported guide</a></p></body></html>";
        }
        Files.write(outDir.resolve("index.html"), html.getBytes(StandardCharsets.UTF_8));
    }

    public String pageUrl(String namespace, String guidePath, String language, String pageRelativeFile) {
        return "guides/" + namespace + "/" + guidePath + "/" + language + "/" + pageRelativeFile.replace('\\', '/');
    }

    public String navigationJson(MutableGuide guide, String language, NavigationTree tree) {
        List<Map<String, Object>> rootNodes = new ArrayList<>();
        for (NavigationNode node : tree.getRootNodes()) {
            rootNodes.add(navigationNodeData(guide, language, node));
        }
        return GSON.toJson(rootNodes);
    }

    public String renderSidebar(MutableGuide guide, String language, NavigationTree tree,
        ResourceLocation currentPageId) {
        return renderSidebar(guide, language, tree, currentPageId, null, GuideSiteItemIconResolver.NONE);
    }

    public String renderSidebar(MutableGuide guide, String language, NavigationTree tree,
        ResourceLocation currentPageId, GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        return renderSidebar(guide, language, tree, currentPageId, assetExporter, itemIconResolver, null);
    }

    public String renderSidebar(MutableGuide guide, String language, NavigationTree tree,
        ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable List<GuideSiteLanguageLink> languageLinks) {
        SiteUiText uiText = SiteUiText.forLanguage(language);
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-sidebar-tools\">");
        appendLanguageSwitcher(html, language, languageLinks, uiText);
        html.append("<label class=\"guide-search\">");
        html.append("<span class=\"guide-search-label\">")
            .append(escapeHtml(uiText.searchLabel()))
            .append("</span>");
        html.append("<input type=\"search\" class=\"guide-search-input\" data-guide-search-input " + "placeholder=\"")
            .append(escapeHtml(uiText.searchPlaceholder()))
            .append("\" autocomplete=\"off\" spellcheck=\"false\">");
        html.append("</label>");
        html.append("<div class=\"guide-search-results\" data-guide-search-results data-guide-search-empty-template=\"")
            .append(escapeHtml(uiText.searchEmptyTemplate()))
            .append("\" hidden></div>");
        html.append("</div>");
        html.append("<nav class=\"guide-nav\"><ul>");
        for (NavigationNode node : tree.getRootNodes()) {
            appendNavigationNode(html, guide, language, node, currentPageId, assetExporter, itemIconResolver);
        }
        html.append("</ul></nav>");
        return html.toString();
    }

    private void writeStartScripts(Path outDir) throws Exception {
        Files.write(outDir.resolve("start.bat"), windowsStartScript().getBytes(StandardCharsets.UTF_8));
        Files.write(outDir.resolve("stop.bat"), windowsStopScript().getBytes(StandardCharsets.UTF_8));
        Path startSh = outDir.resolve("start.sh");
        Path stopSh = outDir.resolve("stop.sh");
        Files.write(startSh, unixStartScript().getBytes(StandardCharsets.UTF_8));
        Files.write(stopSh, unixStopScript().getBytes(StandardCharsets.UTF_8));
        trySetExecutable(startSh);
        trySetExecutable(stopSh);
    }

    private String windowsStartScript() {
        return "@echo off\r\n" + "setlocal\r\n"
            + "set \"PORT=8734\"\r\n"
            + "set \"SITE_DIR=%~dp0.\"\r\n"
            + "set \"SERVER_JAR=%SITE_DIR%\\_site\\guidenh-site-server.jar\"\r\n"
            + "set \"PID_FILE=%SITE_DIR%\\.guidenh-site-server.pid\"\r\n"
            + "set \"LOG_DIR=%SITE_DIR%\\.guidenh-site-server\"\r\n"
            + "set \"STDOUT_LOG=%LOG_DIR%\\stdout.log\"\r\n"
            + "set \"STDERR_LOG=%LOG_DIR%\\stderr.log\"\r\n"
            + "if not exist \"%SERVER_JAR%\" (\r\n"
            + "  echo Missing bundled server jar: \"%SERVER_JAR%\"\r\n"
            + "  exit /b 1\r\n"
            + ")\r\n"
            + "where java >nul 2>nul\r\n"
            + "if errorlevel 1 (\r\n"
            + "  echo Java runtime not found. Install Java and run this script again.\r\n"
            + "  exit /b 1\r\n"
            + ")\r\n"
            + "if exist \"%PID_FILE%\" (\r\n"
            + "  set \"SERVER_PID=\"\r\n"
            + "  for /f \"usebackq delims=\" %%P in (\"%PID_FILE%\") do if not defined SERVER_PID set \"SERVER_PID=%%P\"\r\n"
            + "  if defined SERVER_PID (\r\n"
            + "    powershell -NoProfile -Command \"if (Get-Process -Id $env:SERVER_PID -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }\"\r\n"
            + "    if not errorlevel 1 (\r\n"
            + "      start \"\" \"http://127.0.0.1:%PORT%/index.html\"\r\n"
            + "      exit /b 0\r\n"
            + "    )\r\n"
            + "  )\r\n"
            + "  del /f /q \"%PID_FILE%\" >nul 2>nul\r\n"
            + ")\r\n"
            + "if not exist \"%LOG_DIR%\" mkdir \"%LOG_DIR%\"\r\n"
            + "powershell -NoProfile -ExecutionPolicy Bypass -Command ^\r\n"
            + "  \"$jar = $env:SERVER_JAR; $dir = $env:SITE_DIR; $pid = $env:PID_FILE; $out = $env:STDOUT_LOG; $err = $env:STDERR_LOG; \" ^\r\n"
            + "  \"Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar, $dir, $env:PORT, '127.0.0.1', $pid) -WorkingDirectory $dir -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err | Out-Null\"\r\n"
            + "if errorlevel 1 (\r\n"
            + "  echo Failed to start bundled Java site server.\r\n"
            + "  exit /b 1\r\n"
            + ")\r\n"
            + "timeout /t 1 /nobreak >nul\r\n"
            + "start \"\" \"http://127.0.0.1:%PORT%/index.html\"\r\n";
    }

    private String windowsStopScript() {
        return "@echo off\r\n" + "setlocal\r\n"
            + "set \"SITE_DIR=%~dp0.\"\r\n"
            + "set \"PID_FILE=%SITE_DIR%\\.guidenh-site-server.pid\"\r\n"
            + "if not exist \"%PID_FILE%\" (\r\n"
            + "  echo GuideNH static site server is not running.\r\n"
            + "  exit /b 0\r\n"
            + ")\r\n"
            + "set \"SERVER_PID=\"\r\n"
            + "for /f \"usebackq delims=\" %%P in (\"%PID_FILE%\") do if not defined SERVER_PID set \"SERVER_PID=%%P\"\r\n"
            + "if not defined SERVER_PID (\r\n"
            + "  del /f /q \"%PID_FILE%\" >nul 2>nul\r\n"
            + "  echo Removed empty pid file.\r\n"
            + "  exit /b 0\r\n"
            + ")\r\n"
            + "powershell -NoProfile -Command \"if (Get-Process -Id $env:SERVER_PID -ErrorAction SilentlyContinue) { Stop-Process -Id $env:SERVER_PID -Force; exit 0 } else { exit 1 }\"\r\n"
            + "del /f /q \"%PID_FILE%\" >nul 2>nul\r\n"
            + "if errorlevel 1 (\r\n"
            + "  echo Removed stale pid file.\r\n"
            + ") else (\r\n"
            + "  echo GuideNH static site server stopped.\r\n"
            + ")\r\n";
    }

    private String unixStartScript() {
        return "#!/usr/bin/env sh\n" + "DIR=\"$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\"\n"
            + "PORT=8734\n"
            + "SERVER_JAR=\"$DIR/_site/guidenh-site-server.jar\"\n"
            + "PID_FILE=\"$DIR/.guidenh-site-server.pid\"\n"
            + "LOG_DIR=\"$DIR/.guidenh-site-server\"\n"
            + "STDOUT_LOG=\"$LOG_DIR/stdout.log\"\n"
            + "STDERR_LOG=\"$LOG_DIR/stderr.log\"\n"
            + "\n"
            + "open_browser() {\n"
            + "  URL=\"http://127.0.0.1:$PORT/index.html\"\n"
            + "  if command -v xdg-open >/dev/null 2>&1; then\n"
            + "    xdg-open \"$URL\" >/dev/null 2>&1\n"
            + "  elif command -v open >/dev/null 2>&1; then\n"
            + "    open \"$URL\" >/dev/null 2>&1\n"
            + "  fi\n"
            + "}\n"
            + "\n"
            + "if [ ! -f \"$SERVER_JAR\" ]; then\n"
            + "  echo \"Missing bundled server jar: $SERVER_JAR\"\n"
            + "  exit 1\n"
            + "fi\n"
            + "if ! command -v java >/dev/null 2>&1; then\n"
            + "  echo \"Java runtime not found. Install Java and run this script again.\"\n"
            + "  exit 1\n"
            + "fi\n"
            + "if [ -f \"$PID_FILE\" ]; then\n"
            + "  SERVER_PID=\"$(sed -n '1p' \"$PID_FILE\")\"\n"
            + "  if [ -n \"$SERVER_PID\" ] && kill -0 \"$SERVER_PID\" >/dev/null 2>&1; then\n"
            + "    open_browser\n"
            + "    exit 0\n"
            + "  fi\n"
            + "  rm -f \"$PID_FILE\"\n"
            + "fi\n"
            + "mkdir -p \"$LOG_DIR\"\n"
            + "if command -v nohup >/dev/null 2>&1; then\n"
            + "  (cd \"$DIR\" && nohup java -jar \"$SERVER_JAR\" \"$DIR\" \"$PORT\" \"127.0.0.1\" \"$PID_FILE\" >\"$STDOUT_LOG\" 2>\"$STDERR_LOG\" </dev/null &)\n"
            + "else\n"
            + "  (cd \"$DIR\" && java -jar \"$SERVER_JAR\" \"$DIR\" \"$PORT\" \"127.0.0.1\" \"$PID_FILE\" >\"$STDOUT_LOG\" 2>\"$STDERR_LOG\" </dev/null &)\n"
            + "fi\n"
            + "sleep 1\n"
            + "open_browser\n";
    }

    private String unixStopScript() {
        return "#!/usr/bin/env sh\n" + "DIR=\"$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\"\n"
            + "PID_FILE=\"$DIR/.guidenh-site-server.pid\"\n"
            + "if [ ! -f \"$PID_FILE\" ]; then\n"
            + "  echo \"GuideNH static site server is not running.\"\n"
            + "  exit 0\n"
            + "fi\n"
            + "SERVER_PID=\"$(sed -n '1p' \"$PID_FILE\")\"\n"
            + "if [ -z \"$SERVER_PID\" ]; then\n"
            + "  rm -f \"$PID_FILE\"\n"
            + "  echo \"Removed empty pid file.\"\n"
            + "  exit 0\n"
            + "fi\n"
            + "if kill -0 \"$SERVER_PID\" >/dev/null 2>&1; then\n"
            + "  kill \"$SERVER_PID\" >/dev/null 2>&1 || true\n"
            + "  sleep 1\n"
            + "  if kill -0 \"$SERVER_PID\" >/dev/null 2>&1; then\n"
            + "    kill -9 \"$SERVER_PID\" >/dev/null 2>&1 || true\n"
            + "  fi\n"
            + "  echo \"GuideNH static site server stopped.\"\n"
            + "else\n"
            + "  echo \"Removed stale pid file.\"\n"
            + "fi\n"
            + "rm -f \"$PID_FILE\"\n";
    }

    private void trySetExecutable(Path script) {
        try {
            Files.setPosixFilePermissions(
                script,
                EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX file systems such as Windows ignore executable bits.
        } catch (Exception ignored) {}
    }

    private void appendLanguageSwitcher(StringBuilder html, String currentLanguage,
        @Nullable List<GuideSiteLanguageLink> languageLinks, SiteUiText uiText) {
        if (languageLinks == null || languageLinks.size() < 2) {
            return;
        }

        html.append("<div class=\"guide-language-switcher\">");
        html.append("<span class=\"guide-search-label\">")
            .append(escapeHtml(uiText.languagesLabel()))
            .append("</span>");
        html.append("<div class=\"guide-language-links\">");
        String normalizedCurrentLanguage = normalizeLanguage(currentLanguage);
        for (GuideSiteLanguageLink link : languageLinks) {
            String normalizedLinkLanguage = normalizeLanguage(link.language());
            boolean current = normalizedCurrentLanguage.equals(normalizedLinkLanguage);
            html.append("<a class=\"guide-language-link");
            if (current) {
                html.append(" is-current");
            }
            if (link.fallbackUsed()) {
                html.append(" is-fallback");
            }
            html.append("\" href=\"")
                .append(escapeHtml(link.url()))
                .append("\"");
            if (current) {
                html.append(" aria-current=\"page\"");
            }
            if (link.fallbackUsed()) {
                html.append(" title=\"")
                    .append(escapeHtml(uiText.fallbackTitle(displayLanguage(link.sourceLanguage(), currentLanguage))))
                    .append("\"");
            }
            html.append("><span class=\"guide-language-link-label\">")
                .append(escapeHtml(displayLanguage(link.language(), currentLanguage)))
                .append("</span>");
            if (link.fallbackUsed()) {
                html.append("<span class=\"guide-language-link-badge\">")
                    .append(escapeHtml(uiText.fallbackBadge()))
                    .append("</span>");
            }
            html.append("</a>");
        }
        html.append("</div></div>");
    }

    private Map<String, Object> navigationNodeData(MutableGuide guide, String language, NavigationNode node) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", node.title());
        data.put("position", Integer.valueOf(node.position()));
        if (node.pageId() != null) {
            data.put(
                "pageId",
                node.pageId()
                    .toString());
            data.put(
                "url",
                pageUrl(
                    guide.getId()
                        .getResourceDomain(),
                    guide.getId()
                        .getResourcePath(),
                    language,
                    toOutputPageFile(node.pageId())));
        }

        List<Map<String, Object>> children = new ArrayList<>();
        for (NavigationNode child : node.children()) {
            children.add(navigationNodeData(guide, language, child));
        }
        data.put("children", children);
        return data;
    }

    private void appendNavigationNode(StringBuilder html, MutableGuide guide, String language, NavigationNode node,
        ResourceLocation currentPageId, GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        html.append("<li>");
        if (node.pageId() != null) {
            String href = pageUrl(
                guide.getId()
                    .getResourceDomain(),
                guide.getId()
                    .getResourcePath(),
                language,
                toOutputPageFile(node.pageId()));
            html.append("<a href=\"")
                .append(escapeHtml(href))
                .append("\"");
            if (node.pageId()
                .equals(currentPageId)) {
                html.append(" aria-current=\"page\"");
            }
            html.append(">")
                .append(renderNavigationLinkContent(node.title(), node.icon(), assetExporter, itemIconResolver))
                .append("</a>");
        } else {
            html.append("<span>")
                .append(renderNavigationLinkContent(node.title(), node.icon(), assetExporter, itemIconResolver))
                .append("</span>");
        }
        if (!node.children()
            .isEmpty()) {
            html.append("<ul>");
            for (NavigationNode child : node.children()) {
                appendNavigationNode(html, guide, language, child, currentPageId, assetExporter, itemIconResolver);
            }
            html.append("</ul>");
        }
        html.append("</li>");
    }

    private String renderNavigationLinkContent(String title, @Nullable GuidePageIcon icon,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        StringBuilder html = new StringBuilder();
        appendNavigationIcon(html, icon, assetExporter, itemIconResolver);
        html.append("<span class=\"guide-generated-link-text\">")
            .append(escapeHtml(title))
            .append("</span>");
        return html.toString();
    }

    private void appendNavigationIcon(StringBuilder html, @Nullable GuidePageIcon icon,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        if (icon == null) {
            return;
        }
        if (icon.isItemIcon() && icon.itemStack() != null) {
            GuideSiteItemHtml.appendIcon(
                html,
                GuideSiteItemSupport.export(icon.itemStack(), itemIconResolver),
                "guide-nav-item-icon");
            return;
        }
        if (icon.textureId() != null && assetExporter != null) {
            String src = assetExporter.exportResource(icon.textureId());
            if (!src.isEmpty()) {
                html.append("<img class=\"item-icon guide-nav-item-icon\" src=\"")
                    .append(escapeHtml(src))
                    .append("\" alt=\"\" width=\"32\" height=\"32\" decoding=\"async\">");
            }
        }
    }

    private String toOutputPageFile(ResourceLocation pageId) {
        String path = pageId.getResourcePath();
        if (path.endsWith(".md")) {
            return path.substring(0, path.length() - 3) + ".html";
        }
        return path + ".html";
    }

    private String loadText(String resourcePath) throws Exception {
        try (InputStream in = GuideSiteWriter.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resourcePath);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String relativeRoot(Path outDir, Path pagePath) {
        Path pageParent = pagePath.getParent();
        if (pageParent == null) {
            return ".";
        }

        String relative = pageParent.relativize(outDir)
            .toString()
            .replace('\\', '/');
        return relative.isEmpty() ? "." : relative;
    }

    private String displayLanguage(String language, String currentLanguage) {
        String normalized = normalizeLanguage(language);
        boolean chineseUi = normalizeLanguage(currentLanguage).startsWith("zh");
        return switch (normalized) {
            case "en_us" -> chineseUi ? "English" : "English";
            case "zh_cn" -> chineseUi ? "简体中文" : "Simplified Chinese";
            case "zh_tw" -> chineseUi ? "繁體中文" : "Traditional Chinese";
            case "ja_jp" -> chineseUi ? "日本語" : "Japanese";
            case "ko_kr" -> chineseUi ? "한국어" : "Korean";
            case "ru_ru" -> chineseUi ? "Русский" : "Russian";
            default -> normalized.replace('_', '-')
                .toLowerCase(Locale.ROOT);
        };
    }

    private String normalizeLanguage(String language) {
        return language == null ? "" : language.toLowerCase(Locale.ROOT);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static final class SiteUiText {

        private final String searchLabel;
        private final String searchPlaceholder;
        private final String searchEmptyTemplate;
        private final String languagesLabel;
        private final String fallbackBadge;
        private final String fallbackPrefix;

        private SiteUiText(String searchLabel, String searchPlaceholder, String searchEmptyTemplate,
            String languagesLabel, String fallbackBadge, String fallbackPrefix) {
            this.searchLabel = searchLabel;
            this.searchPlaceholder = searchPlaceholder;
            this.searchEmptyTemplate = searchEmptyTemplate;
            this.languagesLabel = languagesLabel;
            this.fallbackBadge = fallbackBadge;
            this.fallbackPrefix = fallbackPrefix;
        }

        private static SiteUiText forLanguage(String language) {
            String normalized = language != null ? language.toLowerCase(Locale.ROOT) : "";
            if (normalized.startsWith("zh")) {
                return new SiteUiText("搜索", "搜索页面", "没有找到“{{query}}”的匹配项", "语言", "回退", "回退自");
            }
            return new SiteUiText(
                "Search",
                "Search pages",
                "No matches for \"{{query}}\"",
                "Languages",
                "Fallback",
                "Fallback from");
        }

        private String searchLabel() {
            return searchLabel;
        }

        private String searchPlaceholder() {
            return searchPlaceholder;
        }

        private String searchEmptyTemplate() {
            return searchEmptyTemplate;
        }

        private String languagesLabel() {
            return languagesLabel;
        }

        private String fallbackBadge() {
            return fallbackBadge;
        }

        private String fallbackTitle(String sourceLanguage) {
            return fallbackPrefix + " " + sourceLanguage;
        }
    }

    private void writeResource(Path target, String resourcePath) throws Exception {
        Files.createDirectories(target.getParent());
        try (InputStream in = GuideSiteWriter.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resourcePath);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteRecursively(Path target, Path outDir) throws Exception {
        Path normalizedTarget = target.toAbsolutePath()
            .normalize();
        if (!normalizedTarget.startsWith(outDir)) {
            throw new IllegalArgumentException("Refusing to delete path outside export directory: " + normalizedTarget);
        }
        if (!Files.exists(normalizedTarget)) {
            return;
        }

        if (Files.isDirectory(normalizedTarget)) {
            try (Stream<Path> stream = Files.walk(normalizedTarget)) {
                for (Path path : stream.sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList())) {
                    Files.deleteIfExists(path);
                }
            }
            return;
        }

        Files.deleteIfExists(normalizedTarget);
    }
}
