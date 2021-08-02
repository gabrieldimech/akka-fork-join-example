package com.news.services;

import com.news.entities.NewsItem;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleNewsService {

    public List<NewsItem> getNewsItemsByTopic(String topic) throws IOException, FeedException {
        List<NewsItem> res = new ArrayList<>();
        URL feedSource = new URL("https://news.google.com/rss/search?q=" + topic + "+when:1h&ceid=US:en&hl=en-US&gl=US");
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedSource));
        List<SyndEntryImpl> entries = (List<com.sun.syndication.feed.synd.SyndEntryImpl>) feed.getEntries();

        for (com.sun.syndication.feed.synd.SyndEntryImpl entry : entries) {
            res.add(new NewsItem(entry.getTitle(),entry.getLink(), topic, topic, entry.getPublishedDate()));
        }
        return res;
    }
}
