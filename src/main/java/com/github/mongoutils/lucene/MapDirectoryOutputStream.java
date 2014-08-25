package com.github.mongoutils.lucene;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.lucene.store.BufferedChecksum;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.Accountable;

public class MapDirectoryOutputStream extends IndexOutput implements Accountable {

    boolean dirty;
    byte[] currentBuffer;
    int bufferLength;
    int bufferPosition;
    int bufferSize;
    int currentBufferIndex;
    long bufferStart;
    private final Checksum crc;
    ConcurrentMap<String, MapDirectoryEntry> map;
    MapDirectoryEntry file;
    String name;

    public MapDirectoryOutputStream(final MapDirectoryEntry f,
                                    final String name,
                                    final ConcurrentMap<String, MapDirectoryEntry> map,
                                    final int bufferSize) {
        file = f;
        this.name = name;
        this.map = map;
        this.bufferSize = bufferSize;
        currentBufferIndex = -1;
        currentBuffer = null;
        crc = new BufferedChecksum(new CRC32());
    }

    /*public void writeTo(final IndexOutput out) throws IOException {
        flush();
        final long end = file.length;
        long pos = 0;
        int buffer = 0;
        while (pos < end) {
            int length = bufferSize;
            long nextPos = pos + length;
            if (nextPos > end) {
                length = (int) (end - pos);
            }
            out.writeBytes(file.getBuffer(buffer++), length);
            pos = nextPos;
        }
        dirty = true;
    }

    public void writeTo(byte[] bytes, int offset) throws IOException {
        flush();
        final long end = file.length;
        long pos = 0;
        int buffer = 0;
        int bytesUpto = offset;
        while (pos < end) {
            int length = bufferSize;
            long nextPos = pos + length;
            if (nextPos > end) {                        // at the last buffer
                length = (int) (end - pos);
            }
            System.arraycopy(file.getBuffer(buffer++), 0, bytes, bytesUpto, length);
            bytesUpto += length;
            pos = nextPos;
        }
    }
*/
    public void reset() {
        currentBuffer = null;
        currentBufferIndex = -1;
        bufferPosition = 0;
        bufferStart = 0;
        bufferLength = 0;
        file.setLength(0);

            crc.reset();

    }

    @Override
    public void close() throws IOException {
        if (dirty) {
            flush();
        }
    }

    @Override
    public void writeByte(final byte b) throws IOException {
        if (bufferPosition == bufferLength) {
            currentBufferIndex++;
            switchCurrentBuffer();
        }

            crc.update(b);

        currentBuffer[bufferPosition++] = b;
        //System.out.println("writeByte->"+b);
        dirty = true;
    }

    @Override
    public void writeBytes(final byte[] b, int offset, int len) throws IOException {
        assert b != null;

            crc.update(b, offset, len);

        while (len > 0) {
            if (bufferPosition == bufferLength) {
                currentBufferIndex++;
                switchCurrentBuffer();
            }

            int remainInBuffer = currentBuffer.length - bufferPosition;
            int bytesToCopy = len < remainInBuffer ? len : remainInBuffer;
            System.arraycopy(b, offset, currentBuffer, bufferPosition, bytesToCopy);
            offset += bytesToCopy;
            len -= bytesToCopy;
            bufferPosition += bytesToCopy;
           // System.out.println("writeBytes->"+bytesToCopy);
        }

        dirty = true;
    }

    private void switchCurrentBuffer() throws IOException {
        if (currentBufferIndex == file.numBuffers()) {
            currentBuffer = file.addBuffer(bufferSize);
        } else {
            currentBuffer = file.getBuffer(currentBufferIndex);
        }
        bufferPosition = 0;
        bufferStart = (long) bufferSize * (long) currentBufferIndex;
        bufferLength = currentBuffer.length;
    }

    private void setFileLength() {
        long pointer = bufferStart + bufferPosition;
        if (pointer > file.length) {
            file.setLength(pointer);
        }
    }

    @Override
    public void flush() throws IOException {
        file.setLastModified(System.currentTimeMillis());
        setFileLength();
        map.put(name, file);
        dirty = false;
    }

    @Override
    public long getFilePointer() {
        return currentBufferIndex < 0 ? 0 : bufferStart + bufferPosition;
    }

    @Override
    public long ramBytesUsed() {
        return (long) file.numBuffers() * (long) bufferSize;
    }
   /* public long sizeInBytes() {
        return file.numBuffers() * bufferSize;
    }*/

    @Override
    public void copyBytes(final DataInput input, long numBytes) throws IOException {
        assert numBytes >= 0 : "numBytes=" + numBytes;

        while (numBytes > 0) {
            if (bufferPosition == bufferLength) {
                currentBufferIndex++;
                switchCurrentBuffer();
            }

            int toCopy = currentBuffer.length - bufferPosition;
            if (numBytes < toCopy) {
                toCopy = (int) numBytes;
            }
            input.readBytes(currentBuffer, bufferPosition, toCopy, false);
            numBytes -= toCopy;
            bufferPosition += toCopy;
        }
        dirty = true;
    }

    @Override
    public long getChecksum() throws IOException {
        if (crc == null) {
            throw new IllegalStateException("internal MapDirectoryOutputStream created with checksum disabled");
        } else {
            return crc.getValue();
        }
    }
}
