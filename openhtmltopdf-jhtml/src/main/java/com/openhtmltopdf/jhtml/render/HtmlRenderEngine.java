package com.openhtmltopdf.jhtml.render;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.htmlunit.WebClient;
import org.htmlunit.WebClientOptions;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.jsoup.HtmlUnitDOMToJsoupConverter;
import org.jsoup.nodes.Document;

import com.openhtmltopdf.jhtml.util.JsoupUtil;

/**
 * Thread-safe HTML rendering engine based on HtmlUnit.
 * <p>
 * This engine maintains a separate {@link WebClient} instance per thread (via {@link ThreadLocal}),
 * making it suitable for high-concurrency environments. It executes JavaScript snippets on the loaded page,
 * optionally waits for background (asynchronous) JavaScript tasks, and returns either the rendered HTML as a
 * string ({@code page.asXml()}) or as a jsoup {@link Document}.
 * </p>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Simple rendering with a single JS snippet
 * String result = HtmlRenderEngine.render("<html>...</html>", "document.title='New Title';");
 *
 * // With waiting for background JS (500 ms)
 * String result2 = HtmlRenderEngine.render("<html>...</html>", "someJS();", 500);
 *
 * // Multiple JS snippets and custom configuration
 * RenderConfig config = RenderConfig.builder()
 *     .waitForBackgroundJs(2, TimeUnit.SECONDS)
 *     .javaScriptTimeout(3, TimeUnit.SECONDS)
 *     .build();
 * Document doc = HtmlRenderEngine.renderToDocument(config, "<html>...</html>", "js1();", "js2();");
 * }</pre>
 *
 * @author (original) openhtmltopdf community, enhanced by AI
 * @see WebClient
 * @see HtmlUnitDOMToJsoupConverter
 */
public final class HtmlRenderEngine {

    // ---------- Static fields ----------

    /**
     * Thread-local storage for WebClient instances.
     * Each thread gets its own WebClient, which is reused across multiple render calls on the same thread.
     * This avoids the overhead of creating a new WebClient for each request and ensures thread safety.
     */
    private static final ThreadLocal<WebClient> WEB_CLIENT_HOLDER = ThreadLocal.withInitial(() -> {
        WebClient wc = createDefaultWebClient();
        registerCleanupHook();
        return wc;
    });

    /**
     * Flag to ensure the JVM shutdown hook is registered only once.
     */
    private static volatile boolean cleanupHookRegistered = false;

  
    // ---------- Private constructor ----------

    private HtmlRenderEngine() {
        // Utility class, no instantiation
    }

    // ---------- Public API: return String (page.asXml()) ----------

