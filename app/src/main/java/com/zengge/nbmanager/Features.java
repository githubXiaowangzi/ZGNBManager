package com.zengge.nbmanager;

public class Features {
    public static native int ExtractAllRAR(String f, String d);

    public static native boolean Oat2Dex(String f);

    public static native void printLog(String file, String content, boolean append);

    public static native boolean Odex2Dex(String file, String dest);

    public static native boolean ZipAlign(String zip, String destZip);

    public static native boolean isZipAligned(String zip);

    public static native boolean isValidElf(String elf);

    public static native String compressStrToInt(String str);

    public static native long ELFHash(String strUri);
}
