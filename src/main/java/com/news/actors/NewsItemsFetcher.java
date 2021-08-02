package com.news.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.news.entities.NewsItem;
import com.news.services.GoogleNewsService;
import com.sun.syndication.io.FeedException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class NewsItemsFetcher extends AbstractBehavior<NewsItemsFetcher.NewsItemsFetchRequest> {

    private final GoogleNewsService googleNewsService;

    public NewsItemsFetcher(ActorContext<NewsItemsFetchRequest> context) {
        super(context);
        this.googleNewsService = new GoogleNewsService();
    }

    public static Behavior<NewsItemsFetchRequest> create() {
        return Behaviors.setup(NewsItemsFetcher::new);
    }

    @Override
    public Receive<NewsItemsFetchRequest> createReceive() {
        return newReceiveBuilder().onMessage(NewsItemsFetchRequest.class, this::onFetch).build();
    }

    private Behavior<NewsItemsFetchRequest> onFetch(NewsItemsFetchRequest command) throws FeedException, IOException {
        getContext().getLog().info("Fetching news items for {}!", command.topic);

        List<NewsItem> newsItems = googleNewsService.getNewsItemsByTopic(command.topic);
        command.replyTo.tell(new NewsItemsFetchResponse(command.topic, getContext().getSelf(), newsItems));
        return this;
    }

    public static final class NewsItemsFetchRequest {
        public final String topic;
        public final ActorRef<NewsItemsFetchResponse> replyTo;

        public NewsItemsFetchRequest(String topic, ActorRef<NewsItemsFetchResponse> replyTo) {
            this.topic = topic;
            this.replyTo = replyTo;
        }
    }

    public static final class NewsItemsFetchResponse {
        public final String topic;
        public final ActorRef<NewsItemsFetchRequest> from;
        public final List<NewsItem> newsItems;

        public NewsItemsFetchResponse(String whom, ActorRef<NewsItemsFetchRequest> from ,List<NewsItem> newsItems) {
            this.topic = whom;
            this.from = from;
            this.newsItems = newsItems;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NewsItemsFetchResponse newsItemsFetchResponse = (NewsItemsFetchResponse) o;
            return Objects.equals(topic, newsItemsFetchResponse.topic) &&
                    Objects.equals(from, newsItemsFetchResponse.from);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topic, from);
        }

        @Override
        public String toString() {
            return "NewsItemsFetchResponse{" +
                    "whom='" + topic + '\'' +
                    ", from=" + from +
                    '}';
        }
    }

}