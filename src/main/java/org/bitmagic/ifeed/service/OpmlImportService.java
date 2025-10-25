package org.bitmagic.ifeed.service;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.api.request.OpmlImportConfirmRequest;
import org.bitmagic.ifeed.api.response.OpmlImportConfirmResponse;
import org.bitmagic.ifeed.api.response.OpmlImportSkippedResponse;
import org.bitmagic.ifeed.api.response.OpmlPreviewFeedResponse;
import org.bitmagic.ifeed.api.response.OpmlPreviewResponse;
import org.bitmagic.ifeed.domain.entity.Feed;
import org.bitmagic.ifeed.domain.entity.User;
import org.bitmagic.ifeed.domain.repository.FeedRepository;
import org.bitmagic.ifeed.domain.repository.UserSubscriptionRepository;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OpmlImportService {

    private static final String FAVICON_TEMPLATE = "https://favicon.im/%s";
    private static final int USER_SUBSCRIPTION_QUOTA = 500;
    private static final Pattern INVALID_ENTITY_PATTERN = Pattern.compile("&(?!#?\\w+;)");

    private final FeedRepository feedRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionService subscriptionService;

    public OpmlPreviewResponse generatePreview(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OPML file is required");
        }

        Document document = parseOpmlDocument(file);
        NodeList outlines = document.getElementsByTagName("outline");

        if (outlines == null || outlines.getLength() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No subscriptions found in OPML file");
        }

        List<OpmlPreviewFeedResponse> feeds = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        for (int i = 0; i < outlines.getLength(); i++) {
            Node node = outlines.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element outline = (Element) node;
            String feedUrl = resolveAttribute(outline, "xmlUrl");
            if (!StringUtils.hasText(feedUrl)) {
                feedUrl = resolveAttribute(outline, "url");
            }

            if (!StringUtils.hasText(feedUrl)) {
                String label = resolveAttribute(outline, "title");
                if (!StringUtils.hasText(label)) {
                    label = resolveAttribute(outline, "text");
                }
                warnings.add("Skipped outline without feedUrl" + (StringUtils.hasText(label) ? ": " + label : ""));
                continue;
            }

            feedUrl = feedUrl.trim();
            if (!seenUrls.add(feedUrl)) {
                warnings.add("Duplicate feed skipped: " + feedUrl);
                continue;
            }

            String siteUrl = resolveAttribute(outline, "htmlUrl");
            String title = resolveTitle(outline);

            Optional<Feed> existingFeed = feedRepository.findByUrl(feedUrl);
            boolean alreadySubscribed = false;
            List<String> errors = List.of();

            if (existingFeed.isPresent()) {
                Feed feed = existingFeed.get();
                alreadySubscribed = userSubscriptionRepository.existsByUser_IdAndFeedAndActiveTrue(user.getId(), feed);

                if (!StringUtils.hasText(siteUrl)) {
                    siteUrl = feed.getSiteUrl();
                }
                if (!StringUtils.hasText(title)) {
                    title = feed.getTitle();
                }
            }

            siteUrl = fallbackSiteUrl(siteUrl, feedUrl);
            title = resolveFeedTitle(title, siteUrl, feedUrl);
            String avatar = resolveAvatar(siteUrl, feedUrl);

            feeds.add(new OpmlPreviewFeedResponse(
                    feedUrl,
                    title,
                    siteUrl,
                    avatar,
                    alreadySubscribed,
                    errors
            ));
        }

        if (feeds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No valid subscriptions found in OPML file");
        }
        long usedQuota = userSubscriptionRepository.countByUserAndActiveTrue(user);
        int remainingQuota = (int) Math.max(0, USER_SUBSCRIPTION_QUOTA - usedQuota);

        return new OpmlPreviewResponse(feeds, warnings.isEmpty() ? List.of() : List.copyOf(warnings), remainingQuota);
    }

    public OpmlImportConfirmResponse confirm(User user, OpmlImportConfirmRequest request) {
        if (request == null || request.feeds() == null || request.feeds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feed list cannot be empty");
        }

        long usedQuota = userSubscriptionRepository.countByUserAndActiveTrue(user);
        int remainingQuota = (int) Math.max(0, USER_SUBSCRIPTION_QUOTA - usedQuota);
        int importedCount = 0;
        List<OpmlImportSkippedResponse> skipped = new ArrayList<>();
        Set<String> processed = new LinkedHashSet<>();

        for (OpmlImportConfirmRequest.OpmlImportConfirmRequestItem item : request.feeds()) {
            if (item == null || !Boolean.TRUE.equals(item.selected())) {
                continue;
            }

            String feedUrl = normalize(item.feedUrl());
            if (!StringUtils.hasText(feedUrl)) {
                skipped.add(new OpmlImportSkippedResponse(null, SkipReason.INVALID_FEED.name()));
                continue;
            }

            if (!processed.add(feedUrl)) {
                skipped.add(new OpmlImportSkippedResponse(feedUrl, SkipReason.DUPLICATE.name()));
                continue;
            }

            String siteUrl = fallbackSiteUrl(item.siteUrl(), feedUrl);
            String title = resolveFeedTitle(item.title(), siteUrl, feedUrl);

            Feed existingFeed = feedRepository.findByUrl(feedUrl).orElse(null);
            if (existingFeed != null && userSubscriptionRepository.existsByUser_IdAndFeedAndActiveTrue(user.getId(), existingFeed)) {
                skipped.add(new OpmlImportSkippedResponse(feedUrl, SkipReason.ALREADY_SUBSCRIBED.name()));
                continue;
            }

            if (remainingQuota <= 0) {
                skipped.add(new OpmlImportSkippedResponse(feedUrl, SkipReason.QUOTA_EXCEEDED.name()));
                continue;
            }

            try {
                subscriptionService.subscribe(user, new org.bitmagic.ifeed.api.request.SubscriptionRequest(feedUrl, siteUrl, title));
                importedCount++;
                remainingQuota--;
            } catch (ApiException apiException) {
                if (apiException.getStatus() == HttpStatus.CONFLICT) {
                    skipped.add(new OpmlImportSkippedResponse(feedUrl, SkipReason.ALREADY_SUBSCRIBED.name()));
                } else if (apiException.getStatus() == HttpStatus.BAD_REQUEST) {
                    skipped.add(new OpmlImportSkippedResponse(feedUrl, SkipReason.INVALID_FEED.name()));
                } else {
                    skipped.add(new OpmlImportSkippedResponse(feedUrl, SkipReason.FAILED.name()));
                }
            }
        }

        return new OpmlImportConfirmResponse(importedCount, skipped, "OPML import completed.");
    }

    private Document parseOpmlDocument(MultipartFile file) {
        try {
            var rawContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var sanitizedContent = sanitizeXml(rawContent);
            try (var inputStream = new ByteArrayInputStream(sanitizedContent.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                factory.setExpandEntityReferences(false);

                disableFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
                disableFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
                disableFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
                disableFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(inputStream);
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read OPML file", ex);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OPML file", ex);
        }
    }

    private String sanitizeXml(String xml) {
        if (xml == null || xml.isEmpty()) {
            return "";
        }
        return INVALID_ENTITY_PATTERN.matcher(xml).replaceAll("&amp;");
    }

    private void disableFeature(DocumentBuilderFactory factory, String feature, boolean value) throws ParserConfigurationException {
        factory.setFeature(feature, value);
    }

    private String resolveAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveTitle(Element outline) {
        String title = resolveAttribute(outline, "title");
        if (!StringUtils.hasText(title)) {
            title = resolveAttribute(outline, "text");
        }
        return title;
    }

    private String fallbackSiteUrl(String siteUrl, String feedUrl) {
        if (StringUtils.hasText(siteUrl)) {
            return siteUrl.trim();
        }
        return feedUrl;
    }

    private String resolveFeedTitle(String requestedTitle, String siteUrl, String feedUrl) {
        if (StringUtils.hasText(requestedTitle)) {
            return requestedTitle.trim();
        }

        String host = extractHost(siteUrl);
        if (StringUtils.hasText(host)) {
            return host;
        }

        host = extractHost(feedUrl);
        if (StringUtils.hasText(host)) {
            return host;
        }

        return "未命名订阅";
    }

    private String resolveAvatar(String siteUrl, String feedUrl) {
        String host = extractHost(siteUrl);
        if (!StringUtils.hasText(host)) {
            host = extractHost(feedUrl);
        }
        if (!StringUtils.hasText(host)) {
            return null;
        }
        return FAVICON_TEMPLATE.formatted(host);
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            if (StringUtils.hasText(uri.getHost())) {
                return uri.getHost();
            }
            String path = uri.getPath();
            if (StringUtils.hasText(path)) {
                return path;
            }
        } catch (URISyntaxException ignored) {
            return url;
        }
        return url;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private enum SkipReason {
        ALREADY_SUBSCRIBED,
        INVALID_FEED,
        DUPLICATE,
        FAILED,
        QUOTA_EXCEEDED
    }
}
