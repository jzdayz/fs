package io.github.jzdayz.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class Utils {

    public static int BYTE_BUFFER_SIZE = 4096;

    public static byte[] toBytes(InputStream inputStream) throws Exception{
        byte[] buf = new byte[BYTE_BUFFER_SIZE];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int index = 0;
        while (index!=-1){
            index = inputStream.read(buf);
            if (index == -1){continue;}
            byteArrayOutputStream.write(buf,0,index);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
