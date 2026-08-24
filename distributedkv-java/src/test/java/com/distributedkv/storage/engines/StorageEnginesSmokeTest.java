package com.distributedkv.storage.engines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.distributedkv.storage.StorageEngine;
import com.distributedkv.storage.engines.btree.BPlusTreeEngine;
import com.distributedkv.storage.engines.hashtable.HashTableEngine;
import com.distributedkv.storage.engines.rocksdb.RocksDBEngine;

/**
 * Resume-critical smoke coverage for RocksDB / B+Tree / HashTable engines.
 */
public class StorageEnginesSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void hashTablePutGetDelete() throws Exception {
        exercise(new HashTableEngine("ht", tempDir.resolve("ht").toString()));
    }

    @Test
    void bPlusTreePutGetDelete() throws Exception {
        exercise(new BPlusTreeEngine("bt", tempDir.resolve("bt").toString()));
    }

    @Test
    void rocksDbPutGetDelete() throws Exception {
        Path path = Files.createDirectories(tempDir.resolve("rocks"));
        exercise(new RocksDBEngine("rocks", path.toString()));
    }

    private static void exercise(StorageEngine engine) throws Exception {
        engine.initialize();
        try {
            byte[] key = "hello".getBytes();
            byte[] value = "world".getBytes();
            engine.put(key, value);
            assertArrayEquals(value, engine.get(key));
            assertTrue(engine.delete(key));
            assertNull(engine.get(key));
        } finally {
            engine.close();
        }
    }
}
