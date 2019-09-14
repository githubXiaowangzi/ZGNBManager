package jd.common.loader;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jd.core.loader.LoaderException;

public class LoaderManager {
    protected static final String JAR_SUFFIX = ".jar";
    protected static final String ZIP_SUFFIX = ".zip";
    protected Map<String, BaseLoader> map;

    public LoaderManager() {
        this.map = new ConcurrentHashMap<String, BaseLoader>();
    }

    public BaseLoader getLoader(String codebase)
    throws LoaderException {
        File file = new File(codebase);
        String key = file.getAbsolutePath();
        BaseLoader loader = (BaseLoader)this.map.get(key);
        if(loader == null) {
            if(file.exists())
                loader = newLoader(key, file);
        } else if(file.exists()) {
            if((file.lastModified() != loader.getLastModified()) ||
                    (file.isFile() != loader.isFile()))
                loader = newLoader(key, file);
        } else
            this.map.remove(key);
        return loader;
    }

    protected BaseLoader newLoader(String key, File file)
    throws LoaderException {
        BaseLoader loader = null;
        if(file.isFile()) {
            if((endsWithIgnoreCase(key, ".jar")) ||
                    (endsWithIgnoreCase(key, ".zip")))
                this.map.put(key, loader = new JarLoader(file));
        } else if(file.isDirectory())
            this.map.put(key, loader = new DirectoryLoader(file));
        return loader;
    }

    protected static boolean endsWithIgnoreCase(String s, String suffix) {
        int suffixLength = suffix.length();
        int index = s.length() - suffixLength;
        return s.regionMatches(true, index, suffix, 0, suffixLength);
    }
}
