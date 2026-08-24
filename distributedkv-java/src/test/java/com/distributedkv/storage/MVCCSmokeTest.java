package com.distributedkv.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.distributedkv.storage.engines.hashtable.HashTableEngine;
import com.distributedkv.storage.mvcc.MVCCStorage;
import com.distributedkv.storage.mvcc.Timestamp;
import com.distributedkv.storage.mvcc.Transaction;
import com.distributedkv.storage.mvcc.TransactionManager;

/**
 * Resume-critical MVCC version-read smoke test against the current API.
 */
public class MVCCSmokeTest {

    @TempDir
    Path tempDir;

    private HashTableEngine engine;
    private TransactionManager txManager;
    private MVCCStorage mvcc;

    @BeforeEach
    void setUp() throws Exception {
        Path path = Files.createDirectories(tempDir.resolve("mvcc-ht"));
        engine = new HashTableEngine("mvcc", path.toString());
        engine.initialize();
        txManager = new TransactionManager();
        mvcc = new MVCCStorage(engine, txManager);
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void snapshotIsolationSeesCommittedVersion() throws Exception {
        byte[] key = "k1".getBytes();
        byte[] v1 = "v1".getBytes();

        Transaction tx = mvcc.beginTransaction(false);
        tx.put(key, v1);
        tx.commit();

        byte[] read = mvcc.get(key, Timestamp.of(Long.MAX_VALUE));
        assertArrayEquals(v1, read);
    }

    @Test
    void missingKeyReturnsNull() {
        assertNull(mvcc.get("missing".getBytes(), Timestamp.of(1)));
    }
}
