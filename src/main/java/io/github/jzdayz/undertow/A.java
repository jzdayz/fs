package io.github.jzdayz.undertow;

import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class A {
    public static void main(String[] args) throws Exception{

        Scanner s = new Scanner(System.in);
        s.next();
        /**
         * 从ClassPath中的Jar包读取某文件夹下的所有文件
         *
         * @author lihzh
         * @throws IOException
         * @data 2012-4-13 下午10:22:24
         */
            String dirPath = "template/";
            URL url = A.class.getClassLoader().getResource(dirPath);
//            Assert.assertNotNull(url);
            String urlStr = url.toString();
            // 找到!/ 截断之前的字符串
            String jarPath = urlStr.substring(0, urlStr.indexOf("!/") + 2);
            URL jarURL = new URL(jarPath);
            JarURLConnection jarCon = (JarURLConnection) jarURL.openConnection();
            JarFile jarFile = jarCon.getJarFile();
            Enumeration<JarEntry> jarEntrys = jarFile.entries();
//            Assert.assertTrue(jarEntrys.hasMoreElements());
            Properties props = new Properties();
            while (jarEntrys.hasMoreElements()) {
                JarEntry entry = jarEntrys.nextElement();
                // 简单的判断路径，如果想做到像Spring，Ant-Style格式的路径匹配需要用到正则。
                String name = entry.getName();
                if (name.startsWith(dirPath) && !entry.isDirectory()) {
                    // 开始读取文件内容
                    InputStream is = A.class.getClassLoader().getResourceAsStream(name);
//                    Assert.assertNotNull(is);
                    props.load(is);
                }
            }
//            Assert.assertTrue(props.containsKey("test.key"));
//            Assert.assertEquals("thisIsValue", props.getProperty("test.key"));
//            Assert.assertTrue(props.containsKey("test.key.two"));
//            Assert.assertEquals("thisIsAnotherValue", props.getProperty("test.key.two"));
    }
}
