package com.zengge.res;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

import com.zengge.util.LEDataInputStream;
import com.zengge.util.LEDataOutputStream;

public class ARSCDecoder {
    private final LEDataInputStream mIn;
    public StringBlock mTableStrings;
    int packageCount;

    byte[] buf;
    String name;
    int id;

    public static final int ARSC_CHUNK_TYPE = 0x000c0002;
    public static final int CHECK_PACKAGE = 512;
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

    private ARSCDecoder(InputStream arscStream) {
        this.mIn = new LEDataInputStream(arscStream);
    }

    private void readTable() throws IOException {
        int type = mIn.readInt();
        checkChunk(type, ARSC_CHUNK_TYPE);
        mIn.readInt();// chunk size
        packageCount = this.mIn.readInt();
        this.mTableStrings = StringBlock.read(this.mIn);
        readPackage();
    }

    public void write(List<String> list, OutputStream out) throws IOException {
        write(list, new LEDataOutputStream(out));
    }

    public void write(List<String> list, LEDataOutputStream out)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LEDataOutputStream buf = new LEDataOutputStream(baos);
        buf.writeInt(packageCount);
        mTableStrings.write(list, buf);
        writePackage(buf);
        // write to out
        out.writeInt(ARSC_CHUNK_TYPE);
        out.writeInt(baos.size() + 8);
        out.writeFully(baos.toByteArray());
    }

    public static ARSCDecoder read(InputStream in) throws IOException {
        ARSCDecoder arsc = new ARSCDecoder(in);
        arsc.readTable();
        return arsc;
    }

    public void writePackage(LEDataOutputStream out) throws IOException {
        out.writeFully(byteOut.toByteArray());
    }

    private void readPackage() throws IOException {
        byte[] buf = new byte[2048];
        int num;
        while((num = mIn.read(buf, 0, 2048)) != -1)
            byteOut.write(buf, 0, num);
    }

    private void checkChunk(int type, int expectedType) throws IOException {
        if(type != expectedType)
            throw new IOException(String.format(
                                      "Invalid chunk type: expected=0x%08x, got=0x%08x",
                                      new Object[] { Integer.valueOf(expectedType),
                                              Short.valueOf((short) type)
                                                   }));
    }

}
