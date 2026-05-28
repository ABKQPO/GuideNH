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
            outDir.resolve("_site/model-viewer/vendor/diamond.png"),
            "/assets/guidenh/textures/guide/diamond.png");
        writeResource(
            outDir.resolve("_site/fonts/PixeloidMono.woff2"),
            "/assets/guidenh/siteexport/fonts/PixeloidMono.woff2");
        writeResource(
            outDir.resolve("_site/fonts/PixeloidMono.woff"),
            "/assets/guidenh/siteexport/fonts/PixeloidMono.woff");
        writeResource(
            outDir.resolve("_site/fonts/PixeloidSans-Bold.woff2"),
            "/assets/guidenh/siteexport/fonts/PixeloidSans-Bold.woff2");
        writeResource(
            outDir.resolve("_site/fonts/PixeloidSans-Bold.woff"),
            "/assets/guidenh/siteexport/fonts/PixeloidSans-Bold.woff");
        writeResource(
            outDir.resolve("_site/fonts/PixeloidSans.woff2"),
            "/assets/guidenh/siteexport/fonts/PixeloidSans.woff2");
        writeResource(
            outDir.resolve("_site/fonts/PixeloidSans.woff"),
            "/assets/guidenh/siteexport/fonts/PixeloidSans.woff");
        writeResource(
            outDir.resolve("_site/textures/background.png"),
            "/assets/guidenh/siteexport/textures/background.png");
        writeResource(outDir.resolve("_site/textures/guide/buttons.png"), "/assets/guidenh/textures/guide/buttons.png");
        writeResource(outDir.resolve("_site/textures/slot.png"), "/assets/guidenh/siteexport/textures/slot.png");
        writeResource(
            outDir.resolve("_site/textures/slot_cross.png"),
            "/assets/guidenh/siteexport/textures/slot_cross.png");
        writeResource(
            outDir.resolve("_site/textures/sky_stone_block.png"),
            "/assets/guidenh/siteexport/textures/sky_stone_block.png");
        writeResource(
            outDir.resolve("_site/textures/recipe_arrow_light.png"),
            "/assets/guidenh/siteexport/textures/recipe_arrow_light.png");
        writeResource(
            outDir.resolve("_site/textures/recipe_arrow_filled_light.png"),
            "/assets/guidenh/siteexport/textures/recipe_arrow_filled_light.png");
        writeResource(
            outDir.resolve("_site/textures/large_slot_light.png"),
            "/assets/guidenh/siteexport/textures/large_slot_light.png");
        writeResource(
            outDir.resolve("_site/textures/listitem.svg"),
            "/assets/guidenh/siteexport/textures/listitem.svg");
        writeExternalLinkPage(outDir);
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
        deleteRecursively(normalizedOutDir.resolve(".guidenh-site-server.state"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve(".guidenh-site-server"), normalizedOutDir);
        deleteRecursively(normalizedOutDir.resolve("export-report.json"), normalizedOutDir);
    }

    public void writePage(Path outDir, String namespace, String guidePath, String language, String pageRelativeFile,
        String langSwitcherHtml, String sidebarHtml, String contentHtml, List<String> templateHtml, String title)
        throws Exception {
        Path pagePath = outDir.resolve(Paths.get("guides", namespace, guidePath, language))
            .resolve(pageRelativeFile);
        Files.createDirectories(pagePath.getParent());

        String layout = loadText("/assets/guidenh/siteexport/layout.html").replace("{{lang}}", escapeHtml(language))
            .replace("{{title}}", escapeHtml(title))
            .replace("{{lang_switcher}}", langSwitcherHtml)
            .replace("{{sidebar}}", sidebarHtml)
            .replace("{{content}}", contentHtml + String.join("", templateHtml))
            .replace("{{root}}", relativeRoot(outDir, pagePath));
        Files.writeString(pagePath, layout);
    }

    public void writeNavigationIndex(Path outDir, String namespace, String guidePath, String language, String json)
        throws Exception {
        Path path = outDir.resolve(Paths.get("_data", "nav", namespace, guidePath, language + ".json"));
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }

    public void writeSearchIndex(Path outDir, String language, String json) throws Exception {
        Path path = outDir.resolve(Paths.get("_data", "search", language + ".json"));
        Files.createDirectories(path.getParent());
        Files.writeString(path, json);
    }

    public void writeReport(Path outDir, String json) throws Exception {
        Files.writeString(outDir.resolve("export-report.json"), json);
    }

    public void writeLandingPage(Path outDir, @Nullable String firstPageUrl, String title) throws Exception {
        SiteUiText uiText = SiteUiText.forLanguage("en_us");
        String html;
        if (firstPageUrl == null || firstPageUrl.isEmpty()) {
            html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + escapeHtml(title)
                + "</title></head><body><main><h1>"
                + escapeHtml(title)
                + "</h1><p>"
                + escapeHtml(uiText.siteExportNoPages())
                + "</p></main></body></html>";
        } else {
            String escapedUrl = escapeHtml(firstPageUrl);
            html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + escapeHtml(title)
                + "</title><meta http-equiv=\"refresh\" content=\"0; url="
                + escapedUrl
                + "\"></head><body><p><a href=\""
                + escapedUrl
                + "\">"
                + escapeHtml(uiText.siteExportOpenGuide())
                + "</a></p></body></html>";
        }
        Files.writeString(outDir.resolve("index.html"), html);
    }

    public void writeExternalLinkPage(Path outDir) throws Exception {
        Path pagePath = outDir.resolve(Paths.get("_site", "external-link.html"));
        Files.createDirectories(pagePath.getParent());
        SiteUiText english = SiteUiText.forLanguage("en_us");
        SiteUiText chinese = SiteUiText.forLanguage("zh_cn");
        String html = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>{{title}}</title>
              <link rel="stylesheet" href="./app.css">
            </head>
            <body class="guide-site-external-body">
              <main class="guide-site-external-card">
                <h1 id="guide-external-title">{{title}}</h1>
                <p id="guide-external-message">{{message}}</p>
                <p class="guide-site-external-target" id="guide-external-target"></p>
                <div class="guide-site-external-actions">
                  <a class="guide-mediawiki-special-link" id="guide-external-open" href="#">{{open}}</a>
                  <a class="guide-site-external-cancel" href="javascript:history.back()">{{back}}</a>
                </div>
              </main>
              <script>
              (function () {
                var params = new URLSearchParams(window.location.search);
                var target = params.get("target") || "";
                var label = params.get("label") || target;
                var lang = (params.get("lang") || "en_us").toLowerCase();
                var zh = lang.indexOf("zh") === 0;
                var title = zh ? '{{zh_title}}' : '{{en_title}}';
                var message = zh ? '{{zh_message}}' : '{{en_message}}';
                var openText = zh ? '{{zh_open}}' : '{{en_open}}';
                var backText = zh ? '{{zh_back}}' : '{{en_back}}';
                document.title = title;
                document.getElementById("guide-external-title").textContent = title;
                document.getElementById("guide-external-message").textContent = message;
                document.getElementById("guide-external-target").textContent = label || target;
                var open = document.getElementById("guide-external-open");
                open.textContent = openText;
                if (target) {
                  open.href = target;
                  open.rel = "noopener noreferrer";
                  open.target = "_blank";
                } else {
                  open.removeAttribute("href");
                }
                document.querySelector(".guide-site-external-cancel").textContent = backText;
              }());
              </script>
            </body>
            </html>
            """.replace("{{title}}", escapeHtml(english.externalLinkTitle()))
            .replace("{{message}}", escapeHtml(english.externalLinkMessage()))
            .replace("{{open}}", escapeHtml(english.externalLinkOpen()))
            .replace("{{back}}", escapeHtml(english.externalLinkBack()))
            .replace("{{en_title}}", escapeJsString(english.externalLinkTitle()))
            .replace("{{en_message}}", escapeJsString(english.externalLinkMessage()))
            .replace("{{en_open}}", escapeJsString(english.externalLinkOpen()))
            .replace("{{en_back}}", escapeJsString(english.externalLinkBack()))
            .replace("{{zh_title}}", escapeJsString(chinese.externalLinkTitle()))
            .replace("{{zh_message}}", escapeJsString(chinese.externalLinkMessage()))
            .replace("{{zh_open}}", escapeJsString(chinese.externalLinkOpen()))
            .replace("{{zh_back}}", escapeJsString(chinese.externalLinkBack()));
        Files.writeString(pagePath, html);
    }

    public String pageUrl(String namespace, String guidePath, String language, String pageRelativeFile) {
        return "guides/" + namespace + "/" + guidePath + "/" + language + "/" + pageRelativeFile.replace('\\', '/');
    }

    public String navigationJson(MutableGuide guide, String language, NavigationTree tree) {
        List<Map<String, Object>> rootNodes = new ArrayList<>();
        for (NavigationNode node : tree.getRootNodes()) {
            rootNodes.add(navigationNodeData(language, node));
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
        return renderSidebar(guide, language, tree, currentPageId, assetExporter, itemIconResolver, null, null);
    }

    public String renderLanguageSwitcher(String language, @Nullable List<GuideSiteLanguageLink> languageLinks) {
        SiteUiText uiText = SiteUiText.forLanguage(language);
        StringBuilder html = new StringBuilder();
        appendLanguageSwitcher(html, language, languageLinks, uiText);
        return html.toString();
    }

    public String renderSidebar(MutableGuide guide, String language, NavigationTree tree,
        ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable List<GuideSiteLanguageLink> languageLinks) {
        return renderSidebar(
            guide,
            language,
            tree,
            currentPageId,
            assetExporter,
            itemIconResolver,
            languageLinks,
            null);
    }

    public String renderSidebar(MutableGuide guide, String language, NavigationTree tree,
        ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable List<GuideSiteLanguageLink> languageLinks,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
        SiteUiText uiText = SiteUiText.forLanguage(language);
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-sidebar-tools\">");
        // Language switching is offered in the page header; avoid duplicating it inside the sidebar.
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
            appendNavigationNode(
                html,
                language,
                node,
                currentPageId,
                assetExporter,
                itemIconResolver,
                assetExportersByGuideId);
        }
        html.append("</ul></nav>");
        return html.toString();
    }

    private void writeStartScripts(Path outDir) throws Exception {
        Files.writeString(outDir.resolve("start.bat"), windowsStartScript());
        Files.writeString(outDir.resolve("stop.bat"), windowsStopScript());
        Path startSh = outDir.resolve("start.sh");
        Path stopSh = outDir.resolve("stop.sh");
        Files.writeString(startSh, unixStartScript());
        Files.writeString(stopSh, unixStopScript());
        trySetExecutable(startSh);
        trySetExecutable(stopSh);
    }

    private String windowsStartScript() {
        return """
            @echo off\r
            setlocal\r
            set "PORT=8734"\r
            set "SITE_DIR=%~dp0."\r
            set "SERVER_JAR=%SITE_DIR%\\_site\\guidenh-site-server.jar"\r
            set "STATE_FILE=%SITE_DIR%\\.guidenh-site-server.state"\r
            set "LEGACY_PID_FILE=%SITE_DIR%\\.guidenh-site-server.pid"\r
            set "LOG_DIR=%SITE_DIR%\\.guidenh-site-server"\r
            set "STDOUT_LOG=%LOG_DIR%\\stdout.log"\r
            set "STDERR_LOG=%LOG_DIR%\\stderr.log"\r
            if not exist "%SERVER_JAR%" (\r
              echo Missing bundled server jar: "%SERVER_JAR%"\r
              exit /b 1\r
            )\r
            set "JAVA_EXE="\r
            if defined JAVA_HOME if exist "%JAVA_HOME%\\bin\\java.exe" set "JAVA_EXE=%JAVA_HOME%\\bin\\java.exe"\r
            if not defined JAVA_EXE for /f "usebackq delims=" %%J in (`where java 2^>nul`) do if not defined JAVA_EXE set "JAVA_EXE=%%J"\r
            if not defined JAVA_EXE (\r
              echo Java runtime not found. Install Java and run this script again.\r
              exit /b 1\r
            )\r
            if not exist "%STATE_FILE%" if exist "%LEGACY_PID_FILE%" set "STATE_FILE=%LEGACY_PID_FILE%"\r
            "%JAVA_EXE%" -jar "%SERVER_JAR%" status "%STATE_FILE%" >nul 2>nul\r
            if not errorlevel 1 (\r
              start "" "http://127.0.0.1:%PORT%/index.html"\r
              exit /b 0\r
            )\r
            if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"\r
            powershell -NoProfile -ExecutionPolicy Bypass -Command ^\r
              "$javaExe = $env:JAVA_EXE; $jar = $env:SERVER_JAR; $dir = $env:SITE_DIR; $stateFile = $env:STATE_FILE; $stdoutLog = $env:STDOUT_LOG; $stderrLog = $env:STDERR_LOG; " ^\r
              "Start-Process -FilePath $javaExe -ArgumentList @('-jar', $jar, 'serve', $dir, $env:PORT, '127.0.0.1', $stateFile) -WorkingDirectory $dir -WindowStyle Hidden -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog | Out-Null"\r
            if errorlevel 1 (\r
              echo Failed to start bundled Java site server.\r
              exit /b 1\r
            )\r
            set /a WAIT_COUNT=0\r
            :wait_for_server\r
            "%JAVA_EXE%" -jar "%SERVER_JAR%" status "%STATE_FILE%" >nul 2>nul\r
            if not errorlevel 1 goto server_ready\r
            if %WAIT_COUNT% GEQ 10 goto server_failed\r
            set /a WAIT_COUNT+=1\r
            timeout /t 1 /nobreak >nul\r
            goto wait_for_server\r
            :server_ready\r
            start "" "http://127.0.0.1:%PORT%/index.html"\r
            exit /b 0\r
            :server_failed\r
            echo Failed to confirm bundled Java site server startup.\r
            echo Check logs under "%LOG_DIR%".\r
            exit /b 1\r
            """;
    }

    private String windowsStopScript() {
        return """
            @echo off\r
            setlocal\r
            set "SITE_DIR=%~dp0."\r
            set "SERVER_JAR=%SITE_DIR%\\_site\\guidenh-site-server.jar"\r
            set "STATE_FILE=%SITE_DIR%\\.guidenh-site-server.state"\r
            set "LEGACY_PID_FILE=%SITE_DIR%\\.guidenh-site-server.pid"\r
            if not exist "%SERVER_JAR%" (\r
              echo Missing bundled server jar: "%SERVER_JAR%"\r
              exit /b 1\r
            )\r
            set "JAVA_EXE="\r
            if defined JAVA_HOME if exist "%JAVA_HOME%\\bin\\java.exe" set "JAVA_EXE=%JAVA_HOME%\\bin\\java.exe"\r
            if not defined JAVA_EXE for /f "usebackq delims=" %%J in (`where java 2^>nul`) do if not defined JAVA_EXE set "JAVA_EXE=%%J"\r
            if not defined JAVA_EXE (\r
              echo Java runtime not found. Install Java and run this script again.\r
              exit /b 1\r
            )\r
            if not exist "%STATE_FILE%" if exist "%LEGACY_PID_FILE%" set "STATE_FILE=%LEGACY_PID_FILE%"\r
            if not exist "%STATE_FILE%" (\r
              echo GuideNH static site server is not running.\r
              exit /b 0\r
            )\r
            "%JAVA_EXE%" -jar "%SERVER_JAR%" stop "%STATE_FILE%"\r
            """;
    }

    private String unixStartScript() {
        return """
            #!/usr/bin/env sh
            DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
            PORT=8734
            SERVER_JAR="$DIR/_site/guidenh-site-server.jar"
            STATE_FILE="$DIR/.guidenh-site-server.state"
            LEGACY_PID_FILE="$DIR/.guidenh-site-server.pid"
            LOG_DIR="$DIR/.guidenh-site-server"
            STDOUT_LOG="$LOG_DIR/stdout.log"
            STDERR_LOG="$LOG_DIR/stderr.log"

            open_browser() {
              URL="http://127.0.0.1:$PORT/index.html"
              if command -v xdg-open >/dev/null 2>&1; then
                xdg-open "$URL" >/dev/null 2>&1
              elif command -v open >/dev/null 2>&1; then
                open "$URL" >/dev/null 2>&1
              fi
            }

            if [ ! -f "$SERVER_JAR" ]; then
              echo "Missing bundled server jar: $SERVER_JAR"
              exit 1
            fi
            if ! command -v java >/dev/null 2>&1; then
              echo "Java runtime not found. Install Java and run this script again."
              exit 1
            fi
            if [ ! -f "$STATE_FILE" ] && [ -f "$LEGACY_PID_FILE" ]; then
              STATE_FILE="$LEGACY_PID_FILE"
            fi
            if java -jar "$SERVER_JAR" status "$STATE_FILE" >/dev/null 2>&1; then
              open_browser
              exit 0
            fi
            mkdir -p "$LOG_DIR"
            if command -v nohup >/dev/null 2>&1; then
              (cd "$DIR" && nohup java -jar "$SERVER_JAR" serve "$DIR" "$PORT" "127.0.0.1" "$STATE_FILE" >"$STDOUT_LOG" 2>"$STDERR_LOG" </dev/null &)
            else
              (cd "$DIR" && java -jar "$SERVER_JAR" serve "$DIR" "$PORT" "127.0.0.1" "$STATE_FILE" >"$STDOUT_LOG" 2>"$STDERR_LOG" </dev/null &)
            fi
            attempt=0
            while ! java -jar "$SERVER_JAR" status "$STATE_FILE" >/dev/null 2>&1; do
              attempt=$((attempt + 1))
              if [ "$attempt" -ge 10 ]; then
                echo "Failed to confirm bundled Java site server startup."
                echo "Check logs under $LOG_DIR"
                exit 1
              fi
              sleep 1
            done
            open_browser
            """;
    }

    private String unixStopScript() {
        return """
            #!/usr/bin/env sh
            DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
            SERVER_JAR="$DIR/_site/guidenh-site-server.jar"
            STATE_FILE="$DIR/.guidenh-site-server.state"
            LEGACY_PID_FILE="$DIR/.guidenh-site-server.pid"
            if [ ! -f "$SERVER_JAR" ]; then
              echo "Missing bundled server jar: $SERVER_JAR"
              exit 1
            fi
            if ! command -v java >/dev/null 2>&1; then
              echo "Java runtime not found. Install Java and run this script again."
              exit 1
            fi
            if [ ! -f "$STATE_FILE" ] && [ -f "$LEGACY_PID_FILE" ]; then
              STATE_FILE="$LEGACY_PID_FILE"
            fi
            if [ ! -f "$STATE_FILE" ]; then
              echo "GuideNH static site server is not running."
              exit 0
            fi
            java -jar "$SERVER_JAR" stop "$STATE_FILE"
            """;
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
                    .append(
                        escapeHtml(
                            uiText.fallbackTitle(
                                link.sourceLanguage() != null ? displayLanguage(link.sourceLanguage(), currentLanguage)
                                    : uiText.sharedPageLabel())))
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

    private Map<String, Object> navigationNodeData(String language, NavigationNode node) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", node.title());
        data.put("position", node.position());
        if (node.pageId() != null) {
            data.put(
                "pageId",
                node.pageId()
                    .toString());
            data.put("url", pageUrlForNode(language, node));
        }

        List<Map<String, Object>> children = new ArrayList<>();
        for (NavigationNode child : node.children()) {
            children.add(navigationNodeData(language, child));
        }
        data.put("children", children);
        return data;
    }

    private void appendNavigationNode(StringBuilder html, String language, NavigationNode node,
        ResourceLocation currentPageId, GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
        html.append("<li>");
        if (node.pageId() != null) {
            String href = pageUrlForNode(language, node);
            html.append("<a href=\"")
                .append(escapeHtml(href))
                .append("\"");
            if (node.pageId()
                .equals(currentPageId)) {
                html.append(" aria-current=\"page\"");
            }
            html.append(">")
                .append(
                    renderNavigationLinkContent(
                        node.title(),
                        node.icon(),
                        node.guideId(),
                        assetExporter,
                        itemIconResolver,
                        assetExportersByGuideId))
                .append("</a>");
        } else {
            html.append("<span>")
                .append(
                    renderNavigationLinkContent(
                        node.title(),
                        node.icon(),
                        node.guideId(),
                        assetExporter,
                        itemIconResolver,
                        assetExportersByGuideId))
                .append("</span>");
        }
        if (!node.children()
            .isEmpty()) {
            html.append("<ul>");
            for (NavigationNode child : node.children()) {
                appendNavigationNode(
                    html,
                    language,
                    child,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    assetExportersByGuideId);
            }
            html.append("</ul>");
        }
        html.append("</li>");
    }

    private String pageUrlForNode(String language, NavigationNode node) {
        ResourceLocation guideId = node.guideId() != null ? node.guideId()
            : node.pageId() != null ? new ResourceLocation(
                node.pageId()
                    .getResourceDomain(),
                "guidenh") : null;
        ResourceLocation pageId = node.pageId();
        if (guideId == null || pageId == null) {
            return "";
        }
        return pageUrl(guideId.getResourceDomain(), guideId.getResourcePath(), language, toOutputPageFile(pageId));
    }

    private String renderNavigationLinkContent(String title, @Nullable GuidePageIcon icon,
        @Nullable ResourceLocation guideId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
        StringBuilder html = new StringBuilder();
        appendNavigationIcon(html, icon, guideId, assetExporter, itemIconResolver, assetExportersByGuideId);
        html.append("<span class=\"guide-generated-link-text\">")
            .append(escapeHtml(title))
            .append("</span>");
        return html.toString();
    }

    private void appendNavigationIcon(StringBuilder html, @Nullable GuidePageIcon icon,
        @Nullable ResourceLocation guideId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
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
        GuideSitePageAssetExporter resolvedAssetExporter = resolveAssetExporter(
            guideId,
            assetExporter,
            assetExportersByGuideId);
        if (resolvedAssetExporter == null) {
            return;
        }
        // Resolve a texture resource id from either the explicit textureId field or the
        // GuidePageTexture wrapper. Frontmatter "icon: ns:textures/..." populates textureId,
        // while some loaders only set the texture wrapper, so we cover both paths here.
        ResourceLocation resolvedTextureId = icon.resolveCurrentTextureId();
        if (resolvedTextureId == null && icon.resolveCurrentTexture() != null) {
            resolvedTextureId = icon.resolveCurrentTexture()
                .getSourceId();
        }
        if (resolvedTextureId == null) {
            return;
        }
        String src = resolvedAssetExporter.exportResource(resolvedTextureId);
        if (!src.isEmpty()) {
            html.append("<img class=\"item-icon guide-nav-item-icon\" src=\"")
                .append(escapeHtml(src))
                .append("\" alt=\"\" width=\"32\" height=\"32\" decoding=\"async\">");
        }
    }

    @Nullable
    private GuideSitePageAssetExporter resolveAssetExporter(@Nullable ResourceLocation guideId,
        @Nullable GuideSitePageAssetExporter assetExporter,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
        if (guideId != null && assetExportersByGuideId != null) {
            GuideSitePageAssetExporter mapped = assetExportersByGuideId.get(guideId);
            if (mapped != null) {
                return mapped;
            }
        }
        return assetExporter;
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
            return out.toString(StandardCharsets.UTF_8);
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
            case "en_us" -> "English";
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

    private String escapeJsString(String text) {
        return text.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", "\\n");
    }

    private static final class SiteUiText {

        private final String searchLabel;
        private final String searchPlaceholder;
        private final String searchEmptyTemplate;
        private final String languagesLabel;
        private final String fallbackBadge;
        private final String fallbackPrefix;
        private final String siteExportNoPages;
        private final String siteExportOpenGuide;
        private final String externalLinkTitle;
        private final String externalLinkMessage;
        private final String externalLinkOpen;
        private final String externalLinkBack;

        private SiteUiText(String searchLabel, String searchPlaceholder, String searchEmptyTemplate,
            String languagesLabel, String fallbackBadge, String fallbackPrefix, String siteExportNoPages,
            String siteExportOpenGuide, String externalLinkTitle, String externalLinkMessage, String externalLinkOpen,
            String externalLinkBack) {
            this.searchLabel = searchLabel;
            this.searchPlaceholder = searchPlaceholder;
            this.searchEmptyTemplate = searchEmptyTemplate;
            this.languagesLabel = languagesLabel;
            this.fallbackBadge = fallbackBadge;
            this.fallbackPrefix = fallbackPrefix;
            this.siteExportNoPages = siteExportNoPages;
            this.siteExportOpenGuide = siteExportOpenGuide;
            this.externalLinkTitle = externalLinkTitle;
            this.externalLinkMessage = externalLinkMessage;
            this.externalLinkOpen = externalLinkOpen;
            this.externalLinkBack = externalLinkBack;
        }

        private static SiteUiText forLanguage(String language) {
            String normalized = language != null ? language.toLowerCase(Locale.ROOT) : "";
            if (normalized.startsWith("zh")) {
                return new SiteUiText(
                    "搜索",
                    "搜索页面",
                    "没有找到“{{query}}”的匹配项",
                    "语言",
                    "回退",
                    "回退自",
                    "没有导出任何指南页面。",
                    "打开导出的指南",
                    "外部链接",
                    "你即将打开一个外部链接。",
                    "继续打开",
                    "返回");
            }
            return new SiteUiText(
                "Search",
                "Search pages",
                "No matches for \"{{query}}\"",
                "Languages",
                "Fallback",
                "Fallback from",
                "No guide pages were exported.",
                "Open exported guide",
                "External Link",
                "You are about to open an external link.",
                "Open Link",
                "Back");
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

        private String sharedPageLabel() {
            return "Fallback from".equals(fallbackPrefix) ? "Shared page" : "共享页面";
        }

        private String siteExportNoPages() {
            return siteExportNoPages;
        }

        private String siteExportOpenGuide() {
            return siteExportOpenGuide;
        }

        private String externalLinkTitle() {
            return externalLinkTitle;
        }

        private String externalLinkMessage() {
            return externalLinkMessage;
        }

        private String externalLinkOpen() {
            return externalLinkOpen;
        }

        private String externalLinkBack() {
            return externalLinkBack;
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
                    .toList()) {
                    Files.deleteIfExists(path);
                }
            }
            return;
        }

        Files.deleteIfExists(normalizedTarget);
    }
}
