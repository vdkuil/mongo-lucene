package com.github.mongoutils.lucene;

import java.io.EOFException;
import java.io.IOException;

import org.apache.lucene.store.IndexInput;

public class MapDirectoryInputStream extends IndexInput implements Cloneable {

    MapDirectoryEntry file;
    byte[] currentBuffer;
    int bufferLength;
    int bufferPosition;
    int currentBufferIndex;
    long bufferStart;
    long length;

    public MapDirectoryInputStream(final String name, final MapDirectoryEntry file) throws IOException {
        super(name);
        this.file = file;
        length = file.length;
        if (length / file.getBufferSize() >= Integer.MAX_VALUE) {
            throw new IOException("MapDirectoryInputStream too large length=" + length + ": " + name);
        }

        currentBufferIndex = -1;
        currentBuffer = null;
    }

    @Override
    public void close() {

    }

    @Override
    public long length() {
        //System.out.println("file->"+length);
        return length;
    }

    @Override
    public byte readByte() throws IOException {
        if (bufferPosition >= bufferLength) {
            currentBufferIndex++;
            switchCurrentBuffer(true);
        }
        // System.out.println("readByte->"+bufferLength);
        return currentBuffer[bufferPosition++];
    }

    @Override
    public void readBytes(final byte[] b, int offset, int len) throws IOException {
        while (len > 0) {
            if (bufferPosition >= bufferLength) {
                currentBufferIndex++;
                switchCurrentBuffer(true);
            }

            int remainInBuffer = bufferLength - bufferPosition;
            int bytesToCopy = len < remainInBuffer ? len : remainInBuffer;
            System.arraycopy(currentBuffer, bufferPosition, b, offset, bytesToCopy);
            offset += bytesToCopy;
            len -= bytesToCopy;
            bufferPosition += bytesToCopy;
            //System.out.println("readBytes->"+bytesToCopy);
        }
    }

    private void switchCurrentBuffer(final boolean enforceEOF) throws IOException {
        bufferStart = file.getBufferSize() * currentBufferIndex;
        if (currentBufferIndex >= file.numBuffers()) {
            if (enforceEOF) {
                throw new EOFException("Read past EOF (resource: " + this + ")");
            } else {
                currentBufferIndex--;
                bufferPosition = file.getBufferSize();
            }
        } else {
            currentBuffer = file.getBuffer(currentBufferIndex);
            bufferPosition = 0;
            long buflen = length - bufferStart;
            bufferLength = buflen > file.getBufferSize() ? file.getBufferSize() : (int) buflen;
        }
    }


    @Override
    public long getFilePointer() {
        return currentBufferIndex < 0 ? 0 : bufferStart + bufferPosition;
    }

    @Override
    public void seek(final long pos) throws IOException {
        if (currentBuffer == null || pos < bufferStart || pos >= bufferStart + file.getBufferSize()) {
            currentBufferIndex = (int) (pos / file.getBufferSize());
            switchCurrentBuffer(false);
        }
        bufferPosition = (int) (pos % file.getBufferSize());
    }

    @Override
    public IndexInput slice(String sliceDescription, final long offset, final long length) throws IOException {
        if (offset < 0 || length < 0 || offset + length > this.length) {
            throw new IllegalArgumentException("slice() " + sliceDescription + " out of bounds: " + this);
        }

        final String newResourceDescription = (sliceDescription == null) ? toString() : (toString() + " [slice=" + sliceDescription + "]");
        //MapDirectoryEntry newFile =new MapDirectoryEntry();
        //newFile.setBufferSize(512);
        file.setLength(offset + length);
        return new MapDirectoryInputStream(newResourceDescription, file) {
            {
                seek(0L);
            }

            @Override
            public void seek(long pos) throws IOException {
                if (pos < 0L) {
                    throw new IllegalArgumentException("Seeking to negative position: " + this);
                }
                super.seek(pos + offset);
            }

            @Override
            public long getFilePointer() {
                return super.getFilePointer() - offset;
            }

            @Override
            public long length() {
                return super.length() - offset;
            }

            @Override
            public IndexInput slice(String sliceDescription, long ofs, long len) throws IOException {
                return super.slice(sliceDescription, offset + ofs, len);
            }
        };
    }

}



