package com.edunex.backend.service.ai;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.util.Timeout;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Reads the text of a public web page or PDF. Defends against server-side request forgery:
 * only http(s) on default ports, only public addresses (checked by the resolver used to connect,
 * so the address cannot change between the check and the connection), every redirect checked again.
 */
@Component
public class LinkFetcher {

    public record Fetched(String title, String text) {
    }

    private record Page(int code, String location, String type, byte[] bytes) {
    }

    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_CHARS = 400_000;
    private static final int MAX_REDIRECTS = 3;

    /** The addresses that are checked are exactly the ones the connection will use. */
    private static final class PublicOnlyDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            InetAddress[] all = InetAddress.getAllByName(host);
            for (InetAddress a : all) {
                if (!isPublic(a)) {
                    throw new UnknownHostException("blocked address");
                }
            }
            return all;
        }

        @Override
        public String resolveCanonicalHostname(String host) throws UnknownHostException {
            InetAddress[] all = resolve(host);
            return all.length == 0 ? host : all[0].getCanonicalHostName();
        }
    }

    private final CloseableHttpClient client = HttpClients.custom()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                    .setDnsResolver(new PublicOnlyDnsResolver())
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setConnectTimeout(Timeout.ofSeconds(10))
                            .setSocketTimeout(Timeout.ofSeconds(20))
                            .build())
                    .build())
            .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(20)).build())
            .disableRedirectHandling()
            .disableCookieManagement()
            .disableAutomaticRetries()
            .build();

    public Fetched fetch(String rawUrl) throws IOException {
        URI uri = validate(rawUrl);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Page page = get(uri);
            if (List.of(301, 302, 303, 307, 308).contains(page.code())) {
                if (page.location() == null) {
                    throw new IOException("redirect without location");
                }
                uri = validate(uri.resolve(page.location()).toString());
                continue;
            }
            if (page.code() != 200) {
                throw new IOException("the page answered HTTP " + page.code());
            }
            if (page.bytes().length > MAX_BYTES) {
                throw new IOException("the page is too large (10 MB maximum)");
            }
            return parse(uri, page.type(), page.bytes());
        }
        throw new IOException("too many redirects");
    }

    private Page get(URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader("User-Agent", "EduNexBot/1.0 (+institute knowledge base)");
        request.setHeader("Accept", "text/html,application/pdf,text/plain;q=0.9,*/*;q=0.1");
        return client.execute(request, response -> {
            int code = response.getCode();
            Header location = response.getFirstHeader("Location");
            Header type = response.getFirstHeader("Content-Type");
            byte[] bytes = new byte[0];
            if (code == 200 && response.getEntity() != null) {
                try (InputStream in = response.getEntity().getContent()) {
                    bytes = in.readNBytes(MAX_BYTES + 1);
                }
            }
            return new Page(code, location == null ? null : location.getValue(),
                    type == null ? "" : type.getValue().toLowerCase(Locale.ROOT), bytes);
        });
    }

    private Fetched parse(URI uri, String type, byte[] bytes) throws IOException {
        boolean pdf = type.contains("pdf")
                || (bytes.length > 4 && new String(bytes, 0, 4, StandardCharsets.ISO_8859_1).equals("%PDF"));
        if (pdf) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                if (doc.getNumberOfPages() > 400) {
                    throw new IOException("too many pages");
                }
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
            int z = b[2] & 0xFF;
            return !(x == 0 || x >= 240
                    || (x == 100 && y >= 64 && y <= 127)
                    || (x == 192 && y == 0 && z <= 2)
                    || (x == 198 && (y == 18 || y == 19))
                    || (x == 198 && y == 51 && z == 100)
                    || (x == 203 && y == 0 && z == 113));
        }
        int first = ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
        boolean documentation = first == 0x2001 && (b[2] & 0xFF) == 0x0D && (b[3] & 0xFF) == 0xB8;
        return !((b[0] & 0xFE) == 0xFC || documentation || first == 0x0064 || first == 0x2002);
    }
}