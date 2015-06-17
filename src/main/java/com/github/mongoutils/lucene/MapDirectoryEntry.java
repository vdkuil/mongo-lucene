package com.github.mongoutils.lucene;

import org.apache.lucene.util.Accountable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MapDirectoryEntry implements Accountable {

    List<byte[]> buffers = new ArrayList<byte[]>();
    int bufferSize;
    long length;
    long lastModified = System.currentTimeMillis();
    protected long sizeInBytes;

    public MapDirectoryEntry() {
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public synchronized long getLength() {

        return length;
    }

    protected synchronized void setLength(final long length) {
        this.length = length;
    }

    public long getLastModified() {
        return lastModified;
    }

    protected void setLastModified(final long lastModified) {
        this.lastModified = lastModified;
    }

    protected byte[] addBuffer(final int size) {
        byte[] buffer = new byte[size];
        synchronized (this) {
            buffers.add(buffer);
            sizeInBytes += size;
        }
        return buffer;
    }

    protected synchronized byte[] getBuffer(final int index) {
        return buffers.get(index);
    }

    protected final synchronized int numBuffers() {
        return buffers.size();
    }

    public List<byte[]> getBuffers() {
        return buffers;
    }

    public void setBuffers(final List<byte[]> buffers) {
        this.buffers = buffers;
    }

    @Override
    public long ramBytesUsed() {
        return sizeInBytes;
    }

    @Override
    public Collection<Accountable> getChildResources() {
        return Collections.EMPTY_LIST;
    }
}
