package com.openhtmltopdf.jhtml.util;

import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;

/**
 * Utility class for parsing HTML strings into Jsoup Document objects with specific output settings.
 */
public class JsoupUtil {

    // Global configuration: request timeout in milliseconds
    private static final int TIME_OUT = 7777777;
    // Simulated browser User-Agent
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final SSLContext TRUST_ALL_SSL_CONTEXT;
    static {
        try {
            // Define a TrustManager that trusts all server certificates
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};

            // Initialize SSLContext instance
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            TRUST_ALL_SSL_CONTEXT = sslContext;
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize SSLContext for ignoring certificates.", e);
        }
    }

    /**
     * Parses HTML string into a Jsoup Document with specific output settings.
     * @param html
     * @return
     */
    public static Document parse(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings().syntax(Syntax.xml);
        doc.outputSettings().prettyPrint(false);
        return doc;
    }

    /**
     * Checks whether the URL uses HTTP or HTTPS protocol (case-insensitive).
     * Examples: "http://", "HTTP://", "Http://", "https://", "HTTPS://" all return true.
     * @param url URL to check
     * @return true if protocol is HTTP or HTTPS (ignoring case), false otherwise
     */
    public static boolean isHttp(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
    }

    /**
     * Core method: validates HTTP/HTTPS protocol (case-insensitive), sends request and returns InputStream.
     * @param url Target URL (any case variation of http/https is accepted)
     * @return Request input stream
     * @throws Exception if protocol is invalid or request fails
     */
    public static InputStream getInputStream(String url) throws Exception {
        // Build optimized request with Jsoup
        Connection connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)           // Browser User-Agent
                .timeout(TIME_OUT)               // Timeout
                .followRedirects(true)           // Automatically follow redirects
                .ignoreContentType(true)         // Ignore content type (supports images, files, web pages, etc.)
                .ignoreHttpErrors(true)          // Ignore HTTP errors like 404/500
                .header("Accept", "*/*")         // Accept all types
                .header("Connection", "close");  // Close connection after use

        // Apply custom SSL context only for HTTPS (case-insensitive check)
        if (url.toLowerCase().startsWith("https://")) {
            connection.sslContext(TRUST_ALL_SSL_CONTEXT);
        }

        // Execute request and return stream
        return connection.execute().bodyStream();
    }
}