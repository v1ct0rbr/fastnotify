package com.victorqueiroga.fastnotify.origem;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SingleInstanceLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static SingleInstanceLock tryLock(Path lockFile) throws IOException {
        Path parent = lockFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        FileChannel ch = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock fl;
        try {
            fl = ch.tryLock();
        } catch (IOException e) {
            closeQuietly(ch, null);
            throw e;
        }
        if (fl == null) {
            closeQuietly(ch, null);
            return null;
        }
        return new SingleInstanceLock(ch, fl);
    }

    @Override
    public void close() {
        closeQuietly(channel, lock);
    }

    private static void closeQuietly(FileChannel channel, FileLock lock) {
        if (lock != null) {
            try {
                if (lock.isValid()) {
                    lock.release();
                }
            } catch (IOException ignored) {
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
