package com.zengge.util;

import java.util.Collection;
import java.util.Iterator;

public class StringUtils {

    public static String join(Collection<String> collection, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = collection.iterator();
        while(iter.hasNext()) {
            buffer.append(iter.next());
            if(iter.hasNext())
                buffer.append(delimiter);
        }
        return buffer.toString();
    }
}