    /**
     * Renders the given HTML content, executes a single JavaScript snippet,
     * and returns the final HTML as a string.
     * <p>Equivalent to {@code render(htmlContent, jsToExecute, 0)}.</p>
     *
     * @param htmlContent the raw HTML to load
     * @param jsToExecute the JavaScript code to execute (may be {@code null} or empty)
     * @return the rendered HTML as produced by {@link HtmlPage#asXml()}
     * @throws IOException if loading the HTML into HtmlUnit fails
     */
    public static String render(String htmlContent, String jsToExecute) throws IOException {
        return render(RenderConfig.defaultConfig(), htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content, executes multiple JavaScript snippets,
     * and returns the final HTML as a string.
     * <p>Equivalent to {@code render(RenderConfig.defaultConfig(), htmlContent, jsToExecute)}.</p>
     *
     * @param htmlContent the raw HTML to load
     * @param jsToExecute zero or more JavaScript snippets to execute
     * @return the rendered HTML as produced by {@link HtmlPage#asXml()}
     * @throws IOException if loading the HTML into HtmlUnit fails
     */
    public static String render(String htmlContent, String... jsToExecute) throws IOException {
        return render(RenderConfig.defaultConfig(), htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content, executes a single JavaScript snippet,
     * waits for background JavaScript execution for the specified time (in milliseconds),
     * and returns the final HTML as a string.
     *
     * @param htmlContent               the raw HTML to load
     * @param jsToExecute               the JavaScript code to execute (may be {@code null} or empty)
     * @param waitForBackgroundJsMillis milliseconds to wait for background JS (0 = don't wait)
     * @return the rendered HTML as produced by {@link HtmlPage#asXml()}
     * @throws IOException if loading the HTML into HtmlUnit fails
     */
    public static String render(String htmlContent, String jsToExecute, int waitForBackgroundJsMillis) throws IOException {
        RenderConfig config = RenderConfig.builder()
                .waitForBackgroundJs(waitForBackgroundJsMillis, TimeUnit.MILLISECONDS)
                .build();
        return render(config, htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content, executes multiple JavaScript snippets,
     * waits for background JavaScript execution for the specified time (in milliseconds),
     * and returns the final HTML as a string.
     *
     * @param htmlContent               the raw HTML to load
     * @param waitForBackgroundJsMillis milliseconds to wait for background JS (0 = don't wait)
     * @param jsToExecute               zero or more JavaScript snippets to execute
     * @return the rendered HTML as produced by {@link HtmlPage#asXml()}
     * @throws IOException if loading the HTML into HtmlUnit fails
     */
    public static String render(String htmlContent, int waitForBackgroundJsMillis, String... jsToExecute) throws IOException {
        RenderConfig config = RenderConfig.builder()
                .waitForBackgroundJs(waitForBackgroundJsMillis, TimeUnit.MILLISECONDS)
                .build();
        return render(config, htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content with full control via {@link RenderConfig},
     * executes the provided JavaScript snippets, and returns the final HTML as a string.
     *
     * @param config      the rendering configuration
     * @param htmlContent the raw HTML to load
     * @param jsSnippets  one or more JavaScript snippets to execute
     * @return the rendered HTML as produced by {@link HtmlPage#asXml()}
     * @throws IOException if loading the HTML into HtmlUnit fails
     */
    public static String render(RenderConfig config, String htmlContent, String... jsSnippets) throws IOException {
        HtmlPage page = executePage(config, htmlContent, jsSnippets);
        return page.asXml();
    }

    // ---------- Public API: return jsoup Document ----------

    /**
     * Renders the given HTML content, executes a single JavaScript snippet,
     * and returns the result as a jsoup {@link Document}.
     * <p>Equivalent to {@code renderToDocument(htmlContent, jsToExecute, 0)}.</p>
     *
     * @param htmlContent the raw HTML to load
     * @param jsToExecute the JavaScript code to execute (may be {@code null} or empty)
     * @return the rendered content as a jsoup Document
     * @throws IOException if loading the HTML or conversion to jsoup fails
     */
    public static Document renderToDocument(String htmlContent, String jsToExecute) throws IOException {
        return renderToDocument(RenderConfig.defaultConfig(), htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content, executes a single JavaScript snippet,
     * waits for background JavaScript execution for the specified time (in milliseconds),
     * and returns the result as a jsoup {@link Document}.
     *
     * @param htmlContent               the raw HTML to load
     * @param jsToExecute               the JavaScript code to execute (may be {@code null} or empty)
     * @param waitForBackgroundJsMillis milliseconds to wait for background JS (0 = don't wait)
     * @return the rendered content as a jsoup Document
     * @throws IOException if loading the HTML or conversion to jsoup fails
     */
    public static Document renderToDocument(String htmlContent, String jsToExecute, int waitForBackgroundJsMillis) throws IOException {
        RenderConfig config = RenderConfig.builder()
                .waitForBackgroundJs(waitForBackgroundJsMillis, TimeUnit.MILLISECONDS)
                .build();
        return renderToDocument(config, htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content, executes multiple JavaScript snippets,
     * waits for background JavaScript execution for the specified time (in milliseconds),
     * and returns the result as a jsoup {@link Document}.
     *
     * @param htmlContent               the raw HTML to load
     * @param waitForBackgroundJsMillis milliseconds to wait for background JS (0 = don't wait)
     * @param jsToExecute               zero or more JavaScript snippets to execute
     * @return the rendered content as a jsoup Document
     * @throws IOException if loading the HTML or conversion to jsoup fails
     */
    public static Document renderToDocument(String htmlContent, int waitForBackgroundJsMillis, String... jsToExecute) throws IOException {
        RenderConfig config = RenderConfig.builder()
                .waitForBackgroundJs(waitForBackgroundJsMillis, TimeUnit.MILLISECONDS)
                .build();
        return renderToDocument(config, htmlContent, jsToExecute);
    }

    /**
     * Renders the given HTML content with full control via {@link RenderConfig},
     * executes the provided JavaScript snippets, and returns the result as a jsoup {@link Document}.
     *
     * @param config      the rendering configuration
     * @param htmlContent the raw HTML to load
     * @param jsSnippets  one or more JavaScript snippets to execute
     * @return the rendered content as a jsoup Document
     * @throws IOException if loading the HTML or conversion to jsoup fails
     */
    public static Document renderToDocument(RenderConfig config, String htmlContent, String... jsSnippets) throws IOException {
        HtmlPage page = executePage(config, htmlContent, jsSnippets);
        try {
            return JsoupUtil.parse(page.asXml());
        } catch (Exception e) {
            throw new IOException("Failed to convert HtmlUnit page to jsoup Document", e);
        }
    }

    // ---------- Core execution logic (private, reused) ----------

    /**
     * Loads the given HTML content into the current window of the (thread-local) WebClient,
     * executes all provided JavaScript snippets, optionally waits for background JavaScript tasks,
     * and returns the resulting {@link HtmlPage}.
     * <p>
     * This method handles the complete rendering lifecycle without converting the result.
     * It is used by both {@code render(...)} and {@code renderToDocument(...)} methods.
     * </p>
     *
     * @param config      the rendering configuration
     * @param htmlContent the raw HTML to load
     * @param jsSnippets  JavaScript snippets to execute (may be {@code null} or empty)
     * @return the fully processed HtmlPage
     * @throws IOException if loading the HTML fails
     */
    private static HtmlPage executePage(RenderConfig config, String htmlContent, String... jsSnippets) throws IOException {
        WebClient webClient = getWebClient(config);
        try {
            HtmlPage page = webClient.loadHtmlCodeIntoCurrentWindow(htmlContent);
         // 手动触发 window.onload 事件
            page.executeJavaScript("if (window.onload) window.onload();");
            
            if (jsSnippets != null) {
                for (String snippet : jsSnippets) {
                    if (snippet != null && !snippet.trim().isEmpty()) {
                        page.executeJavaScript(snippet);
                    }
                }
            }
            if (config.getWaitForBackgroundJsMs() > 0) {
                webClient.waitForBackgroundJavaScript(config.getWaitForBackgroundJsMs());
            }
            return page;
        } finally {
            if (config.isRecreateAfterUse()) {
                WEB_CLIENT_HOLDER.remove();
            }
        }
    }

    // ---------- WebClient management ----------

    /**
     * Retrieves the WebClient instance for the current thread (creating it if necessary),
     * and applies the given configuration to it.
     *
     * @param config the configuration to apply
     * @return the (already configured) WebClient for the current thread
     */
    private static WebClient getWebClient(RenderConfig config) {
        WebClient wc = WEB_CLIENT_HOLDER.get();
        applyConfig(wc, config);
        return wc;
    }

    /**
     * Applies the runtime configuration options to the given WebClient.
     * <p>
     * Note: {@code setJavaScriptTimeout} is called directly on {@link WebClient},
     * because {@link WebClientOptions} does not expose this setter (it exists as a
     * package-private method in older versions, but has been moved to WebClient level).
     * </p>
     *
     * @param wc     the WebClient to configure
     * @param config the configuration providing the values
     */
    private static void applyConfig(WebClient wc, RenderConfig config) {
        WebClientOptions options = wc.getOptions();
        options.setCssEnabled(config.isCssEnabled());
        options.setJavaScriptEnabled(config.isJavaScriptEnabled());
        options.setThrowExceptionOnScriptError(config.isThrowExceptionOnScriptError());
        options.setUseInsecureSSL(config.isUseInsecureSSL());
        options.setTimeout(config.getPageLoadTimeoutMs());
        if (config.getJavaScriptTimeoutMs() > 0) {
            wc.setJavaScriptTimeout(config.getJavaScriptTimeoutMs());
        }
    }

    /**
     * Creates a WebClient instance with the engine's default settings.
     *
     * @return a newly created, pre-configured WebClient
     */
    private static WebClient createDefaultWebClient() {
        WebClient wc = new WebClient();
        wc.getOptions().setCssEnabled(false);
        wc.getOptions().setUseInsecureSSL(true);
        wc.getOptions().setJavaScriptEnabled(true);
        wc.getOptions().setThrowExceptionOnScriptError(false);
        wc.getOptions().setTimeout(10000);
        wc.setJavaScriptTimeout(5000);
        return wc;
    }

    /**
     * Registers a JVM shutdown hook that closes the WebClient of the thread that executes the hook.
     * In most environments this helps to release resources gracefully.
     */
    private static synchronized void registerCleanupHook() {
        if (cleanupHookRegistered)
            return;
        Runtime.getRuntime().addShutdownHook(new Thread(HtmlRenderEngine::closeCurrentThreadClient));
        cleanupHookRegistered = true;
    }

    /**
     * Explicitly closes the WebClient associated with the current thread and removes it from the ThreadLocal.
     * This is useful when a thread is about to be terminated or when you want to force resource cleanup.
     * After calling this method, the next render call on the same thread will create a new WebClient.
     */
    public static void closeCurrentThreadClient() {
        WebClient wc = WEB_CLIENT_HOLDER.get();
        if (wc != null) {
            wc.close();
            WEB_CLIENT_HOLDER.remove();
        }
    }

    // ---------- Configuration class (Builder pattern) ----------

    /**
     * Configuration object for the rendering engine.
     * <p>
     * Use the {@link Builder} to create instances. All fields have sensible defaults.
     * </p>
     */
    public static final class RenderConfig {
        private final boolean cssEnabled;
        private final boolean javaScriptEnabled;
        private final boolean throwExceptionOnScriptError;
        private final boolean useInsecureSSL;
        private final int pageLoadTimeoutMs;
        private final int javaScriptTimeoutMs;
        private final int waitForBackgroundJsMs;
        private final boolean recreateAfterUse;

        private RenderConfig(Builder builder) {
            this.cssEnabled = builder.cssEnabled;
            this.javaScriptEnabled = builder.javaScriptEnabled;
            this.throwExceptionOnScriptError = builder.throwExceptionOnScriptError;
            this.useInsecureSSL = builder.useInsecureSSL;
            this.pageLoadTimeoutMs = builder.pageLoadTimeoutMs;
            this.javaScriptTimeoutMs = builder.javaScriptTimeoutMs;
            this.waitForBackgroundJsMs = builder.waitForBackgroundJsMs;
            this.recreateAfterUse = builder.recreateAfterUse;
        }

        /**
         * Creates a new builder instance.
         *
         * @return a fresh Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Returns a configuration with all default values.
         *
         * @return the default configuration
         */
        public static RenderConfig defaultConfig() {
            return builder().build();
        }

        // ----- Getters -----

        /** Whether CSS processing is enabled. */
        public boolean isCssEnabled() { return cssEnabled; }
        /** Whether JavaScript execution is enabled. */
        public boolean isJavaScriptEnabled() { return javaScriptEnabled; }
        /** Whether an exception should be thrown when a script error occurs. */
        public boolean isThrowExceptionOnScriptError() { return throwExceptionOnScriptError; }
        /** Whether to accept insecure SSL certificates (self-signed, expired). */
        public boolean isUseInsecureSSL() { return useInsecureSSL; }
        /** Page load timeout in milliseconds (network level). */
        public int getPageLoadTimeoutMs() { return pageLoadTimeoutMs; }
        /** JavaScript execution timeout in milliseconds (per script evaluation). */
        public int getJavaScriptTimeoutMs() { return javaScriptTimeoutMs; }
        /** Time to wait for background (asynchronous) JavaScript to finish, in milliseconds. */
        public int getWaitForBackgroundJsMs() { return waitForBackgroundJsMs; }
        /** Whether to discard the thread-local WebClient after each render call. */
        public boolean isRecreateAfterUse() { return recreateAfterUse; }

        /**
         * Builder for {@link RenderConfig}.
         * <p>Allows stepwise configuration of rendering parameters.</p>
         */
        public static final class Builder {
            private boolean cssEnabled = false;
            private boolean javaScriptEnabled = true;
            private boolean throwExceptionOnScriptError = false;
            private boolean useInsecureSSL = true;
            private int pageLoadTimeoutMs = 10000;
            private int javaScriptTimeoutMs = 5000;
            private int waitForBackgroundJsMs = 0;
            private boolean recreateAfterUse = false;

            /** Enables/disables CSS processing. Default: {@code false}. */
            public Builder cssEnabled(boolean enabled) { this.cssEnabled = enabled; return this; }
            /** Enables/disables JavaScript execution. Default: {@code true}. */
            public Builder javaScriptEnabled(boolean enabled) { this.javaScriptEnabled = enabled; return this; }
            /** Sets whether script errors should throw an exception. Default: {@code false}. */
            public Builder throwExceptionOnScriptError(boolean throwExc) { this.throwExceptionOnScriptError = throwExc; return this; }
            /** Enables insecure SSL (accept all certificates). Default: {@code true}. */
            public Builder useInsecureSSL(boolean use) { this.useInsecureSSL = use; return this; }
            /** Sets the page load timeout. Default: 10 seconds. */
            public Builder pageLoadTimeout(int timeout, TimeUnit unit) { this.pageLoadTimeoutMs = (int) unit.toMillis(timeout); return this; }
            /** Sets the JavaScript execution timeout. Default: 5 seconds. */
            public Builder javaScriptTimeout(int timeout, TimeUnit unit) { this.javaScriptTimeoutMs = (int) unit.toMillis(timeout); return this; }
            /** Sets the time to wait for background JavaScript tasks. Default: 0 (no wait). */
            public Builder waitForBackgroundJs(int timeout, TimeUnit unit) { this.waitForBackgroundJsMs = (int) unit.toMillis(timeout); return this; }
            /**
             * If {@code true}, the thread-local WebClient will be closed and removed after each render call.
             * This prevents state accumulation across requests but incurs a small performance penalty.
             * Default: {@code false}.
             */
            public Builder recreateAfterUse(boolean recreate) { this.recreateAfterUse = recreate; return this; }

            /** Builds the configuration instance. */
            public RenderConfig build() { return new RenderConfig(this); }
        }
    }
}