package com.edunex.backend.service.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Reads the text of a public web page or PDF. Defends against server-side request forgery:
 * only http(s) on default ports, only public addresses, and every redirect is checked again.
 */
@Component
public class LinkFetcher {

    public record Fetched(String title, String text) {
    }

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_CHARS = 400_000;
    private static final int MAX_REDIRECTS = 3;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Fetched fetch(String rawUrl) throws IOException {
        URI uri = validate(rawUrl);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpResponse<InputStream> response;
            try {
                response = client.send(HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "EduNexBot/1.0 (+institute knowledge base)")
                        .header("Accept", "text/html,application/pdf,text/plain;q=0.9,*/*;q=0.1")
                        .GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            }
            int code = response.statusCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = response.headers().firstValue("location")
                        .orElseThrow(() -> new IOException("redirect without location"));
                response.body().close();
                uri = validate(uri.resolve(location).toString());
                continue;
            }
            if (code != 200) {
                response.body().close();
                throw new IOException("the page answered HTTP " + code);
            }
            byte[] bytes;
            try (InputStream in = response.body()) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) {
                throw new IOException("the page is too large (10 MB maximum)");
            }
            String type = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
            return parse(uri, type, bytes);
        }
        throw new IOException("too many redirects");
    }

    private Fetched parse(URI uri, String type, byte[] bytes) throws IOException {
        boolean pdf = type.contains("pdf") || (bytes.length > 4 && new String(bytes, 0, 4, StandardCharsets.ISO_8859_1).equals("%PDF"));
        if (pdf) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                String name = uri.getPath() == null ? "" : uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
                return new Fetched(name, cap(new PDFTextStripper().getText(doc)));
            }
        }
        String body = new String(bytes, StandardCharsets.UTF_8);
        if (type.contains("html") || body.stripLeading().startsWith("<")) {
            Document doc = Jsoup.parse(body, uri.toString());
            doc.select("script,style,noscript,nav,footer,header,aside,form,iframe,svg,button").remove();
            StringBuilder sb = new StringBuilder();
            String last = "";
            for (Element e : doc.select("h1,h2,h3,h4,p,li,tr,pre,blockquote,dd,dt")) {
                String line = e.text().trim();
                if (!line.isEmpty() && !line.equals(last)) {
                    sb.append(line).append('\n');
                    last = line;
                }
            }
            if (sb.length() < 200 && doc.body() != null) {
                sb = new StringBuilder(doc.body().text());
            }
            return new Fetched(doc.title(), cap(sb.toString()));
        }
        if (type.startsWith("text/")) {
            return new Fetched("", cap(body));
        }
        throw new IOException("unsupported content type: " + type);
    }

    private static String cap(String s) {
        return s.length() > MAX_CHARS ? s.substring(0, MAX_CHARS) : s;
    }

    /** Throws IllegalArgumentException when the address must not be fetched. */
    public static URI validate(String raw) {
        URI uri;
        try {
            uri = new URI(raw == null ? "" : raw.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http and https links are allowed");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid URL");
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw new IllegalArgumentException("Only the default web ports are allowed");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (!isPublic(address)) {
                    throw new IllegalArgumentException("This address is not allowed");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host");
        }
        return uri;
    }

    public static boolean isPublic(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return false;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int x = b[0] & 0xFF;
            int y = b[1] & 0xFF;
            if (x == 0 || (x == 100 && y >= 64 && y <= 127)) {
                return false;
            }
        }
        return !(b.length == 16 && (b[0] & 0xFE) == 0xFC);
    }
}