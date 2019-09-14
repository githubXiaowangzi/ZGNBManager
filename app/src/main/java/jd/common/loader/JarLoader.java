package jd.common.loader;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jd.core.loader.LoaderException;

public class JarLoader
    extends BaseLoader {
    private ZipFile zipFile;

    public JarLoader(File file)
    throws LoaderException {
        super(file);
        if((!file.exists()) || (!file.isFile()))
            throw new LoaderException("'" + this.codebase + "' is not a directory");
        try {
            this.zipFile = new ZipFile(this.codebase);
        } catch(IOException e) {
            throw new LoaderException("Error reading from '" + this.codebase + "'");
        }
    }

    public DataInputStream load(String internalPath) {
        ZipEntry zipEntry = this.zipFile.getEntry(internalPath);
        if(zipEntry == null) {
            try {
                throw new LoaderException("Can not read '" + internalPath + "'");
            } catch(LoaderException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            return new DataInputStream(this.zipFile.getInputStream(zipEntry));
        } catch(IOException e) {
            try {
                throw new LoaderException("Error reading '" + internalPath + "'");
            } catch(LoaderException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        return null;
    }

    public boolean canLoad(String internalPath) {
        return this.zipFile.getEntry(internalPath) != null;
    }
}
