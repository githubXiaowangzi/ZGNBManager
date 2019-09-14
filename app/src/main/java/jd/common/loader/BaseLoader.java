package jd.common.loader;

import java.io.File;
import jd.core.loader.Loader;

public abstract class BaseLoader
    implements Loader {
    protected String codebase;
    protected long lastModified;
    protected boolean isFile;

    public BaseLoader(File file) {
        this.codebase = file.getAbsolutePath();
        this.lastModified = file.lastModified();
        this.isFile = file.isFile();
    }

    public String getCodebase() {
        return this.codebase;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    public boolean isFile() {
        return this.isFile;
    }
}
