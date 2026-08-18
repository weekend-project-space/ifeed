package org.bitmagic.ifeed;

import org.bitmagic.ifeed.config.Const;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

@SpringBootTest
class IfeedApplicationTests {

    @Autowired
    private FeedRepository feedRepository;

	@Test
	void contextLoads() {
	}

    @Test
    void fixIcon(){
        feedRepository.saveAll(feedRepository.findAll().stream().peek(feed -> feed.setIcon(Const.FAVICON_TEMPLATE.formatted(extractHost(feed.getSiteUrl())))).collect(Collectors.toList()));
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            var uri = new URI(url.trim());
            if (StringUtils.hasText(uri.getHost())) {
                return uri.getHost();
            }
            var path = uri.getPath();
            if (StringUtils.hasText(path)) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            // fallback to raw url below
        }
        return url;
    }

}
