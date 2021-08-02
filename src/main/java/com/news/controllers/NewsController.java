package com.news.controllers;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.dto.NewsItemsRequestDto;
import com.news.actors.AggregatorTrigger;
import com.news.entities.NewsItem;
import com.news.services.GoogleNewsService;
import com.sun.syndication.io.FeedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

@RestController
public class NewsController {

    private final GoogleNewsService googleNewsService;

    @Autowired
    NewsController(GoogleNewsService googleNewsService) {
        this.googleNewsService = googleNewsService;
    }

    @GetMapping("/news/{topic}")
    public ResponseEntity<List<NewsItem>> getNewsItems(@PathVariable String topic)
            throws FeedException, IOException {
        List<NewsItem> newsItems = googleNewsService.getNewsItemsByTopic(topic);

        return ResponseEntity.ok(newsItems);
    }

    @PostMapping("/news")
    public ResponseEntity<Map<String, List<NewsItem>>> getNewsItems(@RequestBody NewsItemsRequestDto newsItemsRequestDto){

        final ActorSystem<AggregatorTrigger.Command> system =
                ActorSystem.create(AggregatorTrigger.create(), "news-aggregator");

        Duration d = Duration.ofSeconds(100);

        ActorRef<AggregatorTrigger.Command> aggregatorTriggerActorRef = ActorSystem.create(AggregatorTrigger.create(), "aggregator-trigger");

        CompletionStage<AggregatorTrigger.AggregatedResult> aggregator = AskPattern.ask(aggregatorTriggerActorRef, ref -> new AggregatorTrigger.NewsItemsRequest(newsItemsRequestDto.getTopics(), ref), d, system.scheduler());

        AggregatorTrigger.AggregatedResult aggregatedResult = (AggregatorTrigger.AggregatedResult) ((FuturesConvertersImpl.CF) aggregator).get(100, TimeUnit.SECONDS);

        Map<String, List<NewsItem>> result = new HashMap<>();

        for (NewsItem newsItem : aggregatedResult.newsItems) {
            if (!result.containsKey(newsItem.getTopic()))
                result.computeIfAbsent(newsItem.getTopic(), x -> new ArrayList<>()).add(newsItem);
            else
                result.computeIfPresent(newsItem.getTopic(), (x, y) -> y).add(newsItem);
        }

        return ResponseEntity.ok(result);

    }

}

