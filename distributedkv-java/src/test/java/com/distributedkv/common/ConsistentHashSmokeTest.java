package com.distributedkv.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Resume-critical consistent hashing smoke test.
 */
public class ConsistentHashSmokeTest {

    @Test
    void keysMapToKnownNodesAndStayStable() {
        ConsistentHash<String> hash = new ConsistentHash<>(100);
        hash.addNodes(Arrays.asList("node1", "node2", "node3"));

        String node = hash.getNode("user:42");
        assertTrue(Set.of("node1", "node2", "node3").contains(node));
        assertEquals(node, hash.getNode("user:42"));
    }

    @Test
    void distributionUsesAllNodesForDiverseKeys() {
        ConsistentHash<String> hash = new ConsistentHash<>(50);
        hash.addNodes(Arrays.asList("a", "b", "c"));

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(hash.getNode("k-" + i));
        }
        assertEquals(3, seen.size());
    }
}
