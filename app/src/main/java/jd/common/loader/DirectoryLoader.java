package jd.common.loader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class DirectoryLoader
    extends BaseLoader {
    public DirectoryLoader(File file) {
        super(file);
        if((!file.exists()) || (!file.isDirectory()))
            return;
    }

    public DataInputStream load(String internalPath) {
        File file = new File(this.codebase, internalPath);
        try {
            return new DataInputStream(
                       new BufferedInputStream(new FileInputStream(file)));
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean canLoad(String internalPath) {
        File file = new File(this.codebase, internalPath);
        return (file.exists()) && (file.isFile());
    }
}
