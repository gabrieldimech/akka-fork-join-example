package com.news.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.news.entities.NewsItem;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AggregatorTrigger extends AbstractBehavior<AggregatorTrigger.Command> {

    ActorRef<Aggregator.Command> aggregator;
    ActorContext<Command> context;

    private AggregatorTrigger(ActorContext<Command> context) {
        super(context);
        this.context = context;
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(AggregatorTrigger::new);
    }

    private AggregatedResult aggregateReplies(List<Object> replies) {
        List<NewsItem> quotes =
                replies.stream().map(x -> (NewsItemsFetcher.NewsItemsFetchResponse) x)
                        .flatMap(newsItemsFetchResponse -> newsItemsFetchResponse.newsItems.stream())
                        .collect(Collectors.toList());

        return new AggregatedResult(quotes);
    }

    @Override
    public Receive<AggregatorTrigger.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(NewsItemsRequest.class, this::onAggregatedQuotes)
                .build();
    }

    private AggregatorTrigger onAggregatedQuotes(NewsItemsRequest newsItemsRequest) {

        Consumer<ActorRef<Object>> sendRequests =
                replyTo -> {
                    for (String topic : newsItemsRequest.topics) {
                        ActorRef<NewsItemsFetcher.NewsItemsFetchRequest> request = ActorSystem.create(NewsItemsFetcher.create(),  topic);
                        request.tell(new NewsItemsFetcher.NewsItemsFetchRequest(topic, replyTo.narrow()));
                    }
                };

        aggregator = context.spawnAnonymous(
                Aggregator.create(
                        Object.class,
                        sendRequests,
                        newsItemsRequest.topics.size(),
                        newsItemsRequest.replyTo,
                        this::aggregateReplies,
                        Duration.ofSeconds(5)));
        return this;
    }

    public interface Command {
    }

    public static class NewsItemsRequest implements Command {
        public final List<String> topics;
        public final ActorRef<AggregatedResult> replyTo;

        public NewsItemsRequest(List<String> topics, ActorRef<AggregatedResult> replyTo) {
            this.topics = topics;
            this.replyTo = replyTo;
        }
    }

    public static class AggregatedResult implements Command {
        public final List<NewsItem> newsItems;

        public AggregatedResult(List<NewsItem> newsItems) {
            this.newsItems = newsItems;
        }
    }

}

