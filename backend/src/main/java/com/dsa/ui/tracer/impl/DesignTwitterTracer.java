package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** Stateful Twitter simulation whose feed merges followed users' tweet streams by recency. */
@Component
public class DesignTwitterTracer implements AlgorithmTracer {
    private static final String OP = "(post \\d{1,2} \\d{1,4}|follow \\d{1,2} \\d{1,2}|unfollow \\d{1,2} \\d{1,2}|feed \\d{1,2})";

    @Override public String id() { return "design-twitter"; }
    @Override public DsType dsType() { return DsType.HEAP; }

    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("operations", FieldType.STRING).label("Twitter operations")
                .help("Semicolon-separated: post USER TWEET, follow USER OTHER, unfollow USER OTHER, or feed USER.")
                .length(1, 500).constraint("pattern", OP + "(;" + OP + ")*")
                .constraint("operationsHint", "Example: post 1 101;follow 1 2;feed 1")
                .defaultValue("post 1 101;post 2 201;post 2 202;follow 1 2;feed 1;unfollow 1 2;feed 1")
                .build());
    }

    @Override public Map<String, Object> alternateInput() {
        return Map.of("operations", "post 1 11;post 2 21;post 3 31;follow 1 2;follow 1 3;"
                + "post 2 22;feed 1;unfollow 1 2;post 3 32;feed 1;feed 2");
    }

    @Override public String annotatedCode() {
        return """
               public void postTweet(int user, int tweet) {
                   // @a post
                   tweets.get(user).add(new Tweet(tweet, clock++));
               }
               public void follow(int user, int other) {
                   // @a follow
                   follows.get(user).add(other);
               }
               public void unfollow(int user, int other) {
                   // @a unfollow
                   follows.get(user).remove(other);
               }
               public List<Integer> getNewsFeed(int user) {
                   // @a feed.seed
                   seedLatestTweets(user);
                   while (!heap.isEmpty() && feed.size() < 10) {
                       // @a feed.pop
                       Cursor newest = heap.poll();
                       feed.add(newest.tweet.id);
                       if (newest.hasPrevious()) {
                           // @a feed.push
                           heap.offer(newest.previous());
                       }
                   }
                   // @a done
                   return feed;
               }""";
    }

    @Override public void run(Inputs in, StepEmitter emit) {
        Map<Integer, List<Tweet>> tweets = new LinkedHashMap<>();
        Map<Integer, Set<Integer>> follows = new LinkedHashMap<>();
        Map<Integer, List<Integer>> feeds = new LinkedHashMap<>();
        int clock = 0;
        for (String raw : in.getString("operations").split(";")) {
            String[] parts = raw.split(" ");
            int user = Integer.parseInt(parts[1]);
            switch (parts[0]) {
                case "post" -> {
                    int tweetId = Integer.parseInt(parts[2]);
                    tweets.computeIfAbsent(user, ignored -> new ArrayList<>()).add(new Tweet(tweetId, user, clock++));
                    emit.at("post").say("User %d posts tweet %d at timestamp %d.", user, tweetId, clock - 1)
                            .var("user", user).var("tweet", tweetId).arrayState(recentTweets(tweets)).step();
                }
                case "follow" -> {
                    int other = Integer.parseInt(parts[2]);
                    follows.computeIfAbsent(user, ignored -> new LinkedHashSet<>()).add(other);
                    emit.at("follow").say("User %d now follows user %d.", user, other)
                            .var("user", user).var("follows", follows.get(user)).arrayState(recentTweets(tweets)).step();
                }
                case "unfollow" -> {
                    int other = Integer.parseInt(parts[2]);
                    follows.computeIfAbsent(user, ignored -> new LinkedHashSet<>()).remove(other);
                    emit.at("unfollow").say("User %d unfollows user %d; their future feeds exclude that stream.", user, other)
                            .var("user", user).var("follows", follows.get(user)).arrayState(recentTweets(tweets)).step();
                }
                case "feed" -> feeds.put(user, buildFeed(user, tweets, follows, emit));
                default -> throw new IllegalStateException("Validated operation became unknown: " + parts[0]);
            }
        }
        emit.at("done").say("All operations complete. Most recent feed per requested user: %s.", feeds)
                .var("feeds", feeds).arrayState(recentTweets(tweets)).step();
    }

    private static List<Integer> buildFeed(int user, Map<Integer, List<Tweet>> tweets,
                                           Map<Integer, Set<Integer>> follows, StepEmitter emit) {
        PriorityQueue<Cursor> heap = new PriorityQueue<>(Comparator.comparingInt(Cursor::time).reversed());
        Set<Integer> sources = new LinkedHashSet<>();
        sources.add(user);
        sources.addAll(follows.getOrDefault(user, Set.of()));
        for (int source : sources) {
            List<Tweet> stream = tweets.getOrDefault(source, List.of());
            if (!stream.isEmpty()) heap.offer(new Cursor(stream, stream.size() - 1));
        }
        emit.at("feed.seed").say("Seed user %d's feed heap with the newest tweet from each of %d stream(s).",
                        user, sources.size())
                .var("user", user).var("sources", sources).arrayState(render(heap)).step();

        List<Integer> feed = new ArrayList<>();
        while (!heap.isEmpty() && feed.size() < 10) {
            Cursor newest = heap.poll();
            feed.add(newest.tweet().id());
            emit.at("feed.pop").say("Take newest tweet %d from user %d into feed position %d.",
                            newest.tweet().id(), newest.tweet().user(), feed.size())
                    .var("feed", feed).arrayState(renderFeed(feed)).step();
            if (newest.index() > 0) {
                Cursor previous = new Cursor(newest.stream(), newest.index() - 1);
                heap.offer(previous);
                emit.at("feed.push").say("Push user %d's next older tweet %d as a merge candidate.",
                                previous.tweet().user(), previous.tweet().id())
                        .var("feed", feed).arrayState(render(heap)).step();
            }
        }
        return feed;
    }

    private static List<ArrayElement> recentTweets(Map<Integer, List<Tweet>> tweets) {
        List<Tweet> all = tweets.values().stream().flatMap(Collection::stream)
                .sorted(Comparator.comparingInt(Tweet::time).reversed()).toList();
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Tweet t = all.get(i);
            out.add(new ArrayElement(i, t.id(), "default", "u" + t.user()));
        }
        return out;
    }

    private static List<ArrayElement> render(PriorityQueue<Cursor> heap) {
        List<Cursor> copy = new ArrayList<>(heap);
        copy.sort(Comparator.comparingInt(Cursor::time).reversed());
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < copy.size(); i++) {
            Tweet t = copy.get(i).tweet();
            out.add(new ArrayElement(i, t.id(), i == 0 ? "target" : "default", "u" + t.user()));
        }
        return out;
    }

    private static List<ArrayElement> renderFeed(List<Integer> feed) {
        List<ArrayElement> out = new ArrayList<>();
        for (int i = 0; i < feed.size(); i++) out.add(new ArrayElement(i, feed.get(i), i == feed.size() - 1 ? "current" : "default"));
        return out;
    }

    private record Tweet(int id, int user, int time) {}
    private record Cursor(List<Tweet> stream, int index) {
        Tweet tweet() { return stream.get(index); }
        int time() { return tweet().time(); }
    }
}
