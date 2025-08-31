package org.lucentrix.metaframe.runtime.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lucentrix.metaframe.ItemPersistence;
import org.lucentrix.metaframe.metadata.Identity;
import org.lucentrix.metaframe.serde.json.JacksonConfig;
import org.pf4j.ExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class FSJsonPersistence implements ItemPersistence, ExtensionPoint {
    private static final Logger logger = LoggerFactory.getLogger(FSJsonPersistence.class);

    private final Path storageDir;

    private final ObjectMapper mapper = JacksonConfig.configureMapper();


    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public FSJsonPersistence(String root) {
        this(Paths.get(root));
    }

    public FSJsonPersistence() {
        this(Paths.get("storage"));
    }

    public FSJsonPersistence(Path storageDir) {
        if (storageDir == null) {
            throw new IllegalArgumentException("Storage directory is null!");
        }

        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }

            //Resolve symlinks
            this.storageDir = storageDir.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException ex) {
            throw new RuntimeException("Error creating persistent storage in folder " + storageDir, ex);
        }

        logger.info("Created file system crawl point persistence in folder: {} ", this.storageDir);
    }


    private Path filePathForId(String id) {
        return storageDir.resolve(id + ".json");
    }

    private ReentrantLock getLock(String id) {
        return locks.computeIfAbsent(id, k -> new ReentrantLock());
    }

    public <T extends Identity> void save(T obj) {
        Objects.requireNonNull(obj);

        String id = obj.getId();
        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            Path path = filePathForId(id);
            try (OutputStream os = Files.newOutputStream(path)) {
                mapper.writeValue(os, obj);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error saving object " + obj + " as json file", ex);
        } finally {
            try {
                lock.unlock();
            } finally {
                releaseLock(id);
            }
        }
    }

    public <T> T load(String id, Class<T> type) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(type);

        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            Path path = filePathForId(id);
            if (!Files.exists(path)) {
                return null;
            }
            try (InputStream is = Files.newInputStream(path)) {
                return mapper.readValue(is, type);
            } catch (IOException ex) {
                throw new RuntimeException("Error reading object: id=" + id + ", type=" + type, ex);
            }
        } finally {
            try {
                lock.unlock();
            } finally {
                releaseLock(id);
            }
        }
    }

    public boolean delete(String id) {
        ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            Path path = filePathForId(id);

            return Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new RuntimeException("Error deleting object: id=" + id, ex);
        } finally {
            try {
                lock.unlock();
            } finally {
                releaseLock(id);
            }
        }
    }

    private void releaseLock(String id) {
        ReentrantLock lock = locks.get(id);
        if (lock != null && !lock.isLocked() && !lock.hasQueuedThreads()) {
            locks.remove(id, lock); // atomic remove only if value unchanged
        }
    }


}
