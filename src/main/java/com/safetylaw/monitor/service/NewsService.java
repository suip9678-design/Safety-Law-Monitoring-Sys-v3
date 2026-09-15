package com.safetylaw.monitor.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Element;

import com.safetylaw.monitor.domain.NewsItem;
import com.safetylaw.monitor.lawapi.DemoFixtures;
import com.safetylaw.monitor.mapper.NewsItemMapper;
import com.safetylaw.monitor.support.Times;
import com.safetylaw.monitor.support.Xml;
import com.safetylaw.monitor.support.XmlParseException;

/**
 * 안전보건 뉴스를 RSS/Atom 으로 가져와 대시보드 게시판에 채운다.
 *
 * <p>표준 RSS 2.0({@code <item>})과 Atom({@code <entry>})을 모두 읽으므로,
 * 형식만 맞으면 어떤 주소든 설정 화면에서 바꿔 끼울 수 있다.
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";

    /** 일부 뉴스 사이트는 브라우저가 아닌 요청을 막으므로 일반적인 표식을 붙인다. */
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; SafetyLawMonitoringSys/1.0; NewsBoard)";

    public static final Map<String, String> CATEGORY_LABELS = Map.of(
            "moel", "고용노동부",
            "kosha", "안전보건공단",
            "accident", "중대재해 뉴스");

    /** 컬럼 길이를 넘는 값이 들어오면 저장 자체가 실패하므로 미리 자른다. */
    private static final int MAX_TITLE = 512;
    private static final int MAX_LINK = 1024;
    private static final int MAX_GUID = 512;

    private final RestClient restClient;
    private final NewsItemMapper newsMapper;
    private final SettingsService settings;

    public NewsService(@Qualifier("plainRestClient") RestClient restClient,
                       NewsItemMapper newsMapper,
                       SettingsService settings) {
        this.restClient = restClient;
        this.newsMapper = newsMapper;
        this.settings = settings;
    }

    /** 피드 한 건. */
    public record FeedEntry(String title, String link, String guid, LocalDateTime publishedAt) {
    }

    /** 수집 대상 한 갈래. */
    public record NewsSource(String category, String sourceName, String url) {
    }

    public List<NewsSource> configuredSources() {
        Map<String, String> values = settings.getAll();
        return List.of(
                new NewsSource("moel", CATEGORY_LABELS.get("moel"),
                        values.getOrDefault(SettingsService.NEWS_SOURCE_MOEL_URL, "")),
                new NewsSource("kosha", CATEGORY_LABELS.get("kosha"),
                        values.getOrDefault(SettingsService.NEWS_SOURCE_KOSHA_URL, "")),
                new NewsSource("accident", CATEGORY_LABELS.get("accident"),
                        values.getOrDefault(SettingsService.NEWS_SOURCE_ACCIDENT_URL, "")));
    }

    /**
     * 피드를 읽는다.
     *
     * <p>실패해도 예외를 던지지 않고 빈 목록을 돌려준다. 뉴스 피드 하나가
     * 막혔다고 법령 동기화 같은 핵심 기능이 멈추면 안 된다.
     */
    public List<FeedEntry> fetchFeed(String url) {
        if (url == null || url.isBlank()) {
            return List.of();
        }

        byte[] body;
        try {
            body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("뉴스 피드를 가져오지 못했습니다 ({}): {}", url, e.getMessage());
            return List.of();
        }
        if (body == null) {
            return List.of();
        }

        Element root;
        try {
            root = Xml.parse(body).getDocumentElement();
        } catch (XmlParseException e) {
            log.warn("뉴스 피드를 해석하지 못했습니다 ({}): {}", url, e.getMessage());
            return List.of();
        }

        List<FeedEntry> entries = new ArrayList<>();
        for (Element item : Xml.descendantsAndSelf(root, List.of("item"))) {
            String title = Xml.childText(item, "title");
            String link = Xml.childText(item, "link");
            if (title == null || link == null) {
                continue;
            }
            String guid = Xml.childText(item, "guid");
            entries.add(new FeedEntry(title, link, guid != null ? guid : link,
                    parsePublished(Xml.childText(item, "pubDate"))));
        }

        if (entries.isEmpty()) {
            entries.addAll(parseAtom(root));
        }
        return entries;
    }

    private List<FeedEntry> parseAtom(Element root) {
        List<FeedEntry> entries = new ArrayList<>();
        // 이름공간을 쓰지 않고 파싱하므로 태그 이름이 접두어째로 들어올 수
        // 있다. 접두어를 무시하고 지역 이름만으로 맞춘다.
        for (Element entry : localName(root, "entry")) {
            String title = firstLocalText(entry, "title");
            String link = null;
            for (Element linkEl : localName(entry, "link")) {
                String href = linkEl.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    link = href;
                    break;
                }
            }
            if (title == null || link == null) {
                continue;
            }
            String id = firstLocalText(entry, "id");
            String published = firstLocalText(entry, "updated");
            if (published == null) {
                published = firstLocalText(entry, "published");
            }
            entries.add(new FeedEntry(title, link, id != null ? id : link, parsePublished(published)));
        }
        return entries;
    }

    private List<Element> localName(Element root, String name) {
        List<Element> out = new ArrayList<>();
        for (Element el : Xml.descendantsAndSelf(root,
                List.of(name, "atom:" + name, "{" + ATOM_NS + "}" + name))) {
            out.add(el);
        }
        return out;
    }

    private String firstLocalText(Element parent, String name) {
        for (Element el : localName(parent, name)) {
            String text = Xml.trimmed(el.getTextContent());
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    /**
     * 각 갈래를 가져와 새 항목만 저장한다.
     *
     * <p>실제 피드가 비어 있으면(주소 미설정, 사내망 차단 등) 화면이 비어
     * 보이지 않도록 예시 항목으로 채운다. 반대로 실제 항목이 들어오기
     * 시작하면 그 갈래의 예시 항목은 정리한다.
     *
     * @return 새로 저장한 건수
     */
    public int syncNews(List<NewsSource> sources, int maxItemsPerCategory) {
        int added = 0;
        LocalDateTime now = Times.nowUtc();

        for (NewsSource source : sources) {
            List<FeedEntry> fetched = fetchFeed(source.url());
            boolean isDemo = fetched.isEmpty();

            if (isDemo) {
                fetched = new ArrayList<>();
                for (DemoFixtures.DemoNews demo : DemoFixtures.news(source.category())) {
                    fetched.add(new FeedEntry(demo.title(), demo.link(), demo.guid(), null));
                }
            } else {
                newsMapper.deleteDemoByCategory(source.category());
            }

            for (FeedEntry entry : fetched) {
                String guid = cut(entry.guid(), MAX_GUID);
                if (newsMapper.countByCategoryAndGuid(source.category(), guid) > 0) {
                    continue;
                }
                NewsItem item = new NewsItem();
                item.setCategory(source.category());
                item.setSourceName(source.sourceName());
                item.setTitle(cut(entry.title(), MAX_TITLE));
                item.setLink(cut(entry.link(), MAX_LINK));
                item.setGuid(guid);
                item.setPublishedAt(entry.publishedAt());
                item.setFetchedAt(now);
                item.setIsDemo(isDemo);
                newsMapper.insert(item);
                added++;
            }
        }

        for (NewsSource source : sources) {
            newsMapper.deleteBeyondLimit(source.category(), maxItemsPerCategory);
        }
        return added;
    }

    public List<NewsItem> findByCategory(String category, int limit) {
        return newsMapper.findByCategory(category, limit);
    }

    public List<NewsItem> findRecent(int limit) {
        return newsMapper.findRecent(limit);
    }

    /**
     * 발행 시각을 UTC 로 읽는다.
     *
     * <p>RSS 2.0 은 RFC 822 계열, Atom 은 ISO 8601 을 쓰므로 둘 다 시도한다.
     * 어느 쪽으로도 읽히지 않으면 null 을 돌려주고, 목록에서는 발행일 없는
     * 항목으로 뒤에 놓인다.
     */
    private LocalDateTime parsePublished(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            return ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 다음 형식으로 넘어간다.
        }
        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 다음 형식으로 넘어간다.
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // 다음 형식으로 넘어간다.
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            log.debug("뉴스 발행 시각을 읽지 못했습니다: {}", text);
            return null;
        }
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
