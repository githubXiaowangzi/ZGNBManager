package jd.common.util;

public class VersionUtil {
    public static String getJDKVersion(int majorVersion, int minorVersion) {
        StringBuffer sb = new StringBuffer(20);
        if(majorVersion >= 49) {
            sb.append(majorVersion - 44);
            sb.append(" (");
            sb.append(majorVersion);
            sb.append('.');
            sb.append(minorVersion);
            sb.append(')');
        } else if(majorVersion >= 45) {
            sb.append("1.");
            sb.append(majorVersion - 44);
            sb.append(" (");
            sb.append(majorVersion);
            sb.append('.');
            sb.append(minorVersion);
            sb.append(')');
        }
        return sb.toString();
    }
}