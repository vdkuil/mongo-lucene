package com.github.mongoutils.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.store.*;
import org.apache.lucene.util.Accountable;

public class MapDirectory extends BaseDirectory implements Accountable {

    public static final int DEFAULT_BUFFER_SIZE = 1024;

    ConcurrentMap<String, MapDirectoryEntry> store;
    protected final AtomicLong sizeInBytes = new AtomicLong();
    int bufferSize;

    public MapDirectory(final ConcurrentMap<String, MapDirectoryEntry> store) throws IOException {
        this(store, DEFAULT_BUFFER_SIZE);
    }

    private MapDirectory(final ConcurrentMap<String, MapDirectoryEntry> store, final int bufferSize) throws IOException {
        this.store = store;
        this.bufferSize = bufferSize;
        if (lockFactory == null) {
            setLockFactory(new SingleInstanceLockFactory());
        }
    }

    /*public ConcurrentMap<String, MapDirectoryEntry> getStore() {
        return store;
    }*/

    @Override
    public String getLockID() {
        return "lucene-" + Integer.toHexString(hashCode());
    }

    @Override
    public String[] listAll() throws IOException {
        String[] files = new String[store.size()];
        int index = 0;

        for (String file : store.keySet()) {
            files[index++] = file;
        }

        return files;
    }

    @Override
    public boolean fileExists(final String name) throws IOException {
        ensureOpen();
        return store.containsKey(name);
    }


    @Override
    public void deleteFile(final String name) throws IOException {
        ensureOpen();
        MapDirectoryEntry file = store.remove(name);
        if (file != null) {
            sizeInBytes.addAndGet(-file.sizeInBytes);
        } else {
            throw new FileNotFoundException(name);
        }
    }

    @Override
    public long fileLength(final String name) throws IOException {
        ensureOpen();
        if (!store.containsKey(name)) {
            throw new FileNotFoundException(name);
        }
        return store.get(name).getLength();
    }


    @Override
    public void close() throws IOException {
        isOpen = false;
        store.clear();
    }

    @Override
    public IndexOutput createOutput(String s, IOContext ioContext) throws IOException {
        ensureOpen();
        MapDirectoryEntry file = new MapDirectoryEntry();
        /*MapDirectoryEntry existing = store.remove(s);
        if (existing != null) {
            sizeInBytes.addAndGet(-existing.sizeInBytes);
        }*/
        file.setBufferSize(bufferSize);
        store.put(s, file);
        return new MapDirectoryOutputStream(file, s, store, bufferSize);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        /*ensureOpen();
        MapDirectoryEntry file;
        //Set<String> toSync = new HashSet<String>(names);
        //toSync.retainAll(staleFiles);
        for (String name : names) {
            if (!store.containsKey(name)) {
                throw new FileNotFoundException(name);
            }
            file = store.get(name);
            file.setLastModified(System.currentTimeMillis());
            store.put(name, file);
        }
*/

    }

    @Override
    public IndexInput openInput(String s, IOContext ioContext) throws IOException {
        ensureOpen();
        if (!store.containsKey(s)) {
            throw new FileNotFoundException(s);
        }
        return new MapDirectoryInputStream(s, store.get(s));
    }

    @Override
    public long ramBytesUsed() {
        ensureOpen();
        return sizeInBytes.get();
    }
}
