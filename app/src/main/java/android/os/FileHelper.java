package android.os;

public class FileHelper {

    // public static boolean bSuccess = false;
    public static final class PassWord {
        public String name; // user name
        public String passwd; // user password
        public int uid; // user id
        public int gid; // group id
        public String gecos; // real name
        public String dir; // home directory
        public String shell; // shell program
    }

    public static final class Group {
        public String name; /* 组名称 */
        public String passwd; /* 组密码 */
        public int gid; /* 组识别码 */
        public String[] mem; /* 组成员账号 */
    }

    public static final class FileStatus {
        public int dev; // 文件的设备编号
        public int ino;// 节点
        public int mode;// 文件的类型和存取的权限
        public int nlink;// 连到该文件的硬连接数目，刚建立的文件值为1
        public int uid;// 用户ID
        public int gid; // 组ID
        public int rdev;// (设备类型)若此文件为设备文件，则为其设备编号
        public long size;// 文件字节数(文件大小)
        public int blksize;// 块大小(文件系统的I/O 缓冲区大小)
        public long blocks;// 块数
        public long atime;// 最后一次访问时间
        public long mtime;// 最后一次修改时间
        public long ctime;// 最后一次改变时间(指属性)
    }

    public static native boolean stat(String path, FileStatus status);

    public static native int setPermissions(String file, int mode, int uid, int gid);

    // outPermissions = {st.st_mode st.st_uid st.st_gid}
    public static native int getPermissions(String file, int[] outPermissions);

    public static native PassWord getpwuid(int uid);

    public static native int chown(String file, int uid, int gid);

    public static native int chmod(String file, int mode);

    public static native Group getgrgid(int gid);

    // public static native String stringFromJNI();
    /**
     * returns the FAT file system volume ID for the volume mounted at the given
     * mount point, or -1 for failure
     *
     * @param mountPoint
     *            point for FAT volume
     * @return volume ID or -1
     */
    public static native int getFatVolumeId(String mountPoint);

    static {
        // load library: FileHelper.so
        try {
            System.loadLibrary("FileHelper");
        } catch(UnsatisfiedLinkError ule) {
            System.err.println("WARNING: Could not load library : FileHelper!");
        }
    }

}
