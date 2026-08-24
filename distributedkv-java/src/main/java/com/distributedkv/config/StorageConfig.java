package com.distributedkv.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for the storage engines
 */
public class StorageConfig {
    /**
     * Enum for storage engine types
     */
    public enum StorageType {
        ROCKSDB,
        BTREE,
        HASHTABLE
    }

    private final String dataDir;
    private final StorageType defaultStorageType;
    private final int hashTableBuckets;
    private final int bTreeOrder;
    private final int bTreeNodeSize;
    private final boolean enableMVCC;
    private final int mvccGcIntervalSeconds;
    private final int mvccMaxVersions;
    private final Map<String, String> engineMappings;

    // RocksDB specific configurations
    private final int rocksDbMaxOpenFiles;
    private final int rocksDbBlockSize;
    private final int rocksDbBlockCacheSizeMb;
    private final int rocksDbWriteBufferSizeMb;
    private final int rocksDbMaxWriteBufferNumber;
    private final String rocksDbCompression;
    private final boolean rocksDbUseDirectIo;
    private final boolean rocksDbAllowConcurrentMemtableWrite;
    private final boolean rocksDbEnableWriteThreadAdaptivity;
    private final String rocksDbWalDir;
    private final int rocksDbWalSizeLimitMb;

    /**
     * Constructs a StorageConfig from properties
     *
     * @param properties the configuration properties
     */
    public StorageConfig(Properties properties) {
        this.dataDir = properties.getProperty("storage.data.dir", "./data");
        String storageType = properties.getProperty("storage.type", "ROCKSDB");
        this.defaultStorageType = StorageType.valueOf(storageType.toUpperCase());

        // General storage configurations
        this.hashTableBuckets = Integer.parseInt(properties.getProperty("storage.hashtable.buckets", "1000000"));
        this.bTreeOrder = Integer.parseInt(properties.getProperty("storage.btree.order", "128"));
        this.bTreeNodeSize = Integer.parseInt(properties.getProperty("storage.btree.node.size", "8192"));
        this.enableMVCC = Boolean.parseBoolean(properties.getProperty("storage.enable.mvcc", "true"));
        this.mvccGcIntervalSeconds = Integer.parseInt(properties.getProperty("storage.mvcc.gc.interval.seconds", "300"));
        this.mvccMaxVersions = Integer.parseInt(properties.getProperty("storage.mvcc.max.versions", "5"));

        // RocksDB specific configurations
        this.rocksDbMaxOpenFiles = Integer.parseInt(properties.getProperty("storage.rocksdb.max.open.files", "1000"));
        this.rocksDbBlockSize = Integer.parseInt(properties.getProperty("storage.rocksdb.block.size", "4096"));
        this.rocksDbBlockCacheSizeMb = Integer.parseInt(properties.getProperty("storage.rocksdb.block.cache.size.mb", "512"));
        this.rocksDbWriteBufferSizeMb = Integer.parseInt(properties.getProperty("storage.rocksdb.write.buffer.size.mb", "64"));
        this.rocksDbMaxWriteBufferNumber = Integer.parseInt(properties.getProperty("storage.rocksdb.max.write.buffer.number", "4"));
        this.rocksDbCompression = properties.getProperty("storage.rocksdb.compression", "SNAPPY");
        this.rocksDbUseDirectIo = Boolean.parseBoolean(properties.getProperty("storage.rocksdb.use.direct.io", "true"));
        this.rocksDbAllowConcurrentMemtableWrite = Boolean.parseBoolean(
                properties.getProperty("storage.rocksdb.allow.concurrent.memtable.write", "true"));
        this.rocksDbEnableWriteThreadAdaptivity = Boolean.parseBoolean(
                properties.getProperty("storage.rocksdb.enable.write.thread.adaptivity", "true"));
        this.rocksDbWalDir = properties.getProperty("storage.rocksdb.wal.dir", "");
        this.rocksDbWalSizeLimitMb = Integer.parseInt(properties.getProperty("storage.rocksdb.wal.size.limit.mb", "0"));

        // Parse engine mappings
        this.engineMappings = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("storage.mapping.")) {
                String prefix = "storage.mapping.";
                String mapKey = key.substring(prefix.length());
                engineMappings.put(mapKey, properties.getProperty(key));
            }
        }
    }

    /**
     * Gets the default storage engine type
     *
     * @return default storage type
     */
    public StorageType getDefaultStorageType() {
        return defaultStorageType;
    }

    public String getDataDir() {
        return dataDir;
    }

    public Map<String, String> getRocksDBOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("max_open_files", String.valueOf(rocksDbMaxOpenFiles));
        options.put("block_size", String.valueOf(rocksDbBlockSize));
        options.put("block_cache_size_mb", String.valueOf(rocksDbBlockCacheSizeMb));
        options.put("write_buffer_size_mb", String.valueOf(rocksDbWriteBufferSizeMb));
        options.put("max_write_buffer_number", String.valueOf(rocksDbMaxWriteBufferNumber));
        options.put("compression", rocksDbCompression);
        options.put("use_direct_io", String.valueOf(rocksDbUseDirectIo));
        options.put("allow_concurrent_memtable_write", String.valueOf(rocksDbAllowConcurrentMemtableWrite));
        options.put("enable_write_thread_adaptivity", String.valueOf(rocksDbEnableWriteThreadAdaptivity));
        if (rocksDbWalDir != null && !rocksDbWalDir.isEmpty()) {
            options.put("wal_dir", rocksDbWalDir);
        }
        options.put("wal_size_limit_mb", String.valueOf(rocksDbWalSizeLimitMb));
        return options;
    }

    public com.distributedkv.storage.engines.btree.BPlusTreeEngine.BPlusTreeOptions getBTreeOptions() {
        com.distributedkv.storage.engines.btree.BPlusTreeEngine.BPlusTreeOptions options =
                new com.distributedkv.storage.engines.btree.BPlusTreeEngine.BPlusTreeOptions();
        // Keep defaults; order/node size are available via getters if engine options expand later.
        return options;
    }

    public com.distributedkv.storage.mvcc.MVCCStorage.MVCCOptions getMvccOptions() {
        com.distributedkv.storage.mvcc.MVCCStorage.MVCCOptions options =
                new com.distributedkv.storage.mvcc.MVCCStorage.MVCCOptions();
        return options;
    }

    /**
     * Gets the number of buckets for hash table storage
     *
     * @return hash table buckets
     */
    public int getHashTableBuckets() {
        return hashTableBuckets;
    }

    /**
     * Gets the order for B+ tree storage
     *
     * @return B+ tree order
     */
    public int getBTreeOrder() {
        return bTreeOrder;
    }

    /**
     * Gets the node size for B+ tree storage
     *
     * @return B+ tree node size
     */
    public int getBTreeNodeSize() {
        return bTreeNodeSize;
    }

    /**
     * Checks if MVCC is enabled
     *
     * @return true if MVCC is enabled
     */
    public boolean isEnableMVCC() {
        return enableMVCC;
    }

    /**
     * Gets the MVCC garbage collection interval in seconds
     *
     * @return MVCC GC interval
     */
    public int getMvccGcIntervalSeconds() {
        return mvccGcIntervalSeconds;
    }

    /**
     * Gets the maximum number of versions for MVCC
     *
     * @return MVCC max versions
     */
    public int getMvccMaxVersions() {
        return mvccMaxVersions;
    }

    /**
     * Gets the storage type for a specific key prefix
     *
     * @param keyPrefix the key prefix
     * @return the storage type for the prefix, or the default type if no mapping exists
     */
    public StorageType getStorageTypeForPrefix(String keyPrefix) {
        String typeStr = engineMappings.getOrDefault(keyPrefix, defaultStorageType.name());
        return StorageType.valueOf(typeStr.toUpperCase());
    }

    /**
     * Gets all engine mappings
     *
     * @return map of key prefixes to storage types
     */
    public Map<String, String> getEngineMappings() {
        return new HashMap<>(engineMappings);
    }

    /**
     * Gets the maximum number of open files for RocksDB
     *
     * @return RocksDB max open files
     */
    public int getRocksDbMaxOpenFiles() {
        return rocksDbMaxOpenFiles;
    }

    /**
     * Gets the block size for RocksDB
     *
     * @return RocksDB block size
     */
    public int getRocksDbBlockSize() {
        return rocksDbBlockSize;
    }

    /**
     * Gets the block cache size in MB for RocksDB
     *
     * @return RocksDB block cache size in MB
     */
    public int getRocksDbBlockCacheSizeMb() {
        return rocksDbBlockCacheSizeMb;
    }

    /**
     * Gets the write buffer size in MB for RocksDB
     *
     * @return RocksDB write buffer size in MB
     */
    public int getRocksDbWriteBufferSizeMb() {
        return rocksDbWriteBufferSizeMb;
    }

    /**
     * Gets the maximum write buffer number for RocksDB
     *
     * @return RocksDB max write buffer number
     */
    public int getRocksDbMaxWriteBufferNumber() {
        return rocksDbMaxWriteBufferNumber;
    }

    /**
     * Gets the compression algorithm for RocksDB
     *
     * @return RocksDB compression algorithm
     */
    public String getRocksDbCompression() {
        return rocksDbCompression;
    }

    /**
     * Checks if direct I/O is enabled for RocksDB
     *
     * @return true if direct I/O is enabled
     */
    public boolean isRocksDbUseDirectIo() {
        return rocksDbUseDirectIo;
    }

    /**
     * Checks if concurrent memtable write is allowed for RocksDB
     *
     * @return true if concurrent memtable write is allowed
     */
    public boolean isRocksDbAllowConcurrentMemtableWrite() {
        return rocksDbAllowConcurrentMemtableWrite;
    }

    /**
     * Checks if write thread adaptivity is enabled for RocksDB
     *
     * @return true if write thread adaptivity is enabled
     */
    public boolean isRocksDbEnableWriteThreadAdaptivity() {
        return rocksDbEnableWriteThreadAdaptivity;
    }

    /**
     * Gets the WAL directory for RocksDB
     *
     * @return RocksDB WAL directory
     */
    public String getRocksDbWalDir() {
        return rocksDbWalDir;
    }

    /**
     * Gets the WAL size limit in MB for RocksDB
     *
     * @return RocksDB WAL size limit in MB
     */
    public int getRocksDbWalSizeLimitMb() {
        return rocksDbWalSizeLimitMb;
    }

    @Override
    public String toString() {
        return "StorageConfig{" +
                "defaultStorageType=" + defaultStorageType +
                ", hashTableBuckets=" + hashTableBuckets +
                ", bTreeOrder=" + bTreeOrder +
                ", bTreeNodeSize=" + bTreeNodeSize +
                ", enableMVCC=" + enableMVCC +
                ", mvccGcIntervalSeconds=" + mvccGcIntervalSeconds +
                ", mvccMaxVersions=" + mvccMaxVersions +
                ", engineMappings=" + engineMappings +
                ", rocksDbMaxOpenFiles=" + rocksDbMaxOpenFiles +
                ", rocksDbBlockSize=" + rocksDbBlockSize +
                ", rocksDbBlockCacheSizeMb=" + rocksDbBlockCacheSizeMb +
                ", rocksDbWriteBufferSizeMb=" + rocksDbWriteBufferSizeMb +
                ", rocksDbMaxWriteBufferNumber=" + rocksDbMaxWriteBufferNumber +
                ", rocksDbCompression='" + rocksDbCompression + '\'' +
                ", rocksDbUseDirectIo=" + rocksDbUseDirectIo +
                ", rocksDbAllowConcurrentMemtableWrite=" + rocksDbAllowConcurrentMemtableWrite +
                ", rocksDbEnableWriteThreadAdaptivity=" + rocksDbEnableWriteThreadAdaptivity +
                ", rocksDbWalDir='" + rocksDbWalDir + '\'' +
                ", rocksDbWalSizeLimitMb=" + rocksDbWalSizeLimitMb +
                '}';
    }
}
