package org.bitmagic.ifeed.util;

import org.springframework.util.Assert;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author yangrd
 * @date 2025/10/29
 **/
public class UrlChecker {

    public static boolean isValidUrl(String str) {
        try {
            Assert.notNull(str, "url not null");
            URI uri = new URI(str);
            return "http".equalsIgnoreCase(uri.getScheme()) ||
                    "https".equalsIgnoreCase(uri.getScheme());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        String testUrl = "http://www.example.com";
        System.out.println("Is valid URL? " + isValidUrl(testUrl));
        String testUrl2 = "file://www.example.com";
        System.out.println("Is valid URL? " + isValidUrl(testUrl2));
    }
}
