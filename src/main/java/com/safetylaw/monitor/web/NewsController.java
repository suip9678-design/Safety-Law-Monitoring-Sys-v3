package com.safetylaw.monitor.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.config.AppProperties;
import com.safetylaw.monitor.domain.NewsItem;
import com.safetylaw.monitor.dto.NewsItemResponse;
import com.safetylaw.monitor.service.NewsService;
import com.safetylaw.monitor.service.SettingsService;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private static final int MAX_LIMIT = 200;

    private final NewsService newsService;
    private final SettingsService settings;
    private final AppProperties properties;

    public NewsController(NewsService newsService, SettingsService settings, AppProperties properties) {
        this.newsService = newsService;
        this.settings = settings;
        this.properties = properties;
    }

    @GetMapping
    public List<NewsItemResponse> list(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "limit", defaultValue = "40") int limit) {
        if (!settings.getBoolean(SettingsService.NEWS_TICKER_ENABLED)) {
            return List.of();
        }
        int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<NewsItem> items = (category == null || category.isBlank())
                ? newsService.findRecent(capped)
                : newsService.findByCategory(category, capped);

        List<NewsItemResponse> out = new ArrayList<>(items.size());
        for (NewsItem item : items) {
            out.add(NewsItemResponse.of(item));
        }
        return out;
    }

    @PostMapping("/sync")
    public Map<String, Integer> syncNow() {
        int maxItems = settings.getInt(SettingsService.NEWS_MAX_ITEMS_PER_CATEGORY,
                properties.news().maxItemsPerCategory());
        int added = newsService.syncNews(newsService.configuredSources(), maxItems);
        return Map.of("added", added);
    }
}
