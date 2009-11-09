package com.bradmcevoy.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class BufferingOutputStream extends OutputStream{

    private static Logger log = LoggerFactory.getLogger(BufferingOutputStream.class);

    private ByteArrayOutputStream tempMemoryBuffer = new ByteArrayOutputStream();
    private int maxMemorySize;
    private File tempFile;
    private FileOutputStream fout;
    private BufferedOutputStream bufOut;

    private Runnable runnable;
    private long size;
    private boolean closed;

    public BufferingOutputStream( int maxMemorySize ) {
        this.maxMemorySize = maxMemorySize;
    }

    public InputStream getInputStream() {
        if( !closed ) throw new IllegalStateException( "this output stream is not yet closed");
        if( tempMemoryBuffer == null ) {
            FileDeletingInputStream fin;
            try {
                fin = new FileDeletingInputStream( tempFile );
            } catch( FileNotFoundException ex ) {
                throw new RuntimeException( tempFile.getAbsolutePath(), ex );
            }
            BufferedInputStream bufIn = new BufferedInputStream( fin );
            return bufIn;
        } else {
            return new ByteArrayInputStream( tempMemoryBuffer.toByteArray());
        }
    }



    @Override
    public void write( byte[] b ) throws IOException {
        size += b.length;
        if( tempMemoryBuffer != null ) {
            tempMemoryBuffer.write( b );
        } else {
            bufOut.write( b );
        }
        checkSize();
    }

    @Override
    public void write( int b ) throws IOException {
        size++;
        if( tempMemoryBuffer != null ) {
            tempMemoryBuffer.write( b );
        } else {
            bufOut.write( b );
        }
        checkSize();
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        size+=len;
        if( tempMemoryBuffer != null ) {
            tempMemoryBuffer.write( b,off, len );
        } else {
            bufOut.write( b, off, len );
        }
        checkSize();
    }

    private void checkSize() throws IOException {
        if( tempMemoryBuffer == null ) return ;

        if( tempMemoryBuffer.size() < maxMemorySize ) return ;

        tempFile = File.createTempFile( "" + System.currentTimeMillis(), ".buffer");
        fout = new FileOutputStream( tempFile );
        bufOut = new BufferedOutputStream( fout );
        bufOut.write( tempMemoryBuffer.toByteArray());
        tempMemoryBuffer = null;
    }

    @Override
    public void flush() throws IOException {
        if( tempMemoryBuffer != null ) {
            tempMemoryBuffer.flush();
        } else {
            bufOut.flush();
            fout.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if( tempMemoryBuffer != null ) {
            tempMemoryBuffer.close();
        } else {
            bufOut.close();
            fout.close();
        }
        closed = true;
        if( runnable != null ) {
            runnable.run();
        }
    }

    public long getSize() {
        return size;
    }

    File getTempFile() {
        return tempFile;
    }

    ByteArrayOutputStream getTempMemoryBuffer() {
        return tempMemoryBuffer;
    }

    public void setOnClose(Runnable r) {
        this.runnable = r;
    }

    /**
     * returns true if the data is completely held in memory
     *
     * @return
     */
    public boolean isCompleteInMemory() {
        return tempFile == null;
    }

    /**
     * Gets the data currently held in memory
     * 
     * @return
     */
    public byte[] getInMemoryData() {
        return this.tempMemoryBuffer.toByteArray();
    }
}