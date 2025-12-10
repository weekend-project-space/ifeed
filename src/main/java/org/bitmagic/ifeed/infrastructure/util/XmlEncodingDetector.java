package org.bitmagic.ifeed.infrastructure.util;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmlEncodingDetector {

    private static final Pattern ENCODING_PATTERN = Pattern.compile("encoding=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final int HEADER_SIZE = 200;

    private XmlEncodingDetector() {}

    public static String toString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        // 1. 安全读取 XML 头部
        int len = Math.min(bytes.length, HEADER_SIZE);
        String header = new String(bytes, 0, len, StandardCharsets.ISO_8859_1);

        // 2. 提取 encoding
        Matcher m = ENCODING_PATTERN.matcher(header);
        String encoding = m.find() ? m.group(1) : "UTF-8";

        // 3. 规范化编码
        Charset charset = switch (encoding.toUpperCase()) {
            case "GB2312", "GBK", "GB18030" -> Charset.forName("GBK");
            case "SHIFT_JIS", "SJIS" -> Charset.forName("Shift_JIS");
            case "UTF-8", "US-ASCII", "ISO-8859-1", "LATIN1" -> StandardCharsets.UTF_8;
            default -> {
                try {
                    yield Charset.forName(encoding);
                } catch (Exception e) {
                    yield StandardCharsets.UTF_8;
                }
            }
        };

        return new String(bytes, charset);
    }
}