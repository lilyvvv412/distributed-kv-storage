package com.distributedkv.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.distributedkv.storage.engines.hashtable.HashTableEngine;

/**
 * Minimal local migration path between two storage engines / Raft-group stand-ins.
 *
 * <p>Full cross-group RPC migration remains in Day2 Go ShardKV + Java DataMigrator wiring.
 */
public class DataMigrationSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void migrateKeysBetweenEngines() throws Exception {
        Path srcPath = Files.createDirectories(tempDir.resolve("src"));
        Path dstPath = Files.createDirectories(tempDir.resolve("dst"));

        HashTableEngine source = new HashTableEngine("src", srcPath.toString());
        HashTableEngine target = new HashTableEngine("dst", dstPath.toString());
        source.initialize();
        target.initialize();

        try {
            source.put("a".getBytes(), "1".getBytes());
            source.put("b".getBytes(), "2".getBytes());

            for (Map.Entry<byte[], byte[]> entry : source.getAll().entrySet()) {
                target.put(entry.getKey(), entry.getValue());
                source.delete(entry.getKey());
            }

            assertArrayEquals("1".getBytes(), target.get("a".getBytes()));
            assertArrayEquals("2".getBytes(), target.get("b".getBytes()));
            assertNull(source.get("a".getBytes()));
            assertNull(source.get("b".getBytes()));
        } finally {
            source.close();
            target.close();
        }
    }
}
