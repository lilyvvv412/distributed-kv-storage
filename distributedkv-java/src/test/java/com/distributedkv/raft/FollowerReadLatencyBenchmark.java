package com.distributedkv.raft;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.distributedkv.network.protocol.FollowerReadRequestMessage;
import com.distributedkv.network.protocol.FollowerReadResponseMessage;
import com.distributedkv.network.protocol.ReadIndexRequestMessage;
import com.distributedkv.network.protocol.ReadIndexResponseMessage;

/**
 * Measures Leader ReadIndex vs FollowerRead latency under a controlled network-delay model.
 *
 * <p>Methodology (honest, path-level):
 * <ul>
 *   <li>Leader read: {@link ReadIndex} on the leader confirms leadership via
 *       {@code broadcastHeartbeats()} (majority RTT modeled as {@code leaderHeartbeatMs}).</li>
 *   <li>FollowerRead: lease-valid follower forwards one lightweight safety check to the leader
 *       (single RPC modeled as {@code followerRpcMs}); caught-up followers read immediately.</li>
 * </ul>
 *
 * <p>This is not a claim of production-cluster QPS. Numbers are reproducible on this machine
 * with the modeled RTTs documented in the report.
 */
public class FollowerReadLatencyBenchmark {

    private static final int DEFAULT_OPS = 2000;
    private static final int DEFAULT_CONCURRENCY = 8;
    /** Modeled majority heartbeat RTT for a 3-node group (ms). */
    private static final long DEFAULT_LEADER_HB_MS = 5;
    /** Modeled single-hop leader check for FollowerRead (ms). */
    private static final long DEFAULT_FOLLOWER_RPC_MS = 2;

    @Test
    void runFollowerReadLatencyBenchmark() throws Exception {
        BenchmarkConfig config = BenchmarkConfig.fromEnvOrDefaults();
        BenchmarkResult result = run(config);
        System.out.println(result.toMarkdown());
        Path report = writeReport(result, config);
        System.out.println("Wrote " + report.toAbsolutePath());
    }

    public static void main(String[] args) throws Exception {
        new FollowerReadLatencyBenchmark().runFollowerReadLatencyBenchmark();
    }

    static BenchmarkResult run(BenchmarkConfig config) throws Exception {
        LatencyStats leader = measureLeaderReads(config);
        LatencyStats follower = measureFollowerReads(config);
        return new BenchmarkResult(config, leader, follower);
    }

