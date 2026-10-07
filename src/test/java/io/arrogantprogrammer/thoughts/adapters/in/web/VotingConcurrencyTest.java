package io.arrogantprogrammer.thoughts.adapters.in.web;

import io.arrogantprogrammer.thoughts.adapters.out.persistence.ThoughtEntity;
import io.arrogantprogrammer.thoughts.domain.Author;
import io.arrogantprogrammer.thoughts.domain.Content;
import io.arrogantprogrammer.thoughts.domain.Thought;
import io.arrogantprogrammer.thoughts.domain.ThoughtId;
import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the concurrent-votes acceptance criterion: simultaneous thumbs-up requests
 * against the same thought must not lose updates to a read-modify-write race.
 */
@QuarkusTest
class VotingConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 20;

    @Inject
    ThoughtRepository thoughtRepository;

    // Fires CONCURRENT_REQUESTS thumbs-up requests at the same thought at (as close to)
    // the same instant as possible, using latches to line every thread up at the starting
    // gate, then asserts every request succeeded and every vote was actually counted.
    @Test
    void concurrentThumbsUpVotesAreNotLost() throws InterruptedException {
        UUID id = QuarkusTransaction.requiringNew().call(() -> {
            ThoughtEntity.deleteAll();
            Thought thought = Thought.create(
                    new Content("A thought voted on by many people at once."),
                    new Author("Author", null));
            thought.approve();
            thoughtRepository.save(thought);
            return thought.id().value();
        });

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> statusCodes = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            pool.submit(() -> {
                // Signal this worker is ready, then block until every other worker is too,
                // so the requests actually overlap instead of trickling in one at a time.
                ready.countDown();
                await(start);
                int status = given().post("/thoughts/" + id + "/thumbs-up").statusCode();
                synchronized (statusCodes) {
                    statusCodes.add(status);
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(CONCURRENT_REQUESTS, statusCodes.size());
        statusCodes.forEach(code -> assertEquals(200, code));

        // The real assertion: no concurrent increment was silently dropped.
        int finalThumbsUp = QuarkusTransaction.requiringNew().call(() ->
                thoughtRepository.findById(new ThoughtId(id)).orElseThrow().rating().thumbsUp());
        assertEquals(CONCURRENT_REQUESTS, finalThumbsUp);
    }

    // Wraps the checked InterruptedException from CountDownLatch.await() so it can be
    // used inside the unchecked Runnable submitted to the executor.
    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