    private static LatencyStats measureLeaderReads(BenchmarkConfig config) throws Exception {
        RaftNode raftNode = mock(RaftNode.class);
        AsyncApplier asyncApplier = mock(AsyncApplier.class);

        when(raftNode.isLeader()).thenReturn(true);
        when(raftNode.getCurrentTerm()).thenReturn(1L);
        when(raftNode.getGroupId()).thenReturn("g1");
        when(raftNode.getNodeId()).thenReturn("leader");
        when(raftNode.getCommitIndex()).thenReturn(100L);
        when(raftNode.broadcastHeartbeats()).thenAnswer(inv -> delayed(true, config.leaderHeartbeatMs));
        when(asyncApplier.getLastAppliedIndex()).thenReturn(100L);
        when(asyncApplier.waitForApplied(anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        Function<ReadIndexRequestMessage, CompletableFuture<ReadIndexResponseMessage>> forwarder =
                req -> delayed(new ReadIndexResponseMessage(1L, "g1", "leader", req.getReadId(), 100L),
                        config.leaderHeartbeatMs);

        ReadIndex readIndex = new ReadIndex(raftNode, asyncApplier, forwarder, 10_000);
        try {
            return measure("LeaderReadIndex", config, () ->
                    readIndex.read("k".getBytes(StandardCharsets.UTF_8), key -> "v")
                            .get(10, TimeUnit.SECONDS));
        } finally {
            readIndex.stop();
        }
    }

    private static LatencyStats measureFollowerReads(BenchmarkConfig config) throws Exception {
        RaftNode raftNode = mock(RaftNode.class);
        AsyncApplier asyncApplier = mock(AsyncApplier.class);
        ReadIndex readIndexFallback = mock(ReadIndex.class);

        when(raftNode.isLeader()).thenReturn(false);
        when(raftNode.getCurrentTerm()).thenReturn(1L);
        when(raftNode.getGroupId()).thenReturn("g1");
        when(raftNode.getNodeId()).thenReturn("follower");
        when(raftNode.getCommitIndex()).thenReturn(100L);
        when(asyncApplier.getLastAppliedIndex()).thenReturn(100L);
        when(asyncApplier.waitForApplied(anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        Function<FollowerReadRequestMessage, CompletableFuture<FollowerReadResponseMessage>> forwarder =
                req -> delayed(new FollowerReadResponseMessage(1L, "g1", "leader", req.getReadId()),
                        config.followerRpcMs);

        FollowerRead followerRead = new FollowerRead(
                raftNode, asyncApplier, readIndexFallback, forwarder, 10_000, 5_000);
        followerRead.updateHeartbeat(1L, 100L);
        try {
            return measure("FollowerRead", config, () ->
                    followerRead.read("k".getBytes(StandardCharsets.UTF_8), key -> "v")
                            .get(10, TimeUnit.SECONDS));
        } finally {
            followerRead.stop();
        }
    }

    private static LatencyStats measure(String name, BenchmarkConfig config, ThrowingRunnable op)
            throws Exception {
        // Warmup
        for (int i = 0; i < Math.min(100, config.operations / 10); i++) {
            op.run();
        }

        long[] latenciesNs = new long[config.operations];
        AtomicInteger next = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(config.concurrency);
        ExecutorService pool = Executors.newFixedThreadPool(config.concurrency);
        AtomicInteger errors = new AtomicInteger(0);

        long wallStart = System.nanoTime();
        for (int t = 0; t < config.concurrency; t++) {
            pool.submit(() -> {
                try {
                    while (true) {
                        int i = next.getAndIncrement();
                        if (i >= config.operations) {
                            break;
                        }
                        long start = System.nanoTime();
                        try {
                            op.run();
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                        latenciesNs[i] = System.nanoTime() - start;
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        if (!done.await(5, TimeUnit.MINUTES)) {
            pool.shutdownNow();
            throw new IllegalStateException(name + " benchmark timed out");
        }
        long wallNs = System.nanoTime() - wallStart;
        pool.shutdown();

        List<Long> samples = new ArrayList<>(config.operations);
        for (long ns : latenciesNs) {
            if (ns > 0) {
                samples.add(ns);
            }
        }
        Collections.sort(samples);
        return LatencyStats.from(name, samples, wallNs, errors.get(), config.concurrency);
    }

    private static <T> CompletableFuture<T> delayed(T value, long delayMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return value;
        });
    }

    private static Path writeReport(BenchmarkResult result, BenchmarkConfig config) throws IOException {
        Path reportDir = Paths.get(System.getProperty("distributedkv.reportDir", "../reports"))
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(reportDir);
        String ts = Instant.now().toString().replace(":", "").replace(".", "-");
        Path out = reportDir.resolve("follower-read-benchmark-" + ts + ".md");
        Files.writeString(out, result.toMarkdown());
        Path latest = reportDir.resolve("follower-read-benchmark-latest.md");
        Files.writeString(latest, result.toMarkdown());
        return out;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    static final class BenchmarkConfig {
        final int operations;
        final int concurrency;
        final long leaderHeartbeatMs;
        final long followerRpcMs;

        BenchmarkConfig(int operations, int concurrency, long leaderHeartbeatMs, long followerRpcMs) {
            this.operations = operations;
            this.concurrency = concurrency;
            this.leaderHeartbeatMs = leaderHeartbeatMs;
            this.followerRpcMs = followerRpcMs;
        }

        static BenchmarkConfig fromEnvOrDefaults() {
            return new BenchmarkConfig(
                    envInt("FOLLOWER_READ_BENCH_OPS", DEFAULT_OPS),
                    envInt("FOLLOWER_READ_BENCH_CONCURRENCY", DEFAULT_CONCURRENCY),
                    envLong("FOLLOWER_READ_BENCH_LEADER_HB_MS", DEFAULT_LEADER_HB_MS),
                    envLong("FOLLOWER_READ_BENCH_FOLLOWER_RPC_MS", DEFAULT_FOLLOWER_RPC_MS));
        }

        private static int envInt(String key, int def) {
            String v = System.getenv(key);
            return v == null || v.isBlank() ? def : Integer.parseInt(v.trim());
        }

        private static long envLong(String key, long def) {
            String v = System.getenv(key);
            return v == null || v.isBlank() ? def : Long.parseLong(v.trim());
        }
    }

    static final class LatencyStats {
        final String name;
        final int operations;
        final int concurrency;
        final int errors;
        final double avgMs;
        final double p50Ms;
        final double p95Ms;
        final double throughputOpsPerSec;

        LatencyStats(String name, int operations, int concurrency, int errors,
                     double avgMs, double p50Ms, double p95Ms, double throughputOpsPerSec) {
            this.name = name;
            this.operations = operations;
            this.concurrency = concurrency;
            this.errors = errors;
            this.avgMs = avgMs;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.throughputOpsPerSec = throughputOpsPerSec;
        }

        static LatencyStats from(String name, List<Long> samplesNs, long wallNs, int errors, int concurrency) {
            if (samplesNs.isEmpty()) {
                return new LatencyStats(name, 0, concurrency, errors, 0, 0, 0, 0);
            }
            double sumMs = 0;
            for (long ns : samplesNs) {
                sumMs += ns / 1_000_000.0;
            }
            int n = samplesNs.size();
            double avg = sumMs / n;
            double p50 = samplesNs.get((int) Math.floor(0.50 * (n - 1))) / 1_000_000.0;
            double p95 = samplesNs.get((int) Math.floor(0.95 * (n - 1))) / 1_000_000.0;
            double tput = wallNs > 0 ? n / (wallNs / 1_000_000_000.0) : 0;
            return new LatencyStats(name, n, concurrency, errors, avg, p50, p95, tput);
        }
    }

    static final class BenchmarkResult {
        final BenchmarkConfig config;
        final LatencyStats leader;
        final LatencyStats follower;

        BenchmarkResult(BenchmarkConfig config, LatencyStats leader, LatencyStats follower) {
            this.config = config;
            this.leader = leader;
            this.follower = follower;
        }

        double improvementAvg() {
            if (leader.avgMs <= 0) {
                return 0;
            }
            return (leader.avgMs - follower.avgMs) / leader.avgMs;
        }

        double improvementP50() {
            if (leader.p50Ms <= 0) {
                return 0;
            }
            return (leader.p50Ms - follower.p50Ms) / leader.p50Ms;
        }

        String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            sb.append("# FollowerRead Latency Benchmark\n\n");
            sb.append("- Timestamp: ").append(Instant.now()).append('\n');
            sb.append("- Modeled leader heartbeat (majority) RTT: ")
                    .append(config.leaderHeartbeatMs).append(" ms\n");
            sb.append("- Modeled follower→leader safety-check RTT: ")
                    .append(config.followerRpcMs).append(" ms\n");
            sb.append("- Note: path-level measurement using real `ReadIndex` / `FollowerRead` classes ")
                    .append("with injected network delays; not a production-cluster SLO claim.\n\n");

            sb.append("## Leader read (ReadIndex on leader)\n\n");
            appendStats(sb, leader);

            sb.append("## FollowerRead\n\n");
            appendStats(sb, follower);

            sb.append("## Improvement\n\n");
            sb.append(String.format("- average: **%.1f%%**  ((leader - follower) / leader)%n",
                    improvementAvg() * 100.0));
            sb.append(String.format("- P50: **%.1f%%**%n", improvementP50() * 100.0));
            sb.append(String.format("- Leader avg: %.3f ms, FollowerRead avg: %.3f ms%n",
                    leader.avgMs, follower.avgMs));
            return sb.toString();
        }

        private static void appendStats(StringBuilder sb, LatencyStats s) {
            sb.append("| metric | value |\n|---|---|\n");
            sb.append("| operations | ").append(s.operations).append(" |\n");
            sb.append("| concurrency | ").append(s.concurrency).append(" |\n");
            sb.append("| errors | ").append(s.errors).append(" |\n");
            sb.append(String.format("| average | %.3f ms |%n", s.avgMs));
            sb.append(String.format("| P50 | %.3f ms |%n", s.p50Ms));
            sb.append(String.format("| P95 | %.3f ms |%n", s.p95Ms));
            sb.append(String.format("| throughput | %.1f ops/s |%n%n", s.throughputOpsPerSec));
        }
    }
}
